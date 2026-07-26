package com.costumi.app.ui.gestion.sucursales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.SucursalRepository
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoSucursal {
    data class Info(val mensaje: String) : EventoSucursal
    data class Error(val mensaje: String) : EventoSucursal
}

/** Sucursales de la empresa: listar, alta, editar y archivar/activar. */
@HiltViewModel
class SucursalesViewModel @Inject constructor(
    private val repo: SucursalRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<SucursalResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoSucursal>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.sucursales()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty
                    else UiState.Success(r.data.sortedByDescending { it.archivada != true })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun crear(nombre: String, direccion: String?, ubicacionMaps: String?) =
        ejecutar("Sucursal creada.") { repo.crear(nombre, direccion, ubicacionMaps) }
    fun editar(id: UUID, nombre: String, direccion: String?, ubicacionMaps: String?) =
        ejecutar("Sucursal actualizada.") { repo.editar(id, nombre, direccion, ubicacionMaps) }
    fun archivar(id: UUID) = ejecutar("Sucursal archivada.") { repo.archivar(id) }
    fun activar(id: UUID) = ejecutar("Sucursal activada.") { repo.activar(id) }
    fun subirFoto(id: UUID, bytes: ByteArray, mime: String, nombre: String) =
        ejecutar("Foto de la tienda actualizada.") { repo.subirFoto(id, bytes, mime, nombre) }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<SucursalResponse>) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito -> { _eventos.tryEmit(EventoSucursal.Info(exito)); cargar() }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoSucursal.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
