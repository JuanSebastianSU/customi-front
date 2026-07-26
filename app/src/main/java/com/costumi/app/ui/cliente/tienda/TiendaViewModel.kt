package com.costumi.app.ui.cliente.tienda

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.MarketplaceRepository
import com.costumi.app.ui.cliente.explorar.ExplorarFragment
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Catalogo de una tienda con dos apartados: DISFRACES y PRENDAS. El empresaId llega por argumentos
 * de navegacion (SavedStateHandle). Cada apartado tiene su propio estado y se recarga por separado.
 */
@HiltViewModel
class TiendaViewModel @Inject constructor(
    private val repo: MarketplaceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val empresaId: String = savedStateHandle[ExplorarFragment.ARG_EMPRESA_ID] ?: ""
    val nombreTienda: String = savedStateHandle[ExplorarFragment.ARG_NOMBRE] ?: "Tienda"

    private val _disfraces = MutableStateFlow<UiState<List<DisfrazResponse>>>(UiState.Loading)
    val disfraces = _disfraces.asStateFlow()

    private val _prendas = MutableStateFlow<UiState<List<PrendaVitrinaResponse>>>(UiState.Loading)
    val prendas = _prendas.asStateFlow()

    /** Descripción de la tienda (para la cabecera); null si no la cargó o no tiene. */
    private val _descripcion = MutableStateFlow<String?>(null)
    val descripcion = _descripcion.asStateFlow()

    init {
        cargarDisfraces()
        cargarPrendas()
        cargarDetalle()
    }

    private fun cargarDetalle() {
        viewModelScope.launch {
            (repo.detalleEmpresa(empresaId) as? RespuestaRed.Exito)?.data?.descripcion
                ?.takeIf { it.isNotBlank() }?.let { _descripcion.value = it }
        }
    }

    fun cargarDisfraces() {
        viewModelScope.launch {
            _disfraces.value = UiState.Loading
            _disfraces.value = when (val r = repo.disfraces(empresaId)) {
                is RespuestaRed.Exito -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargarDisfraces() }
            }
        }
    }

    fun cargarPrendas() {
        viewModelScope.launch {
            _prendas.value = UiState.Loading
            _prendas.value = when (val r = repo.catalogo(empresaId)) {
                is RespuestaRed.Exito -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargarPrendas() }
            }
        }
    }
}
