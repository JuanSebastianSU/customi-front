package com.costumi.app.ui.gestion.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CajaRepository
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TurnoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

sealed interface EventoCaja {
    data class Info(val mensaje: String) : EventoCaja
    data class Error(val mensaje: String) : EventoCaja
}

/** Lista de turnos de caja (abiertos y cerrados) y apertura de un nuevo turno. */
@HiltViewModel
class CajaViewModel @Inject constructor(
    private val repo: CajaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<TurnoResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _sucursales = MutableStateFlow<List<SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoCaja>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
        cargarSucursales()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.turnos()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    // Abiertos primero; el backend no ordena, así se ve el turno vigente arriba.
                    else UiState.Success(r.data.sortedByDescending { it.estado == "ABIERTO" })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    private fun cargarSucursales() {
        viewModelScope.launch {
            (repo.sucursales() as? RespuestaRed.Exito)?.let { _sucursales.value = it.data }
        }
    }

    fun abrir(sucursalId: UUID, fondoInicial: BigDecimal) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.abrir(sucursalId, fondoInicial)) {
                is RespuestaRed.Exito -> { _eventos.tryEmit(EventoCaja.Info("Turno abierto.")); cargar() }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCaja.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
