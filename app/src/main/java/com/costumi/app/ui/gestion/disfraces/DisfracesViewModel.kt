package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.costumi.apiclient.models.DisfrazResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoDisfraz {
    data class Info(val mensaje: String) : EventoDisfraz
    data class Error(val mensaje: String) : EventoDisfraz
    data class Disponibilidad(val nombre: String, val disponible: Boolean) : EventoDisfraz
}

@HiltViewModel
class DisfracesViewModel @Inject constructor(
    private val repo: DisfrazRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<DisfrazResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _categorias = MutableStateFlow<List<CategoriaDeDisfrazResponse>>(emptyList())
    val categorias = _categorias.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDisfraz>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private var todos: List<DisfrazResponse> = emptyList()
    private var categoriaSel: UUID? = null
    val categoriaSeleccionada: UUID? get() = categoriaSel

    init {
        cargar()
    }

    /** Texto de busqueda vigente; null = sin filtrar. */
    private var buscar: String? = null

    /** El usuario escribio en la caja de busqueda: se guarda y se recarga la lista. */
    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            (repo.categoriasDeDisfraz() as? RespuestaRed.Exito)?.let {
                _categorias.value = it.data.filter { c -> c.archivada != true }
            }
            when (val r = repo.disfraces(buscar)) {
                is RespuestaRed.Exito -> { todos = r.data; emitirFiltrados() }
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Filtra por categoría (null = todas) — RF-2.3, ver los disfraces por categoría ("Piratas"). */
    fun seleccionarCategoria(id: UUID?) {
        categoriaSel = id
        emitirFiltrados()
    }

    private fun emitirFiltrados() {
        val filtrados = todos
            .filter { categoriaSel == null || it.categoriaId == categoriaSel }
            .sortedWith(compareBy({ it.activo != true }, { it.nombre }))
        _estado.value = if (filtrados.isEmpty()) UiState.Empty else UiState.Success(filtrados)
    }

    fun consultarDisponibilidad(disfraz: DisfrazResponse) {
        val id = disfraz.id ?: return
        viewModelScope.launch {
            when (val r = repo.disponibilidad(id)) {
                is RespuestaRed.Exito ->
                    _eventos.tryEmit(EventoDisfraz.Disponibilidad(disfraz.nombre.orEmpty(), r.data))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoDisfraz.Error(r.error.mensaje))
            }
        }
    }

    fun alternarArchivado(disfraz: DisfrazResponse) {
        val id = disfraz.id ?: return
        val activo = disfraz.activo == true
        viewModelScope.launch {
            val r = if (activo) repo.archivar(id) else repo.activar(id)
            when (r) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoDisfraz.Info(if (activo) "Disfraz archivado." else "Disfraz activado."))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoDisfraz.Error(r.error.mensaje))
            }
        }
    }
}
