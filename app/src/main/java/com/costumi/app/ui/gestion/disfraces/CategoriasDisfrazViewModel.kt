package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoCategoriaDisfraz {
    data class Info(val mensaje: String) : EventoCategoriaDisfraz
    data class Error(val mensaje: String) : EventoCategoriaDisfraz
}

/** Gestión de las categorías de DISFRAZ (taxonomía propia): alta, renombrar y archivar/activar. */
@HiltViewModel
class CategoriasDisfrazViewModel @Inject constructor(
    private val repo: DisfrazRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<CategoriaDeDisfrazResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoCategoriaDisfraz>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.categoriasDeDisfraz()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedWith(compareBy({ it.archivada == true }, { it.nombre })))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun crear(nombre: String) = ejecutar("Categoría creada.") { repo.crearCategoriaDisfraz(nombre) }

    fun renombrar(id: UUID, nombre: String) = ejecutar("Categoría renombrada.") {
        repo.renombrarCategoriaDisfraz(id, nombre)
    }

    fun archivar(id: UUID) = ejecutar("Categoría archivada.") { repo.archivarCategoriaDisfraz(id) }

    fun activar(id: UUID) = ejecutar("Categoría activada.") { repo.activarCategoriaDisfraz(id) }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<CategoriaDeDisfrazResponse>) {
        viewModelScope.launch {
            when (val r = accion()) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoCategoriaDisfraz.Info(exito))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCategoriaDisfraz.Error(r.error.mensaje))
            }
        }
    }
}
