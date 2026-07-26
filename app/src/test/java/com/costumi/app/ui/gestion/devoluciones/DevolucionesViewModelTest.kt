package com.costumi.app.ui.gestion.devoluciones

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DevolucionRepository
import com.costumi.apiclient.models.DevolucionResponse
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Historial de devoluciones: cruza cada devolución con la info de su renta (código de retiro y cliente) y
 * muestra la más reciente primero (por cuándo se registró, no por id). El join "de quién es" importa: sin él
 * el empleado ve una liquidación sin saber a qué cliente corresponde.
 */
class DevolucionesViewModelTest {

    private lateinit var repo: DevolucionRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        coEvery { repo.infoRentas() } returns emptyMap()
        coEvery { repo.historial(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun dev(rentaId: UUID?, dia: Int) = DevolucionResponse(
        id = UUID.randomUUID(), rentaId = rentaId,
        registradaEn = OffsetDateTime.of(2026, 1, dia, 0, 0, 0, 0, ZoneOffset.UTC),
    )

    @Test
    fun sin_devoluciones_es_empty() {
        coEvery { repo.historial(any()) } returns RespuestaRed.Exito(emptyList())
        val vm = DevolucionesViewModel(repo)
        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun cruza_cada_devolucion_con_el_codigo_y_cliente_de_su_renta() {
        val renta = UUID.randomUUID()
        coEvery { repo.historial(any()) } returns RespuestaRed.Exito(listOf(dev(renta, 10)))
        coEvery { repo.infoRentas() } returns mapOf(renta.toString() to ("R-123" to "Juan Perez"))
        val vm = DevolucionesViewModel(repo)
        val ui = (vm.estado.value as UiState.Success).data.first()
        assertEquals("R-123", ui.codigoRetiro)
        assertEquals("Juan Perez", ui.clienteNombre)
    }

    @Test
    fun devolucion_sin_match_de_renta_queda_sin_codigo_ni_cliente() {
        coEvery { repo.historial(any()) } returns RespuestaRed.Exito(listOf(dev(UUID.randomUUID(), 10)))
        coEvery { repo.infoRentas() } returns emptyMap() // no hay info de esa renta
        val vm = DevolucionesViewModel(repo)
        val ui = (vm.estado.value as UiState.Success).data.first()
        assertEquals(null, ui.codigoRetiro)
        assertEquals(null, ui.clienteNombre)
    }

    @Test
    fun muestra_la_mas_reciente_primero() {
        coEvery { repo.historial(any()) } returns RespuestaRed.Exito(listOf(dev(null, 8), dev(null, 15), dev(null, 11)))
        val vm = DevolucionesViewModel(repo)
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(15, visibles.first().dev.registradaEn!!.dayOfMonth)
    }

    @Test
    fun error_de_red_es_error() {
        coEvery { repo.historial(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.SERVIDOR, "boom"))
        val vm = DevolucionesViewModel(repo)
        assertTrue(vm.estado.value is UiState.Error)
    }
}
