package com.costumi.app.ui.gestion.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ClientesRepository
import com.costumi.apiclient.models.HistorialItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClienteHistorialViewModel @Inject constructor(
    private val repo: ClientesRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val clienteId: UUID = UUID.fromString(estado[ClienteHistorialFragment.ARG_ID]!!)
    val clienteNombre: String = estado[ClienteHistorialFragment.ARG_NOMBRE] ?: "Cliente"

    private val _estado = MutableStateFlow<UiState<List<HistorialItem>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.historial(clienteId)) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }
}
