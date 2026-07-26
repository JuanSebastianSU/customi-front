package com.costumi.app.ui.gestion.empleados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.EmpleadoRepository
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.InvitacionPendienteResponse
import com.costumi.apiclient.models.InvitarEmpleadoRequest
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
    /** Invitación creada: el enlace de aceptación para compartir con la persona (Fase B). */
    data class Invitacion(val email: String, val enlace: String) : EventoEmpleado
    /** Invitaciones pendientes de la tienda (para listar/cancelar). */
    data class Pendientes(val invitaciones: List<InvitacionPendienteResponse>) : EventoEmpleado
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

    /** Lo último traído del backend sin filtrar por rol/sucursal; se re-filtra en memoria. */
    private var todos: List<EmpleadoDetalleResponse> = emptyList()

    /** Filtros en la app (la lista de personal es acotada y viene completa). null = sin filtrar. */
    private var rolFiltro: String? = null
    private var sucursalFiltro: UUID? = null

    /** El usuario escribio en la caja de busqueda: se guarda y se recarga la lista. */
    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    /** Filtra por rol (en MAYUSCULAS como el backend); null = todos. Se aplica sobre lo ya traido. */
    fun filtrarRol(rol: String?) {
        rolFiltro = rol
        publicar()
    }

    /** Filtra por sucursal asignada; null = todas. */
    fun filtrarSucursal(id: UUID?) {
        sucursalFiltro = id
        publicar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = repo.empleados(buscar)) {
                is RespuestaRed.Exito -> { todos = r.data; publicar() }
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Aplica los filtros de rol/sucursal sobre lo traido, ordena (activos primero) y lo emite. */
    private fun publicar() {
        val visibles = todos
            .filter { rolFiltro == null || it.rol?.uppercase() == rolFiltro }
            .filter { sucursalFiltro == null || it.sucursales.orEmpty().contains(sucursalFiltro) }
            .sortedByDescending { it.activo == true }
        _estado.value = if (visibles.isEmpty()) UiState.Empty else UiState.Success(visibles)
    }

    private fun cargarSucursales() {
        viewModelScope.launch {
            (repo.sucursales() as? RespuestaRed.Exito)?.let { _sucursales.value = it.data }
        }
    }

    /** Alta = invitación (Fase B): invita por email y emite el enlace de aceptación para compartir. */
    fun invitar(email: String, rol: InvitarEmpleadoRequest.Rol) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.invitar(email, rol)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoEmpleado.Invitacion(email, r.data.enlace.orEmpty()))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    fun cambiarRol(id: UUID, rol: CambiarRolRequest.Rol) =
        ejecutar("Rol actualizado.") { repo.cambiarRol(id, rol) }

    fun activar(id: UUID) = ejecutar("Cuenta reactivada.") { repo.activar(id) }
    fun desactivar(id: UUID) = ejecutar("Cuenta desactivada.") { repo.desactivar(id) }

    // Membresía de trabajo (Fase B): el empleado sigue siendo cliente, solo cambia su acceso al trabajo.
    fun suspender(id: UUID) = ejecutar("Empleado suspendido del trabajo.") { repo.suspenderMembresia(id) }
    fun reactivarMembresia(id: UUID) = ejecutar("Empleado reactivado en el trabajo.") { repo.reactivarMembresia(id) }
    fun quitar(id: UUID) = ejecutar("Empleado dado de baja.") { repo.quitar(id) }

    /** Carga las invitaciones pendientes y las emite para mostrarlas. */
    fun verPendientes() {
        viewModelScope.launch {
            when (val r = repo.invitacionesPendientes()) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoEmpleado.Pendientes(r.data))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
            }
        }
    }

    fun cancelarInvitacion(id: UUID) {
        viewModelScope.launch {
            when (val r = repo.cancelarInvitacion(id)) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoEmpleado.Info("Invitación cancelada."))
                    verPendientes()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
            }
        }
    }

    /** Reenvía una invitación pendiente y muestra el enlace nuevo (para compartir a mano si el email no llega). */
    fun reenviarInvitacion(id: UUID, email: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.reenviarInvitacion(id)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoEmpleado.Invitacion(email, r.data.enlace.orEmpty()))
                // 409 = dos reenvíos casi simultáneos: uno ganó (ya salió el correo), el otro choca con el
                // índice único de "una sola invitación pendiente". No es un error para el usuario: el correo
                // ya se mandó. Se muestra un aviso amable en vez del mensaje crudo de base de datos.
                is RespuestaRed.Fallo ->
                    if (r.error.httpCode == 409) {
                        _eventos.tryEmit(EventoEmpleado.Info("Ya se reenvió la invitación a $email. Revisá el correo (puede tardar un momento)."))
                    } else {
                        _eventos.tryEmit(EventoEmpleado.Error(r.error.mensaje))
                    }
            }
            _procesando.value = false
        }
    }

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
