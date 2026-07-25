package com.costumi.app.ui.gestion.devoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DevolucionRepository
import com.costumi.apiclient.models.DevolucionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Historial de devoluciones liquidadas de la empresa (solo lectura). */
@HiltViewModel
class DevolucionesViewModel @Inject constructor(
    private val repo: DevolucionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<DevolucionResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

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
            _estado.value = when (val r = repo.historial(buscar)) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data.sortedByDescending { it.id?.toString() })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }
}
