package com.costumi.app.ui.gestion.dashboard

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.InventarioRepository
import com.costumi.app.data.repo.MiEmpresaRepository
import com.costumi.app.data.repo.ReembolsoRepository
import com.costumi.app.data.repo.ReporteRepository
import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.RentaVencidaResponse
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Panel de gestión: arma las alertas de "lo que requiere atención hoy". Reglas: solo muestra las que
 * existen, pluraliza, calcula el peor atraso de las rentas vencidas, cuenta solo reembolsos PENDIENTES, y
 * las ordena por gravedad (rentas vencidas → stock bajo → reembolsos). Es best-effort: si una fuente falla,
 * muestra las otras.
 */
class DashboardViewModelTest {

    private lateinit var repo: ReporteRepository
    private lateinit var inventario: InventarioRepository
    private lateinit var reembolsos: ReembolsoRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        inventario = mockk(relaxed = true)
        reembolsos = mockk(relaxed = true)
        coEvery { repo.dashboard() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
        // Fuentes de alertas: por defecto vacías (best-effort).
        coEvery { repo.rentasVencidas(any()) } returns RespuestaRed.Exito(emptyList())
        coEvery { inventario.stockBajo(any()) } returns RespuestaRed.Exito(emptyList())
        coEvery { reembolsos.bandeja(any()) } returns RespuestaRed.Exito(emptyList())
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = DashboardViewModel(repo, inventario, mockk<MiEmpresaRepository>(relaxed = true), reembolsos)

    private fun renta(dias: Long) = RentaVencidaResponse(diasVencida = dias)
    private fun pendiente(estado: SolicitudDeReembolsoResponse.Estado) = SolicitudDeReembolsoResponse(estado = estado)

    @Test
    fun rentas_vencidas_pluraliza_y_muestra_el_peor_atraso() {
        coEvery { repo.rentasVencidas(any()) } returns RespuestaRed.Exito(listOf(renta(3), renta(5)))
        val a = vm().alertas.value
        assertEquals(1, a.size)
        assertTrue(a.first().titulo.contains("2 rentas vencidas"))
        assertTrue(a.first().detalle.contains("5 dias")) // la mas atrasada
        assertEquals(Tono.ERROR, a.first().tono)
    }

    @Test
    fun una_sola_renta_va_en_singular() {
        coEvery { repo.rentasVencidas(any()) } returns RespuestaRed.Exito(listOf(renta(2)))
        assertTrue(vm().alertas.value.first().titulo.contains("1 renta vencida"))
    }

    @Test
    fun solo_muestra_las_alertas_que_existen() {
        coEvery { inventario.stockBajo(any()) } returns RespuestaRed.Exito(listOf(GrupoDeStockResponse()))
        val a = vm().alertas.value
        assertEquals(1, a.size) // solo stock; rentas y reembolsos estan vacios
        assertEquals(Tono.ALERTA, a.first().tono)
        assertTrue(a.first().titulo.contains("stock bajo"))
    }

    @Test
    fun ordena_por_gravedad_rentas_stock_reembolsos() {
        coEvery { repo.rentasVencidas(any()) } returns RespuestaRed.Exito(listOf(renta(1)))
        coEvery { inventario.stockBajo(any()) } returns RespuestaRed.Exito(listOf(GrupoDeStockResponse()))
        coEvery { reembolsos.bandeja(any()) } returns RespuestaRed.Exito(listOf(pendiente(SolicitudDeReembolsoResponse.Estado.PENDIENTE)))
        val a = vm().alertas.value
        assertEquals(3, a.size)
        assertTrue(a[0].titulo.contains("renta"))
        assertTrue(a[1].titulo.contains("stock"))
        assertTrue(a[2].titulo.contains("reembolso"))
    }

    @Test
    fun reembolsos_cuenta_solo_los_pendientes() {
        coEvery { reembolsos.bandeja(any()) } returns RespuestaRed.Exito(
            listOf(
                pendiente(SolicitudDeReembolsoResponse.Estado.PENDIENTE),
                pendiente(SolicitudDeReembolsoResponse.Estado.APROBADA),
            ),
        )
        val a = vm().alertas.value
        assertEquals(1, a.size)
        assertTrue(a.first().titulo.contains("1 reembolso"))
    }

    @Test
    fun sin_nada_urgente_no_hay_alertas() {
        assertTrue(vm().alertas.value.isEmpty()) // todo al dia
    }
}
