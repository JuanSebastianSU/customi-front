package com.costumi.app.ui.gestion.sucursales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
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

    /** true tras el primer refresco: recién ahí una caché vacía significa "no hay sucursales" (Empty). */
    private var yaRefresco = false

    /** true cuando se muestra caché porque el refresco falló por falta de red (aviso N4). */
    private val _sinConexion = MutableStateFlow(false)
    val sinConexion = _sinConexion.asStateFlow()

    init {
        // Cache-first: la pantalla se pinta desde Room y se actualiza sola cuando el refresco escribe.
        viewModelScope.launch {
            repo.observarSucursales().collect { lista ->
                if (lista.isNotEmpty()) {
                    _estado.value = UiState.Success(lista.sortedByDescending { it.archivada != true })
                } else if (yaRefresco) {
                    _estado.value = UiState.Empty
                }
            }
        }
        cargar()
    }

    /** Refresca desde la red hacia Room. No tapa la caché con un spinner: solo muestra Loading/Error si aún no hay datos. */
    fun cargar() {
        viewModelScope.launch {
            if (_estado.value !is UiState.Success) _estado.value = UiState.Loading
            when (val r = repo.refrescarSucursales()) {
                is RespuestaRed.Exito -> {
                    yaRefresco = true
                    _sinConexion.value = false
                    // Room no re-emite si la tabla ya estaba vacía: si la red confirma 0, mostrar Empty (no cargar sin fin).
                    if (r.data == 0 && _estado.value !is UiState.Success) _estado.value = UiState.Empty
                }
                is RespuestaRed.Fallo -> {
                    yaRefresco = true
                    val hayCache = _estado.value is UiState.Success
                    _sinConexion.value = hayCache && r.error.tipo == TipoError.SIN_CONEXION
                    if (!hayCache) _estado.value = UiState.Error(r.error.mensaje) { cargar() }
                }
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
