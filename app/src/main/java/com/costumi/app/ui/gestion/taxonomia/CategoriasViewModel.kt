package com.costumi.app.ui.gestion.taxonomia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.CategoriaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoCategoria {
    data class Info(val mensaje: String) : EventoCategoria
    data class Error(val mensaje: String) : EventoCategoria
    /** Pide confirmar el archivado mostrando cuántas prendas cuelgan (null = no se pudo contar). */
    data class ConfirmarArchivar(val categoria: CategoriaResponse, val prendas: Int?) : EventoCategoria
}

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val repo: TaxonomiaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<CategoriaResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoCategoria>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.categorias()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedWith(compareBy({ it.archivada == true }, { it.nombre })))
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun crear(nombre: String) = ejecutar("Categoria creada.") { repo.crearCategoria(nombre) }

    fun renombrar(id: UUID, nombre: String) = ejecutar("Categoria renombrada.") {
        repo.renombrarCategoria(id, nombre)
    }

    fun activar(id: UUID) = ejecutar("Categoria activada.") { repo.activarCategoria(id) }

    /** Antes de archivar, cuenta las prendas y pide confirmación. */
    fun solicitarArchivar(categoria: CategoriaResponse) {
        val id = categoria.id ?: return
        viewModelScope.launch {
            val conteo = (repo.conteoPrendas(id) as? RespuestaRed.Exito)?.data
            _eventos.tryEmit(EventoCategoria.ConfirmarArchivar(categoria, conteo))
        }
    }

    fun archivar(id: UUID) = ejecutar("Categoria archivada.") { repo.archivarCategoria(id) }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<CategoriaResponse>) {
        viewModelScope.launch {
            when (val r = accion()) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoCategoria.Info(exito))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCategoria.Error(r.error.mensaje))
            }
        }
    }
}
