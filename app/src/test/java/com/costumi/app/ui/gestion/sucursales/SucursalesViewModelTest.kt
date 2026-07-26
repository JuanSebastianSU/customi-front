package com.costumi.app.ui.gestion.sucursales

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.SucursalRepository
import com.costumi.apiclient.models.SucursalResponse
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
 * Sucursales cache-first (lado dueño): pinta desde Room, avisa "datos guardados" si el refresco falla por
 * red, y muestra "no hay sucursales" cuando la red confirma 0 sin quedarse cargando.
 */
class SucursalesViewModelTest {

    private lateinit var repo: SucursalRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        every { repo.observarSucursales() } returns flowOf(emptyList())
        coEvery { repo.refrescarSucursales() } returns RespuestaRed.Exito(0)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun sucursal() = SucursalResponse(id = UUID.randomUUID(), nombre = "Centro", archivada = false)

    @Test
    fun cache_con_sucursales_muestra_success() {
        every { repo.observarSucursales() } returns flowOf(listOf(sucursal()))
        coEvery { repo.refrescarSucursales() } returns RespuestaRed.Exito(1)
        val vm = SucursalesViewModel(repo)
        assertTrue(vm.estado.value is UiState.Success)
    }

    @Test
    fun refresco_vacio_sin_cache_muestra_empty() {
        every { repo.observarSucursales() } returns flowOf(emptyList())
        coEvery { repo.refrescarSucursales() } returns RespuestaRed.Exito(0)
        val vm = SucursalesViewModel(repo)
        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun con_cache_y_red_caida_prende_el_aviso_y_sigue_mostrando() {
        every { repo.observarSucursales() } returns flowOf(listOf(sucursal()))
        coEvery { repo.refrescarSucursales() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red"))
        val vm = SucursalesViewModel(repo)
        assertTrue(vm.sinConexion.value)
        assertTrue(vm.estado.value is UiState.Success)
    }

    @Test
    fun sin_cache_y_error_de_red_es_error() {
        every { repo.observarSucursales() } returns flowOf(emptyList())
        coEvery { repo.refrescarSucursales() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SERVIDOR, "boom"))
        val vm = SucursalesViewModel(repo)
        assertTrue(vm.estado.value is UiState.Error)
    }
}
