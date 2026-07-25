package com.costumi.app.ui.gestion.taxonomia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.TipoEtiquetaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoTipo {
    data class Info(val mensaje: String) : EventoTipo
    data class Error(val mensaje: String) : EventoTipo
    data class ConfirmarArchivar(val tipo: TipoEtiquetaResponse, val prendas: Int?) : EventoTipo
}

@HiltViewModel
class TiposEtiquetaViewModel @Inject constructor(
    private val repo: TaxonomiaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<TipoEtiquetaResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoTipo>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.tiposEtiqueta()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedWith(compareBy({ it.archivada == true }, { it.nombre })))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun crear(nombre: String, defineVariante: Boolean, seleccionable: Boolean) =
        ejecutar("Tipo de etiqueta creado.") { repo.crearTipo(nombre, defineVariante, seleccionable) }

    fun renombrar(id: UUID, nombre: String) = ejecutar("Tipo renombrado.") {
        repo.renombrarTipo(id, nombre)
    }

    fun activar(id: UUID) = ejecutar("Tipo activado.") { repo.activarTipo(id) }

    fun solicitarArchivar(tipo: TipoEtiquetaResponse) {
        val id = tipo.id ?: return
        viewModelScope.launch {
            val conteo = (repo.conteoTipo(id) as? RespuestaRed.Exito)?.data
            _eventos.tryEmit(EventoTipo.ConfirmarArchivar(tipo, conteo))
        }
    }

    fun archivar(id: UUID) = ejecutar("Tipo archivado.") { repo.archivarTipo(id) }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<TipoEtiquetaResponse>) {
        viewModelScope.launch {
            when (val r = accion()) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoTipo.Info(exito))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoTipo.Error(r.error.mensaje))
            }
        }
    }
}
