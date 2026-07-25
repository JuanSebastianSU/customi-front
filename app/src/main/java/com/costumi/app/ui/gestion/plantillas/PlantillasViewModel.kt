package com.costumi.app.ui.gestion.plantillas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.PlantillaRepository
import com.costumi.apiclient.models.PlantillaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventoPlantilla {
    data class Info(val mensaje: String) : EventoPlantilla
    data class Error(val mensaje: String) : EventoPlantilla
}

/** Mensajes automaticos: lista las 6 plantillas y persiste texto + switch on/off. */
@HiltViewModel
class PlantillasViewModel @Inject constructor(
    private val repo: PlantillaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<PlantillaResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPlantilla>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.plantillas()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedBy { CategoriaMensaje.de(it.tipo).orden })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Guarda el texto editado (mantiene el estado on/off actual). */
    fun guardarTexto(tipo: String, texto: String, activa: Boolean) = actualizar(tipo, texto, activa, "Plantilla guardada.")

    /** Enciende/apaga la automatizacion (mantiene el texto actual). */
    fun cambiarActiva(tipo: String, texto: String, activa: Boolean) =
        actualizar(tipo, texto, activa, if (activa) "Automatizacion activada." else "Automatizacion desactivada.")

    private fun actualizar(tipo: String, texto: String, activa: Boolean, exito: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.actualizar(tipo, texto, activa)) {
                is RespuestaRed.Exito -> { _eventos.tryEmit(EventoPlantilla.Info(exito)); cargar() }
                is RespuestaRed.Fallo -> { _eventos.tryEmit(EventoPlantilla.Error(r.error.mensaje)); cargar() }
            }
            _procesando.value = false
        }
    }
}
