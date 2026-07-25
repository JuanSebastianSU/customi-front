package com.costumi.app.ui.gestion.inventario

import kotlinx.coroutines.flow.flatMapLatest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.InventarioRepository
import com.costumi.app.data.repo.TipoConValores
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.PrendaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Eventos de una sola vez del inventario (resultado de archivar/activar). */
sealed interface EventoInventario {
    data class Info(val mensaje: String) : EventoInventario
    data class Error(val mensaje: String) : EventoInventario
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val repo: InventarioRepository,
) : ViewModel() {

    /** Flujo paginado (lista de gestión: crear/editar/stock/archivar), cacheado al scope del VM. */
    /** Texto de busqueda; al cambiar se vuelve a pedir la primera pagina. */
    private val _buscar = MutableStateFlow<String?>(null)

    /** El usuario escribio en la caja de busqueda. */
    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    val prendas = _buscar.flatMapLatest { repo.prendas(it) }.cachedIn(viewModelScope)

    private val _stockBajo = MutableStateFlow(0)
    val stockBajo = _stockBajo.asStateFlow()

    /** Ids de las prendas que tienen alguna variante con stock bajo: se marcan en la lista. */
    private val _prendasConStockBajo = MutableStateFlow<Set<UUID>>(emptySet())
    val prendasConStockBajo = _prendasConStockBajo.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoInventario>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    // --- Explorar por categoría + filtros por etiqueta (muestra el stock disponible) ---

    private val _categorias = MutableStateFlow<List<CategoriaResponse>>(emptyList())
    val categorias = _categorias.asStateFlow()

    /** Tipos de etiqueta con sus valores, para el diálogo de filtros (color/talla) y para pintar nombres. */
    private val _tipos = MutableStateFlow<List<TipoConValores>>(emptyList())
    val tipos = _tipos.asStateFlow()

    /** Categoría seleccionada (null = "Todas"). */
    private val _categoriaSel = MutableStateFlow<UUID?>(null)
    val categoriaSel = _categoriaSel.asStateFlow()

    /** Valores de etiqueta elegidos por dimensión (tipoId -> valorIds). */
    private val _etiquetasSel = MutableStateFlow<Map<UUID, Set<UUID>>>(emptyMap())
    val etiquetasSel = _etiquetasSel.asStateFlow()

    /** ¿Hay algún filtro activo? Con filtro se muestra el catálogo (con stock); sin filtro, la lista de gestión. */
    val filtroActivo = combine(_categoriaSel, _etiquetasSel) { cat, etq ->
        cat != null || etq.values.any { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _catalogo = MutableStateFlow<UiState<List<PrendaDeCatalogoResponse>>>(UiState.Loading)
    val catalogo = _catalogo.asStateFlow()

    init {
        cargarStockBajo()
        cargarFiltros()
    }

    private fun cargarFiltros() {
        viewModelScope.launch {
            (repo.categorias() as? RespuestaRed.Exito)?.let { _categorias.value = it.data }
            (repo.tiposConValores() as? RespuestaRed.Exito)?.let { _tipos.value = it.data }
        }
    }

    fun seleccionarCategoria(id: UUID?) {
        if (_categoriaSel.value == id) return
        _categoriaSel.value = id
        recargarSiFiltra()
    }

    fun aplicarEtiquetas(seleccion: Map<UUID, Set<UUID>>) {
        _etiquetasSel.value = seleccion.filterValues { it.isNotEmpty() }
        recargarSiFiltra()
    }

    /** Recalcula si hay filtro (sin depender del StateFlow combinado, que se actualiza async). */
    private fun recargarSiFiltra() {
        val activo = _categoriaSel.value != null || _etiquetasSel.value.values.any { it.isNotEmpty() }
        if (activo) recargarCatalogo()
    }

    fun limpiarFiltros() {
        _categoriaSel.value = null
        _etiquetasSel.value = emptyMap()
        // Sin filtros se vuelve a la lista de gestión; no hace falta cargar el catálogo.
    }

    fun recargarCatalogo() {
        viewModelScope.launch {
            _catalogo.value = UiState.Loading
            val etiquetas = _etiquetasSel.value.flatMap { (tipo, valores) -> valores.map { "$tipo:$it" } }
            when (val r = repo.catalogo(_categoriaSel.value, etiquetas)) {
                is RespuestaRed.Fallo -> _catalogo.value = UiState.Error(r.error.mensaje) { recargarCatalogo() }
                is RespuestaRed.Exito ->
                    _catalogo.value = if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
            }
        }
    }

    /** valorEtiquetaId -> nombre, para mostrar "Rojo · M" en cada stock del catálogo. */
    fun nombresDeValores(): Map<UUID, String> =
        _tipos.value.flatMap { it.valores }
            .mapNotNull { v -> v.id?.let { it to v.valor.orEmpty() } }
            .toMap()

    fun cargarStockBajo() {
        viewModelScope.launch {
            (repo.stockBajo() as? RespuestaRed.Exito)?.let { r ->
                _stockBajo.value = r.data.size
                _prendasConStockBajo.value = r.data.mapNotNull { it.prendaId }.toSet()
            }
        }
    }

    fun alternarArchivado(prenda: PrendaResponse) {
        val id = prenda.id ?: return
        val archivada = prenda.archivada == true
        viewModelScope.launch {
            val r = if (archivada) repo.activar(id) else repo.archivar(id)
            when (r) {
                is RespuestaRed.Exito -> _eventos.tryEmit(
                    EventoInventario.Info(if (archivada) "Prenda activada." else "Prenda archivada."),
                )
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoInventario.Error(r.error.mensaje))
            }
        }
    }
}
