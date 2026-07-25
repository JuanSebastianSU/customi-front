package com.costumi.app.ui.gestion.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ConfiguracionRepository
import com.costumi.apiclient.models.ConfiguracionRequest
import com.costumi.apiclient.models.ConfiguracionResponse
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventoConfig {
    data object Guardada : EventoConfig
    /** La config se exportó: el fragment guarda este JSON en un archivo de respaldo. */
    data class Exportada(val json: String) : EventoConfig
    data object Importada : EventoConfig
    data class Error(val mensaje: String) : EventoConfig
}

/** Configuración de la empresa: cargar el estado actual, guardar (reemplazo total) y respaldar/restaurar. */
@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val repo: ConfiguracionRepository,
    private val gson: Gson,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<ConfiguracionResponse>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoConfig>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.obtener()) {
                is RespuestaRed.Exito -> UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun guardar(req: ConfiguracionRequest) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            when (val r = repo.actualizar(req)) {
                is RespuestaRed.Exito -> { _estado.value = UiState.Success(r.data); _eventos.tryEmit(EventoConfig.Guardada) }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoConfig.Error(r.error.mensaje))
            }
            _guardando.value = false
        }
    }

    fun exportar() {
        viewModelScope.launch {
            when (val r = repo.exportar()) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoConfig.Exportada(gson.toJson(r.data)))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoConfig.Error(r.error.mensaje))
            }
        }
    }

    fun importar(json: String) {
        val req = runCatching { gson.fromJson(json, ConfiguracionRequest::class.java) }.getOrNull()
        if (req == null) {
            _eventos.tryEmit(EventoConfig.Error("El archivo no es un respaldo valido."))
            return
        }
        viewModelScope.launch {
            when (val r = repo.importar(req)) {
                is RespuestaRed.Exito -> { _estado.value = UiState.Success(r.data); _eventos.tryEmit(EventoConfig.Importada) }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoConfig.Error(r.error.mensaje))
            }
        }
    }
}
