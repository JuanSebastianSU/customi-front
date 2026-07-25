package com.costumi.app.ui.gestion.ventas

import kotlinx.coroutines.flow.flatMapLatest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.VentaRepository
import com.costumi.apiclient.models.DevolverVentaRequest
import com.costumi.apiclient.models.LineaADevolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoVenta {
    data class Info(val mensaje: String) : EventoVenta
    data class Error(val mensaje: String) : EventoVenta
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class VentasViewModel @Inject constructor(
    private val repo: VentaRepository,
) : ViewModel() {

    /** Texto de busqueda; al cambiar se vuelve a pedir la primera pagina. */
    private val _buscar = MutableStateFlow<String?>(null)

    /** El usuario escribio en la caja de busqueda. */
    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    val ventas = _buscar.flatMapLatest { repo.ventas(it) }.cachedIn(viewModelScope)

    private val _eventos = MutableSharedFlow<EventoVenta>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private val _nombresPrenda = MutableStateFlow<Map<UUID, String>>(emptyMap())

    init {
        viewModelScope.launch {
            (repo.prendasParaSelector() as? RespuestaRed.Exito)?.let { r ->
                _nombresPrenda.value = r.data.mapNotNull { p -> p.id?.let { it to p.nombre.orEmpty() } }.toMap()
            }
        }
    }

    fun nombrePrenda(id: UUID?): String = _nombresPrenda.value[id] ?: "Articulo"

    fun devolver(ventaId: UUID, lineas: List<LineaADevolver>) {
        if (lineas.isEmpty()) {
            _eventos.tryEmit(EventoVenta.Error("Indica al menos una unidad a devolver."))
            return
        }
        viewModelScope.launch {
            when (val r = repo.devolver(ventaId, DevolverVentaRequest(lineas))) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoVenta.Info("Devolucion registrada."))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoVenta.Error(r.error.mensaje))
            }
        }
    }
}
