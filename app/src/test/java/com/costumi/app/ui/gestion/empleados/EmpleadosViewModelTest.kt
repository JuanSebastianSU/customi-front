package com.costumi.app.ui.gestion.empleados

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.EmpleadoDetalleResponse
import io.mockk.coEvery
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
 * Gestión de personal: el filtrado por rol/sucursal se hace en memoria sobre la lista traída, ordenando
 * activos primero. Y el reenvío de invitación trata el 409 (dos reenvíos casi simultáneos) como aviso amable,
 * no como error crudo de base de datos. Estas dos reglas son fáciles de romper sin querer.
 */
class EmpleadosViewModelTest {

    private lateinit var repo: EmpleadoRepository
    private val sucA = UUID.randomUUID()
    private val sucB = UUID.randomUUID()

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun emp(rol: String, activo: Boolean, sucursales: List<UUID> = emptyList()) =
        EmpleadoDetalleResponse(id = UUID.randomUUID(), email = "$rol@x.com", rol = rol, activo = activo, sucursales = sucursales)

    private fun vmCon(lista: List<EmpleadoDetalleResponse>): EmpleadosViewModel {
        coEvery { repo.empleados(any()) } returns RespuestaRed.Exito(lista)
        // El VM declara sus props (`todos`, filtros) ANTES del init{}, así que el cargar() del init las
        // puebla bien incluso corriendo síncrono (con el dispatcher de test). Sin workaround.
        return EmpleadosViewModel(repo)
    }

    private fun eventos(vm: EmpleadosViewModel): MutableList<EventoEmpleado> {
        val out = mutableListOf<EventoEmpleado>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun filtra_por_rol() {
        val vm = vmCon(listOf(emp("MOSTRADOR", true), emp("BODEGA", true), emp("MOSTRADOR", true)))
        vm.filtrarRol("MOSTRADOR")
        val estado = vm.estado.value
        assertTrue(estado is UiState.Success)
        val visibles = (estado as UiState.Success).data
        assertEquals(2, visibles.size)
        assertTrue(visibles.all { it.rol == "MOSTRADOR" })
    }

    @Test
    fun filtra_por_sucursal() {
        val vm = vmCon(listOf(emp("MOSTRADOR", true, listOf(sucA)), emp("BODEGA", true, listOf(sucB))))
        vm.filtrarSucursal(sucA)
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(1, visibles.size)
        assertEquals(sucA, visibles.first().sucursales?.first())
    }

    @Test
    fun ordena_activos_primero() {
        val vm = vmCon(listOf(emp("MOSTRADOR", false), emp("BODEGA", true)))
        vm.filtrarRol(null) // sin filtro, solo ordena
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals(true, visibles.first().activo) // el activo va arriba
    }

    @Test
    fun sin_coincidencias_queda_vacio() {
        val vm = vmCon(listOf(emp("MOSTRADOR", true)))
        vm.filtrarRol("BODEGA")
        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun reenviar_con_409_muestra_aviso_amable_no_error() {
        val vm = vmCon(emptyList())
        val e = eventos(vm)
        coEvery { repo.reenviarInvitacion(any()) } returns RespuestaRed.Fallo(
            ErrorApi(TipoError.CONFLICTO, "duplicate key", httpCode = 409),
        )
        vm.reenviarInvitacion(UUID.randomUUID(), "juan@x.com")
        // 409 -> Info amable con el email, NO un Error crudo.
        assertTrue(e.any { it is EventoEmpleado.Info && it.mensaje.contains("juan@x.com") })
        assertTrue(e.none { it is EventoEmpleado.Error })
    }

    @Test
    fun reenviar_con_otro_error_si_es_error() {
        val vm = vmCon(emptyList())
        val e = eventos(vm)
        coEvery { repo.reenviarInvitacion(any()) } returns RespuestaRed.Fallo(
            ErrorApi(TipoError.SERVIDOR, "boom", httpCode = 500),
        )
        vm.reenviarInvitacion(UUID.randomUUID(), "juan@x.com")
        assertTrue(e.any { it is EventoEmpleado.Error })
    }
}
