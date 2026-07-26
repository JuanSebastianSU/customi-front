package com.costumi.app.ui.gestion.auditoria

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.AuditoriaRepository
import com.costumi.apiclient.models.AuditoriaResponse
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

/**
 * Auditoría (trail de eventos): la categoría de cada evento se deriva de la primera palabra de la acción
 * ("VENTA_CONFIRMADA" → "Venta"); los chips salen de los datos (no hardcodeados); se muestra más reciente
 * primero y se puede filtrar por categoría en la app.
 */
class AuditoriaViewModelTest {

    private lateinit var repo: AuditoriaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        coEvery { repo.registros(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun evento(accion: String, dia: Int) =
        AuditoriaResponse(accion = accion, fecha = OffsetDateTime.of(2026, 1, dia, 0, 0, 0, 0, ZoneOffset.UTC))

    private fun vmCon(lista: List<AuditoriaResponse>): AuditoriaViewModel {
        coEvery { repo.registros(any()) } returns RespuestaRed.Exito(lista)
        return AuditoriaViewModel(repo) // props antes del init -> cargar() puebla `todas`
    }

    @Test
    fun las_categorias_se_derivan_de_las_acciones_distintas_y_ordenadas() {
        val vm = vmCon(listOf(evento("VENTA_CONFIRMADA", 10), evento("RENTA_ENTREGADA", 12), evento("VENTA_DEVUELTA", 8)))
        assertEquals(listOf("Renta", "Venta"), vm.categorias.value)
    }

    @Test
    fun muestra_mas_reciente_primero() {
        val vm = vmCon(listOf(evento("VENTA_CONFIRMADA", 10), evento("RENTA_ENTREGADA", 12), evento("VENTA_DEVUELTA", 8)))
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(12, visibles.first().fecha!!.dayOfMonth) // el del día 12 arriba
    }

    @Test
    fun filtrar_por_categoria_deja_solo_esa() {
        val vm = vmCon(listOf(evento("VENTA_CONFIRMADA", 10), evento("RENTA_ENTREGADA", 12), evento("VENTA_DEVUELTA", 8)))
        vm.filtrar("Venta")
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(2, visibles.size)
        assertTrue(visibles.all { it.accion?.startsWith("VENTA") == true })
    }

    @Test
    fun sin_registros_es_empty() {
        val vm = vmCon(emptyList())
        assertTrue(vm.estado.value is UiState.Empty)
    }
}
