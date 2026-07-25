package com.costumi.app.ui.gestion.empleados

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.CapacidadDto
import com.costumi.apiclient.models.EstablecerPermisoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Una fila de la matriz de permisos: un encabezado de sección o una capacidad con su toggle. */
sealed interface PermisoFila {
    val id: String

    /** Encabezado de una sección (agrupa sus capacidades). */
    data class Encabezado(val nombre: String) : PermisoFila {
        override val id: String get() = "H:$nombre"
    }

    /** Una capacidad configurable: qué habilita (descripción) y si está concedida. */
    data class Capacidad(val clave: String, val descripcion: String, val concedido: Boolean) : PermisoFila {
        override val id: String get() = clave
    }
}

/** Matriz de capacidades de un empleado (Fase B, paso 5): catálogo agrupado por sección con un toggle c/u. */
@HiltViewModel
class PermisosEmpleadoViewModel @Inject constructor(
    private val repo: EmpleadoRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val empleadoId: UUID = UUID.fromString(estado[PermisosEmpleadoFragment.ARG_ID]!!)
    val email: String = estado.get<String>(PermisosEmpleadoFragment.ARG_EMAIL) ?: "Empleado"

    private val _estado = MutableStateFlow<UiState<List<PermisoFila>>>(UiState.Loading)
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
                is RespuestaRed.Exito -> UiState.Success(aFilas(r.data))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Concede/niega una capacidad (optimista; si falla, recarga para volver al estado real). */
    fun establecer(clave: String, concedido: Boolean) {
        viewModelScope.launch {
            val capacidad = runCatching { EstablecerPermisoRequest.Capacidad.valueOf(clave) }.getOrNull() ?: return@launch
            val r = repo.establecerPermiso(empleadoId, capacidad, concedido)
            if (r is RespuestaRed.Fallo) {
                _error.tryEmit(r.error.mensaje)
                cargar()
            }
        }
    }

    /** Aplana el catálogo en filas: por cada sección (en orden), un encabezado y sus capacidades. */
    private fun aFilas(dtos: List<CapacidadDto>): List<PermisoFila> {
        val porSeccion = dtos.groupBy { it.seccion }
        val filas = mutableListOf<PermisoFila>()
        for ((seccion, nombre) in ORDEN) {
            val caps = porSeccion[seccion].orEmpty()
            if (caps.isEmpty()) continue
            filas += PermisoFila.Encabezado(nombre)
            caps.forEach { c ->
                filas += PermisoFila.Capacidad(
                    clave = c.capacidad.orEmpty(),
                    descripcion = c.descripcion ?: c.capacidad.orEmpty(),
                    concedido = c.concedido == true,
                )
            }
        }
        return filas
    }

    private companion object {
        val ORDEN = listOf(
            "INVENTARIO" to "Inventario",
            "CATALOGO" to "Catálogo",
            "DISFRACES" to "Disfraces",
            "VENTAS" to "Ventas",
            "RENTAS" to "Rentas",
            "DEVOLUCIONES" to "Devoluciones",
            "PAGOS" to "Pagos",
            "CAJA" to "Caja",
            "REEMBOLSOS" to "Reembolsos",
            "CLIENTES" to "Clientes",
            "REPORTES" to "Reportes",
            "AUDITORIA" to "Auditoría",
            "CONFIGURACION" to "Configuración",
            "SUCURSALES" to "Sucursales",
            "IDENTIDAD_TIENDA" to "Identidad de la tienda",
            "NOTIFICACIONES" to "Notificaciones",
            "EMPLEADOS" to "Personal",
        )
    }
}
