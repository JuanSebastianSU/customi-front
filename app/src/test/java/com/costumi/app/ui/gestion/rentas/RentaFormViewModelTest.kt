package com.costumi.app.ui.gestion.rentas

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.RentaRepository
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.apiclient.models.CrearRentaRequest
import com.costumi.apiclient.models.RentaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Alta de renta: la clave de idempotencia es estable durante toda la vida del carrito (un reintento no
 * duplica la renta) y el registro enruta a renta simple o mixta (prendas + disfraces) según corresponda.
 */
class RentaFormViewModelTest {

    private lateinit var repo: RentaRepository
    private lateinit var disfrazRepo: DisfrazRepository
    private lateinit var pedido: PedidoDisfracesStore

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        disfrazRepo = mockk(relaxed = true)
        pedido = mockk(relaxed = true)
        coEvery { repo.sucursales() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = RentaFormViewModel(repo, disfrazRepo, pedido)

    private fun eventos(vm: RentaFormViewModel): MutableList<EventoRentaForm> {
        val out = mutableListOf<EventoRentaForm>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private val retiro = LocalDate.of(2026, 1, 10)
    private val devolucion = LocalDate.of(2026, 1, 13)

    @Test
    fun registrar_ok_usa_la_clave_de_idempotencia_del_vm_y_emite_el_importe() {
        val req = slot<CrearRentaRequest>()
        coEvery { repo.crear(capture(req)) } returns RespuestaRed.Exito(RentaResponse(importe = BigDecimal("30.00")))
        val vm = vm()
        val e = eventos(vm)
        vm.registrar(UUID.randomUUID(), UUID.randomUUID(), retiro, devolucion, emptyList(), deposito = null)

        assertEquals(vm.claveIdempotencia, req.captured.claveIdempotencia)
        val ev = e.filterIsInstance<EventoRentaForm.Registrada>().firstOrNull()
        assertTrue(ev != null)
        assertEquals(BigDecimal("30.00"), ev!!.importe)
    }

    @Test
    fun registrar_fallo_emite_error() {
        coEvery { repo.crear(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.CONFLICTO, "fecha ocupada"))
        val vm = vm()
        val e = eventos(vm)
        vm.registrar(UUID.randomUUID(), UUID.randomUUID(), retiro, devolucion, emptyList(), null)
        assertTrue(e.any { it is EventoRentaForm.Error })
    }

    @Test
    fun registrar_mixto_ok_llama_rentar_varios_limpia_el_pedido_y_avisa() {
        coEvery { disfrazRepo.rentarVarios(any(), any(), any(), any(), any(), any()) } returns RespuestaRed.Exito(UUID.randomUUID())
        val vm = vm()
        val e = eventos(vm)
        vm.registrarMixto(UUID.randomUUID(), UUID.randomUUID(), retiro, devolucion, emptyList(), emptyList())
        coVerify(exactly = 1) { disfrazRepo.rentarVarios(any(), any(), any(), any(), any(), any()) }
        coVerify { pedido.limpiar() }
        assertTrue(e.any { it is EventoRentaForm.Registrada })
    }
}
