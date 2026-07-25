package com.costumi.app.ui.gestion.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.NotificacionRepository
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.EnviarNotificacionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoNotif {
    data class Info(val mensaje: String) : EventoNotif
    data class Error(val mensaje: String) : EventoNotif
}

/** Notificaciones: bandeja paginada, envío manual y recordatorios. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val repo: NotificacionRepository,
) : ViewModel() {

    /** Texto de busqueda; al cambiar se vuelve a pedir la primera pagina. */
    private val _buscar = MutableStateFlow<String?>(null)

    /** Bandeja paginada (scroll infinito); la busqueda va al servidor. */
    val notificaciones = _buscar.flatMapLatest { repo.notificaciones(it) }.cachedIn(viewModelScope)

    /** Clientes (no archivados) para el selector del envío y para nombrar al destinatario en la fila. */
    private val _clientes = MutableStateFlow<List<ClienteResponse>>(emptyList())
    val clientes = _clientes.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoNotif>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargarClientes()
    }

    /** El usuario escribio en la caja de busqueda: se reemite el Pager con el nuevo filtro. */
    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    private fun cargarClientes() {
        viewModelScope.launch {
            (repo.clientes() as? RespuestaRed.Exito)?.let { _clientes.value = it.data }
        }
    }

    fun enviar(canal: EnviarNotificacionRequest.Canal, clienteId: UUID?, mensaje: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.enviar(canal, clienteId, mensaje)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoNotif.Info("Notificacion enviada."))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoNotif.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    fun recordarVencidas() = recordatorio("Recordatorios enviados") { repo.recordarVencidas() }
    fun recordarProximas() = recordatorio("Recordatorios de proximas enviados") { repo.recordarProximas() }
    fun avisarStockBajo() = recordatorio("Avisos de stock bajo enviados") { repo.avisarStockBajo() }

    private fun recordatorio(
        etiqueta: String,
        accion: suspend () -> RespuestaRed<com.costumi.apiclient.models.RecordatorioResponse>,
    ) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoNotif.Info("$etiqueta: ${r.data.enviadas ?: 0}."))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoNotif.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
