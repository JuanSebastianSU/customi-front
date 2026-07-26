package com.costumi.app.ui.gestion.reembolsos

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.ReembolsoRepository
import com.costumi.app.data.repo.RentaRepository
import com.costumi.app.data.repo.VentaRepository
import com.costumi.apiclient.models.RentaResponse
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import com.costumi.apiclient.models.VentaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Bandeja de reembolsos: filtra por pestaña (pendientes/resueltas), muestra pendientes primero, y
 * "registrar devolución" enruta a renta o venta según el tipo del concepto (precondición para aprobar).
 */
class ReembolsosViewModelTest {

    private lateinit var repo: ReembolsoRepository
    private lateinit var rentaRepo: RentaRepository
    private lateinit var ventaRepo: VentaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        rentaRepo = mockk(relaxed = true)
        ventaRepo = mockk(relaxed = true)
        coEvery { repo.bandeja(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = ReembolsosViewModel(repo, mockk(relaxed = true), ventaRepo, rentaRepo)

    private fun eventos(vm: ReembolsosViewModel): MutableList<EventoReembolso> {
        val out = mutableListOf<EventoReembolso>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun solicitud(estado: SolicitudDeReembolsoResponse.Estado, tipo: SolicitudDeReembolsoResponse.TipoConcepto = SolicitudDeReembolsoResponse.TipoConcepto.VENTA) =
        SolicitudDeReembolsoResponse(id = UUID.randomUUID(), conceptoId = UUID.randomUUID(), tipoConcepto = tipo, estado = estado)

    /** Puebla `todas` post-construcción (las props se declaran tras el init, ver chip de fragilidad). */
    private fun vmCon(lista: List<SolicitudDeReembolsoResponse>): ReembolsosViewModel {
        coEvery { repo.bandeja(any()) } returns RespuestaRed.Exito(lista)
        return vm().also { it.cargar() }
    }

    @Test
    fun filtro_pendientes_deja_solo_los_pendientes() {
        val v = vmCon(
            listOf(
                solicitud(SolicitudDeReembolsoResponse.Estado.PENDIENTE),
                solicitud(SolicitudDeReembolsoResponse.Estado.APROBADA),
                solicitud(SolicitudDeReembolsoResponse.Estado.PENDIENTE),
            ),
        )
        v.filtrar(ReembolsosViewModel.Filtro.PENDIENTES)
        val visibles = (v.estado.value as UiState.Success).data
        assertEquals(2, visibles.size)
        assertTrue(visibles.all { it.estado == SolicitudDeReembolsoResponse.Estado.PENDIENTE })
    }

    @Test
    fun filtro_resueltas_deja_los_no_pendientes() {
        val v = vmCon(
            listOf(
                solicitud(SolicitudDeReembolsoResponse.Estado.PENDIENTE),
                solicitud(SolicitudDeReembolsoResponse.Estado.RECHAZADA),
            ),
        )
        v.filtrar(ReembolsosViewModel.Filtro.RESUELTAS)
        val visibles = (v.estado.value as UiState.Success).data
        assertEquals(1, visibles.size)
        assertEquals(SolicitudDeReembolsoResponse.Estado.RECHAZADA, visibles.first().estado)
    }

    @Test
    fun registrar_devolucion_de_renta_va_a_rentaRepo() {
        coEvery { rentaRepo.devolver(any()) } returns RespuestaRed.Exito(RentaResponse())
        val v = vm()
        v.registrarDevolucion(solicitud(SolicitudDeReembolsoResponse.Estado.PENDIENTE, SolicitudDeReembolsoResponse.TipoConcepto.RENTA))
        coVerify(exactly = 1) { rentaRepo.devolver(any()) }
        coVerify(exactly = 0) { ventaRepo.devolver(any(), any()) }
    }

    @Test
    fun registrar_devolucion_de_venta_va_a_ventaRepo() {
        coEvery { ventaRepo.devolver(any(), any()) } returns RespuestaRed.Exito(VentaResponse())
        val v = vm()
        v.registrarDevolucion(solicitud(SolicitudDeReembolsoResponse.Estado.PENDIENTE, SolicitudDeReembolsoResponse.TipoConcepto.VENTA))
        coVerify(exactly = 1) { ventaRepo.devolver(any(), any()) }
        coVerify(exactly = 0) { rentaRepo.devolver(any()) }
    }

    @Test
    fun aprobar_llama_a_aprobar_y_avisa() {
        coEvery { repo.aprobar(any(), any()) } returns RespuestaRed.Exito(solicitud(SolicitudDeReembolsoResponse.Estado.APROBADA))
        val v = vm()
        val e = eventos(v)
        v.aprobar(UUID.randomUUID(), "ok")
        coVerify(exactly = 1) { repo.aprobar(any(), any()) }
        assertTrue(e.any { it is EventoReembolso.Info })
    }

    @Test
    fun rechazar_llama_a_rechazar() {
        coEvery { repo.rechazar(any(), any()) } returns RespuestaRed.Exito(solicitud(SolicitudDeReembolsoResponse.Estado.RECHAZADA))
        val v = vm()
        v.rechazar(UUID.randomUUID(), "no aplica")
        coVerify(exactly = 1) { repo.rechazar(any(), any()) }
    }
}
