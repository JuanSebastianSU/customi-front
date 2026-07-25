package com.costumi.app.ui.gestion.empleados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.AltaDeEmpleadoRequest
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.EmpleadoDetalleResponse
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoEmpleado {
    data class Info(val mensaje: String) : EventoEmpleado
    data class Error(val mensaje: String) : EventoEmpleado
    data class Actividad(val email: String, val ventas: Long, val total: java.math.BigDecimal?) : EventoEmpleado
}

/** Gestión de personal: lista, alta, cambiar rol, baja/reactivación y asignar sucursales. */
@HiltViewModel
class EmpleadosViewModel @Inject constructor(
    private val repo: EmpleadoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<EmpleadoDetalleResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _sucursales = MutableStateFlow<List<SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoEmpleado>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
        cargarSucursales()
    }

    /** Texto de busqueda vigente; null = sin filtrar. */
    private var buscar: String? = null

    /** El usuario escribio en la caja de busqueda: se guarda y se recarga la lista. */
    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.empleados(buscar)) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedByDescending { it.activo == true })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    private fun cargarSucursales() {
        viewModelScope.launch {
            (repo.sucursales() as? RespuestaRed.Exito)?.let { _sucursales.value = it.data }
        }
    }

    fun crear(email: String, password: String, rol: AltaDeEmpleadoRequest.Rol) =
        ejecutar("Empleado dado de alta.") { repo.crear(email, password, rol) }

    fun cambiarRol(id: UUID, rol: CambiarRolRequest.Rol) =
        ejecutar("Rol actualizado.") { repo.cambiarRol(id, rol) }

    fun activar(id: UUID) = ejecutar("Empleado reactivado.") { repo.activar(id) }
    fun desactivar(id: UUID) = ejecutar("Empleado desactivado.") { repo.desactivar(id) }

    fun asignarSucursales(id: UUID, sucursalIds: List<UUID>) =
        ejecutar("Sucursales asignadas.") { repo.asignarSucursales(id, sucursalIds) }

    fun verActividad(id: UUID, email: String) {
        viewModelScope.launch {
            when (val r = repo.actividad(id)) {
                is RespuestaRed.Exito ->
                    _eventos.tryEmit(EventoEmpleado.Actividad(email, r.data.ventas ?: 0L, r.data.totalVendido))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
            }
        }
    }

    private fun <T> ejecutar(exito: String, accion: suspend () -> RespuestaRed<T>) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito -> { _eventos.tryEmit(EventoEmpleado.Info(exito)); cargar() }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
