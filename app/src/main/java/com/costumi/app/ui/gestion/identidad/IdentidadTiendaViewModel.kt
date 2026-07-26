package com.costumi.app.ui.gestion.identidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.MiEmpresaRepository
import com.costumi.apiclient.models.EmpresaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventoIdentidad {
    data class Info(val mensaje: String) : EventoIdentidad
    data class Error(val mensaje: String) : EventoIdentidad
}

/** Identidad de la tienda (RF-18.3): logo y portada que ve el cliente en el marketplace. */
@HiltViewModel
class IdentidadTiendaViewModel @Inject constructor(
    private val repo: MiEmpresaRepository,
) : ViewModel() {

    private val _empresa = MutableStateFlow<EmpresaResponse?>(null)
    val empresa = _empresa.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoIdentidad>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init { cargar() }

    private fun cargar() {
        viewModelScope.launch {
            (repo.mia() as? RespuestaRed.Exito)?.let { _empresa.value = it.data }
        }
    }

    fun editar(nombre: String, descripcion: String?, ciudad: String?, ubicacion: String?, contacto: String?) {
        if (_procesando.value) return
        if (nombre.isBlank()) { _eventos.tryEmit(EventoIdentidad.Error("El nombre es obligatorio.")); return }
        subir("Datos de la tienda actualizados.") { repo.editar(nombre, descripcion, ciudad, ubicacion, contacto) }
    }

    fun subirLogo(bytes: ByteArray, mime: String, nombre: String) =
        subir("Logo actualizado.") { repo.subirLogo(bytes, mime, nombre) }

    fun subirPortada(bytes: ByteArray, mime: String, nombre: String) =
        subir("Portada actualizada.") { repo.subirPortada(bytes, mime, nombre) }

    private fun subir(exito: String, accion: suspend () -> RespuestaRed<EmpresaResponse>) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito -> { _empresa.value = r.data; _eventos.tryEmit(EventoIdentidad.Info(exito)) }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoIdentidad.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
