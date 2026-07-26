package com.costumi.app.ui.cliente.pedidos

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.apiclient.models.HistorialItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
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
 * "Mis pedidos" cache-first: pinta desde Room, aplica el filtro por estado en la app, avisa "datos guardados"
 * si el refresco falla por red, y —clave— muestra "no tienes pedidos" cuando la red confirma 0 sin quedarse
 * cargando (Room no re-emite si la tabla ya estaba vacía).
 */
class MisPedidosViewModelTest {

    private lateinit var repo: CuentaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        every { repo.observarHistorial() } returns flowOf(emptyList())
        coEvery { repo.refrescarHistorial() } returns RespuestaRed.Exito(0)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun item(estado: String) = HistorialItem(operacionId = UUID.randomUUID(), estado = estado)

    @Test
    fun cache_con_pedidos_muestra_success() {
        every { repo.observarHistorial() } returns flowOf(listOf(item("ACTIVA")))
        coEvery { repo.refrescarHistorial() } returns RespuestaRed.Exito(1)
        val vm = MisPedidosViewModel(repo)
        assertTrue(vm.estado.value is UiState.Success)
    }

    @Test
    fun refresco_vacio_sin_cache_muestra_empty_no_se_queda_cargando() {
        every { repo.observarHistorial() } returns flowOf(emptyList())
        coEvery { repo.refrescarHistorial() } returns RespuestaRed.Exito(0)
        val vm = MisPedidosViewModel(repo)
        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun el_filtro_por_estado_se_aplica_sobre_la_lista() {
        every { repo.observarHistorial() } returns flowOf(listOf(item("ACTIVA"), item("CANCELADA")))
        coEvery { repo.refrescarHistorial() } returns RespuestaRed.Exito(2)
        val vm = MisPedidosViewModel(repo)
        vm.filtrar(EstadoDePedido.Filtro.ACTIVOS)
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(1, visibles.size)
        assertEquals("ACTIVA", visibles.first().estado)
    }

    @Test
    fun con_cache_y_red_caida_prende_el_aviso_y_sigue_mostrando() {
        every { repo.observarHistorial() } returns flowOf(listOf(item("ACTIVA")))
        coEvery { repo.refrescarHistorial() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red"))
        val vm = MisPedidosViewModel(repo)
        assertTrue(vm.sinConexion.value)
        assertTrue(vm.estado.value is UiState.Success)
    }

    @Test
    fun solicitar_reembolso_ok_emite_info() {
        coEvery { repo.solicitarReembolso(any(), any()) } returns RespuestaRed.Exito(mockk(relaxed = true))
        val vm = MisPedidosViewModel(repo)
        val eventos = mutableListOf<EventoPedido>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { eventos.add(it) } }
        vm.solicitarReembolso(item("ACTIVA"), "no me sirvio")
        assertTrue(eventos.any { it is EventoPedido.Info })
    }
}
