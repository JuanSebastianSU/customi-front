package com.costumi.app.ui.cliente.tienda

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.MarketplaceRepository
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Catálogo de una tienda (prendas + disfraces) cache-first: cada pestaña pinta desde Room, muestra "vacío"
 * cuando la red confirma 0 y prende el aviso "datos guardados" si el refresco falla por red.
 */
class TiendaViewModelTest {

    private val empresaId = UUID.randomUUID().toString()
    private lateinit var repo: MarketplaceRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        // Por defecto ambas listas vacías con refresco 0.
        every { repo.observarCatalogo(any()) } returns flowOf(emptyList())
        every { repo.observarDisfraces(any()) } returns flowOf(emptyList())
        coEvery { repo.refrescarCatalogo(any()) } returns RespuestaRed.Exito(0)
        coEvery { repo.refrescarDisfraces(any()) } returns RespuestaRed.Exito(0)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = TiendaViewModel(repo, SavedStateHandle(mapOf("empresaId" to empresaId, "nombre" to "El Baul")))

    @Test
    fun prendas_con_cache_muestra_success() {
        every { repo.observarCatalogo(any()) } returns flowOf(listOf(PrendaVitrinaResponse(id = UUID.randomUUID(), nombre = "Capa")))
        coEvery { repo.refrescarCatalogo(any()) } returns RespuestaRed.Exito(1)
        assertTrue(vm().prendas.value is UiState.Success)
    }

    @Test
    fun disfraces_con_cache_muestra_success() {
        every { repo.observarDisfraces(any()) } returns flowOf(listOf(DisfrazResponse(id = UUID.randomUUID(), nombre = "Pirata")))
        coEvery { repo.refrescarDisfraces(any()) } returns RespuestaRed.Exito(1)
        assertTrue(vm().disfraces.value is UiState.Success)
    }

    @Test
    fun prendas_refresco_vacio_muestra_empty() {
        assertTrue(vm().prendas.value is UiState.Empty) // observar vacio + refresco 0
    }

    @Test
    fun con_cache_y_red_caida_prende_el_aviso() {
        every { repo.observarCatalogo(any()) } returns flowOf(listOf(PrendaVitrinaResponse(id = UUID.randomUUID(), nombre = "Capa")))
        coEvery { repo.refrescarCatalogo(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red"))
        val vm = vm()
        assertTrue(vm.sinConexion.value)
        assertTrue(vm.prendas.value is UiState.Success)
    }
}
