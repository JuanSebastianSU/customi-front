package com.costumi.app.ui.cliente.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.apiclient.models.HistorialItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Eventos de una sola vez de Mis Pedidos (resultado del reembolso). */
sealed interface EventoPedido {
    data class Info(val mensaje: String) : EventoPedido
    data class Error(val mensaje: String) : EventoPedido
}

@HiltViewModel
class MisPedidosViewModel @Inject constructor(
    private val repo: CuentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<HistorialItem>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPedido>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /**
     * Filtro por estado. Hoy se aplica en la app sobre la lista completa (el historial no se pagina);
     * cuando el backend lo pagine + acepte `?filtro=`, pasa a ser server-side (ver PROGRESS.md, Grupo B).
     */
    private var filtro = EstadoDePedido.Filtro.TODOS

    /** Lo último traído sin filtrar; se re-filtra sin volver a la red. */
    private var todos: List<HistorialItem> = emptyList()

    init {
        cargar()
    }

    fun filtrar(f: EstadoDePedido.Filtro) {
        filtro = f
        publicar()
    }

    fun solicitarReembolso(pedido: HistorialItem, motivo: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.solicitarReembolso(pedido, motivo)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(
                    EventoPedido.Info("¡Solicitud enviada! La tienda revisara tu reembolso."),
                )
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPedido.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = repo.miHistorial()) {
                is RespuestaRed.Exito -> { todos = r.data; publicar() }
                is RespuestaRed.Fallo ->
                    _estado.value = UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Aplica el filtro vigente sobre lo traído y lo emite. */
    private fun publicar() {
        val visibles = EstadoDePedido.aplicar(todos, filtro)
        _estado.value = if (visibles.isEmpty()) UiState.Empty else UiState.Success(visibles)
    }
}
