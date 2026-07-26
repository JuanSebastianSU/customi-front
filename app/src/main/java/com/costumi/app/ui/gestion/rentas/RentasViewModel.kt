package com.costumi.app.ui.gestion.rentas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.RentaRepository
import com.costumi.apiclient.models.RentaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface EventoRenta {
    data class Info(val mensaje: String) : EventoRenta
    data class Error(val mensaje: String) : EventoRenta
    data class Contrato(val bytes: ByteArray) : EventoRenta
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RentasViewModel @Inject constructor(
    private val repo: RentaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Si se llegó desde la ficha de un cliente, acota a sus rentas (atajo de acción). */
    private val clienteId: UUID? =
        runCatching { UUID.fromString(savedStateHandle.get<String>(RentasFragment.ARG_CLIENTE_ID) ?: "") }.getOrNull()

    /** Nombre del cliente para el subtítulo, si se filtró por uno. */
    val clienteNombre: String? = savedStateHandle[RentasFragment.ARG_CLIENTE_NOMBRE]

    /** Texto de busqueda; al cambiar se vuelve a pedir la primera pagina. */
    private val _buscar = MutableStateFlow<String?>(null)

    /** Bandeja/estado (chip): POR_ENTREGAR, ACTIVAS, VENCIDAS, CERRADAS; null = todas. */
    private val _filtro = MutableStateFlow<String?>(null)

    /** El usuario escribio en la caja de busqueda. */
    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    /** Filtra por bandeja (chip); server-side, así que pagina bien. */
    fun filtrarBandeja(filtro: String?) {
        _filtro.value = filtro
    }

    val rentas = combine(_buscar, _filtro) { buscar, filtro -> buscar to filtro }
        .flatMapLatest { (buscar, filtro) -> repo.rentas(buscar, filtro, clienteId) }
        .cachedIn(viewModelScope)

    private val _eventos = MutableSharedFlow<EventoRenta>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun entregar(id: UUID) = ejecutar("Renta entregada.") { repo.entregar(id) }
    fun devolver(id: UUID) = ejecutar("Renta devuelta.") { repo.devolver(id) }
    fun cerrar(id: UUID) = ejecutar("Renta cerrada.") { repo.cerrar(id) }
    fun cancelar(id: UUID) = ejecutar("Renta cancelada.") { repo.cancelar(id) }
    fun extender(id: UUID, nuevaFecha: LocalDate) = ejecutar("Renta extendida.") { repo.extender(id, nuevaFecha) }

    fun descargarContrato(id: UUID) {
        viewModelScope.launch {
            when (val r = repo.contrato(id)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoRenta.Contrato(r.data))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoRenta.Error(r.error.mensaje))
            }
        }
    }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<RentaResponse>) {
        viewModelScope.launch {
            _eventos.tryEmit(
                when (val r = accion()) {
                    is RespuestaRed.Exito -> EventoRenta.Info(exito)
                    is RespuestaRed.Fallo -> EventoRenta.Error(r.error.mensaje)
                },
            )
        }
    }
}
