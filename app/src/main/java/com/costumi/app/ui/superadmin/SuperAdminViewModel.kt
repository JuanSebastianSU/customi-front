package com.costumi.app.ui.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.SuperAdminRepository
import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoSuperAdmin {
    data class Info(val mensaje: String) : EventoSuperAdmin
    data class Error(val mensaje: String) : EventoSuperAdmin
}

/** Filas del panel: encabezados de sección, solicitudes pendientes y empresas gestionables. */
sealed interface FilaSuperAdmin {
    data class Encabezado(val titulo: String) : FilaSuperAdmin
    data class Solicitud(val empresa: EmpresaPendienteResponse) : FilaSuperAdmin
    data class Empresa(val empresa: EmpresaResumenResponse) : FilaSuperAdmin
}

/** Panel SUPERADMIN: cola de solicitudes (aprobar/rechazar) + empresas (suspender/reactivar). */
@HiltViewModel
class SuperAdminViewModel @Inject constructor(
    private val repo: SuperAdminRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<FilaSuperAdmin>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoSuperAdmin>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            val pendientes = repo.pendientes()
            val empresas = repo.empresas()
            // Si ambas fallan, mostramos error; si al menos una responde, armamos lo que haya.
            if (pendientes is RespuestaRed.Fallo && empresas is RespuestaRed.Fallo) {
                _estado.value = UiState.Error(pendientes.error.mensaje) { cargar() }
                return@launch
            }
            val filas = buildList {
                val pend = (pendientes as? RespuestaRed.Exito)?.data.orEmpty()
                    .sortedBy { it.fechaRegistro }
                if (pend.isNotEmpty()) {
                    add(FilaSuperAdmin.Encabezado("Solicitudes pendientes"))
                    pend.forEach { add(FilaSuperAdmin.Solicitud(it)) }
                }
                val emp = (empresas as? RespuestaRed.Exito)?.data.orEmpty()
                    .sortedBy { it.nombre?.lowercase() }
                if (emp.isNotEmpty()) {
                    add(FilaSuperAdmin.Encabezado("Empresas"))
                    emp.forEach { add(FilaSuperAdmin.Empresa(it)) }
                }
            }
            _estado.value = if (filas.isEmpty()) UiState.Empty else UiState.Success(filas)
        }
    }

    fun aprobar(id: UUID, nombre: String) = operar("\"$nombre\" aprobada.") { repo.aprobar(id) }

    fun rechazar(id: UUID, nombre: String) = operar("\"$nombre\" rechazada.") { repo.rechazar(id) }

    fun suspender(id: UUID, nombre: String) = operar("\"$nombre\" suspendida.") { repo.suspender(id) }

    fun reactivar(id: UUID, nombre: String) = operar("\"$nombre\" reactivada.") { repo.reactivar(id) }

    private fun operar(exito: String, accion: suspend () -> RespuestaRed<*>) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito -> { _eventos.tryEmit(EventoSuperAdmin.Info(exito)); cargar() }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoSuperAdmin.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
