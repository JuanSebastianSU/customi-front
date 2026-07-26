package com.costumi.app.ui.cliente.explorar

import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.repo.MarketplaceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Explorar tiendas (cache-first + búsqueda): muestra la caché al instante y, al buscar, prioriza lo que
 * responde el servidor (para encontrar tiendas que aún no están cacheadas). Buscar vacío vuelve a la caché.
 */
class ExplorarViewModelTest {

    private val main = UnconfinedTestDispatcher()
    private lateinit var repo: MarketplaceRepository

    @Before
    fun antes() {
        Dispatchers.setMain(main)
        repo = mockk(relaxed = true)
        coEvery { repo.refrescarEmpresas(any()) } returns RespuestaRed.Exito(Unit)
        coEvery { repo.destacados() } returns RespuestaRed.Fallo(com.costumi.app.core.ErrorApi(com.costumi.app.core.TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun empresa(nombre: String) = EmpresaEntity(id = java.util.UUID.randomUUID().toString(), nombre = nombre, ciudad = null, logoUrl = null, portadaUrl = null)

    /** Suscribe estado (es un stateIn WhileSubscribed: sin colector no computa). */
    private fun suscribir(vm: ExplorarViewModel) {
        CoroutineScope(main).launch { vm.estado.collect { } }
    }

    @Test
    fun muestra_la_cache_al_instante() {
        every { repo.observarEmpresas() } returns flowOf(listOf(empresa("Fiesta")))
        val vm = ExplorarViewModel(repo)
        suscribir(vm)
        val estado = vm.estado.value
        assertTrue(estado is UiState.Success)
        assertEquals("Fiesta", (estado as UiState.Success).data.first().nombre)
    }

    @Test
    fun al_buscar_prioriza_los_resultados_del_servidor_sobre_la_cache() {
        every { repo.observarEmpresas() } returns flowOf(emptyList()) // cache vacía
        coEvery { repo.buscarEmpresas("pira") } returns RespuestaRed.Exito(listOf(empresa("Pirata Shop")))
        val vm = ExplorarViewModel(repo)
        suscribir(vm)
        vm.buscar("pira")
        val estado = vm.estado.value
        assertTrue(estado is UiState.Success)
        assertEquals("Pirata Shop", (estado as UiState.Success).data.first().nombre)
    }

    @Test
    fun buscar_vacio_vuelve_a_mostrar_la_cache() {
        every { repo.observarEmpresas() } returns flowOf(listOf(empresa("Fiesta")))
        val vm = ExplorarViewModel(repo)
        suscribir(vm)
        vm.buscar("   ") // en blanco -> descarta resultados, muestra cache
        val estado = vm.estado.value
        assertTrue(estado is UiState.Success)
        assertEquals("Fiesta", (estado as UiState.Success).data.first().nombre)
    }
}
