package com.costumi.app.ui.gestion.empleados

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.EstablecerPermisoRequest
import com.costumi.apiclient.models.PermisoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Una sección con sus dos permisos (VER = puede ver la sección; ACCION = puede operar en ella). */
data class PermisoSeccion(
    val seccion: PermisoDto.Seccion,
    val nombre: String,
    val ver: Boolean,
    val accion: Boolean,
)

/** Matriz de permisos de un empleado: sección × acción (VER/ACCION). */
@HiltViewModel
class PermisosEmpleadoViewModel @Inject constructor(
    private val repo: EmpleadoRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val empleadoId: UUID = UUID.fromString(estado[PermisosEmpleadoFragment.ARG_ID]!!)
    val email: String = estado.get<String>(PermisosEmpleadoFragment.ARG_EMAIL) ?: "Empleado"

    private val _estado = MutableStateFlow<UiState<List<PermisoSeccion>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error = _error.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.permisos(empleadoId)) {
                is RespuestaRed.Exito -> UiState.Success(agrupar(r.data))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Setea un permiso (optimista; si falla, recarga para volver al estado real). */
    fun establecer(seccion: PermisoDto.Seccion, esVer: Boolean, concedido: Boolean) {
        viewModelScope.launch {
            val accion = if (esVer) EstablecerPermisoRequest.Accion.VER else EstablecerPermisoRequest.Accion.ACCION
            val r = repo.establecerPermiso(empleadoId, EstablecerPermisoRequest.Seccion.valueOf(seccion.value), accion, concedido)
            if (r is RespuestaRed.Fallo) {
                _error.tryEmit(r.error.mensaje)
                cargar()
            }
        }
    }

    private fun agrupar(dtos: List<PermisoDto>): List<PermisoSeccion> {
        val porSeccion = dtos.groupBy { it.seccion }
        return ORDEN.mapNotNull { (seccion, nombre) ->
            val entradas = porSeccion[seccion] ?: return@mapNotNull null
            PermisoSeccion(
                seccion = seccion,
                nombre = nombre,
                ver = entradas.firstOrNull { it.accion == PermisoDto.Accion.VER }?.concedido == true,
                accion = entradas.firstOrNull { it.accion == PermisoDto.Accion.ACCION }?.concedido == true,
            )
        }
    }

    private companion object {
        val ORDEN = listOf(
            PermisoDto.Seccion.INVENTARIO to "Inventario",
            PermisoDto.Seccion.DISFRACES to "Disfraces",
            PermisoDto.Seccion.VENTAS to "Ventas",
            PermisoDto.Seccion.RENTAS to "Rentas",
            PermisoDto.Seccion.DEVOLUCIONES to "Devoluciones",
            PermisoDto.Seccion.PAGOS to "Pagos",
            PermisoDto.Seccion.CAJA to "Caja",
            PermisoDto.Seccion.REPORTES to "Reportes",
            PermisoDto.Seccion.CLIENTES to "Clientes",
            PermisoDto.Seccion.CONFIGURACION to "Configuracion",
            PermisoDto.Seccion.NOTIFICACIONES to "Notificaciones",
            PermisoDto.Seccion.EMPLEADOS to "Empleados",
        )
    }
}
