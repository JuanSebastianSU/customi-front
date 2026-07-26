package com.costumi.app.ui.gestion.empleados

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.CapacidadDto
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.util.UUID

/**
 * Matriz de permisos de un empleado: aplana el catálogo en filas (un encabezado por sección con caps, en un
 * orden fijo; secciones vacías se omiten) y al togglear valida que la capacidad exista antes de tocar la red.
 */
class PermisosEmpleadoViewModelTest {

    private lateinit var repo: EmpleadoRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        coEvery { repo.permisos(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun cap(seccion: String, capacidad: String, concedido: Boolean = false) =
        CapacidadDto(seccion = seccion, capacidad = capacidad, descripcion = "hace $capacidad", concedido = concedido)

    private fun vmCon(caps: List<CapacidadDto>): PermisosEmpleadoViewModel {
        coEvery { repo.permisos(any()) } returns RespuestaRed.Exito(caps)
        val handle = SavedStateHandle(mapOf("empleadoId" to UUID.randomUUID().toString(), "empleadoEmail" to "juan@x.com"))
        return PermisosEmpleadoViewModel(repo, handle)
    }

    @Test
    fun aplana_en_encabezados_y_capacidades_respetando_el_orden_de_secciones() {
        // Llegan desordenadas (VENTAS antes que INVENTARIO); deben salir INVENTARIO primero (orden fijo).
        val vm = vmCon(listOf(cap("VENTAS", "VENTA_REGISTRAR"), cap("INVENTARIO", "INVENTARIO_VER")))
        val filas = (vm.estado.value as UiState.Success).data
        // Encabezado Inventario, su capacidad, luego Encabezado Ventas, su capacidad.
        assertTrue(filas[0] is PermisoFila.Encabezado && (filas[0] as PermisoFila.Encabezado).nombre == "Inventario")
        assertTrue(filas[1] is PermisoFila.Capacidad && (filas[1] as PermisoFila.Capacidad).clave == "INVENTARIO_VER")
        assertTrue(filas[2] is PermisoFila.Encabezado && (filas[2] as PermisoFila.Encabezado).nombre == "Ventas")
    }

    @Test
    fun las_secciones_sin_capacidades_no_generan_encabezado() {
        val vm = vmCon(listOf(cap("INVENTARIO", "INVENTARIO_VER")))
        val filas = (vm.estado.value as UiState.Success).data
        assertEquals(2, filas.size) // solo 1 encabezado + 1 capacidad; nada de las otras 16 secciones
        assertTrue(filas.none { it is PermisoFila.Encabezado && (it as PermisoFila.Encabezado).nombre == "Ventas" })
    }

    @Test
    fun establecer_con_clave_invalida_no_llama_al_backend() {
        val vm = vmCon(emptyList())
        vm.establecer("NO_ES_UNA_CAPACIDAD", true)
        coVerify(exactly = 0) { repo.establecerPermiso(any(), any(), any()) }
    }

    @Test
    fun establecer_con_clave_valida_llama_al_backend() {
        coEvery { repo.establecerPermiso(any(), any(), any()) } returns RespuestaRed.Exito(mockk(relaxed = true))
        val vm = vmCon(emptyList())
        vm.establecer("INVENTARIO_VER", true)
        coVerify(exactly = 1) { repo.establecerPermiso(any(), any(), true) }
    }
}
