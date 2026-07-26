package com.costumi.app.ui.cliente.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
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

    /** true cuando se muestra caché porque el refresco falló por falta de red (aviso N4). */
    private val _sinConexion = MutableStateFlow(false)
    val sinConexion = _sinConexion.asStateFlow()

    /** true tras el primer refresco: recién ahí una caché vacía significa "no tienes pedidos" (Empty). */
    private var yaRefresco = false

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
        // Cache-first: el historial se pinta desde Room y se actualiza solo cuando el refresco escribe.
        observar()
        cargar()
    }

    private fun observar() {
        viewModelScope.launch {
            repo.observarHistorial().collect { lista ->
                todos = lista
                // Antes del primer refresco, una caché vacía es "cargando", no "no tienes pedidos".
                if (lista.isNotEmpty() || yaRefresco) publicar()
            }
        }
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
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoPedido.Info("¡Solicitud enviada! La tienda revisara tu reembolso."))
                    cargar() // el estado del pedido cambió: refrescar el historial cacheado
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPedido.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** Refresca desde la red hacia Room. No tapa la caché con un spinner: solo Loading/Error si aún no hay datos. */
    fun cargar() {
        viewModelScope.launch {
            if (_estado.value !is UiState.Success) _estado.value = UiState.Loading
            when (val r = repo.refrescarHistorial()) {
                is RespuestaRed.Exito -> {
                    yaRefresco = true
                    _sinConexion.value = false
                    // Si el historial quedó vacío tras refrescar, el Flow puede no re-emitir: publicá igual.
                    publicar()
                }
                is RespuestaRed.Fallo -> {
                    yaRefresco = true
                    val hayCache = _estado.value is UiState.Success
                    _sinConexion.value = hayCache && r.error.tipo == TipoError.SIN_CONEXION
                    if (!hayCache) _estado.value = UiState.Error(r.error.mensaje) { cargar() }
                }
            }
        }
    }

    /** Aplica el filtro vigente sobre lo traído y lo emite. */
    private fun publicar() {
        val visibles = EstadoDePedido.aplicar(todos, filtro)
        _estado.value = if (visibles.isEmpty()) UiState.Empty else UiState.Success(visibles)
    }
}
