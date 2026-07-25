package com.costumi.app.ui.gestion.taxonomia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.ValorEtiquetaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoValor {
    data class Info(val mensaje: String) : EventoValor
    data class Error(val mensaje: String) : EventoValor
    data class ConfirmarArchivar(val valor: ValorEtiquetaResponse, val prendas: Int?) : EventoValor
}

@HiltViewModel
class ValoresViewModel @Inject constructor(
    private val repo: TaxonomiaRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val tipoId: UUID = UUID.fromString(estado[ValoresFragment.ARG_TIPO_ID]!!)
    val tipoNombre: String = estado[ValoresFragment.ARG_TIPO_NOMBRE] ?: "Tipo de etiqueta"

    private val _estado = MutableStateFlow<UiState<List<ValorEtiquetaResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoValor>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.valores(tipoId)) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedWith(compareBy({ it.archivada == true }, { it.valor })))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun crear(valor: String) = ejecutar("Valor agregado.") { repo.agregarValor(tipoId, valor) }

    fun renombrar(valorId: UUID, valor: String) = ejecutar("Valor renombrado.") {
        repo.renombrarValor(tipoId, valorId, valor)
    }

    fun activar(valorId: UUID) = ejecutar("Valor activado.") { repo.activarValor(tipoId, valorId) }

    fun solicitarArchivar(valor: ValorEtiquetaResponse) {
        val id = valor.id ?: return
        viewModelScope.launch {
            val conteo = (repo.conteoValor(tipoId, id) as? RespuestaRed.Exito)?.data
            _eventos.tryEmit(EventoValor.ConfirmarArchivar(valor, conteo))
        }
    }

    fun archivar(valorId: UUID) = ejecutar("Valor archivado.") { repo.archivarValor(tipoId, valorId) }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<ValorEtiquetaResponse>) {
        viewModelScope.launch {
            when (val r = accion()) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoValor.Info(exito))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoValor.Error(r.error.mensaje))
            }
        }
    }
}
