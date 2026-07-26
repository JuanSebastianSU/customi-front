package com.costumi.app.ui.cliente.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.app.data.repo.MembresiaRepository
import com.costumi.app.data.repo.PerfilRepository
import com.costumi.app.data.repo.PushRepository
import com.costumi.apiclient.models.CambiarContextoRequest
import com.costumi.apiclient.models.MembresiaActiva
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Eventos de una sola vez del Perfil. */
sealed interface EventoPerfil {
    data object SesionCerrada : EventoPerfil
    data object IrAGestion : EventoPerfil
    data class Info(val mensaje: String) : EventoPerfil
    data class Error(val mensaje: String) : EventoPerfil
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val cuentaRepo: CuentaRepository,
    private val perfilRepo: PerfilRepository,
    private val membresiaRepo: MembresiaRepository,
    private val pushRepo: PushRepository,
) : ViewModel() {

    private val _email = MutableStateFlow<String?>(null)
    val email = _email.asStateFlow()

    /** Membresía de trabajo activa de la persona (null = solo cliente): habilita el botón «Trabajar». */
    private val _membresiaActiva = MutableStateFlow<MembresiaActiva?>(null)
    val membresiaActiva = _membresiaActiva.asStateFlow()

    /** Datos editables de la cuenta; null mientras no se cargaron. */
    private val _perfil = MutableStateFlow<com.costumi.apiclient.models.PerfilResponse?>(null)
    val perfil = _perfil.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPerfil>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        // Cache-first: el perfil se pinta desde Room (sin parpadeo) y se refresca por detrás.
        observarPerfil()
        cargarPerfil()
        cargarMembresia()
    }

    private fun observarPerfil() {
        viewModelScope.launch {
            perfilRepo.observarPerfil().collect { p ->
                if (p != null) {
                    _perfil.value = p
                    _email.value = p.email
                }
            }
        }
    }

    /** Lee la membresía activa de /auth/me para decidir si mostrar «Entrar a trabajar» y «Desvincularme». */
    private fun cargarMembresia() {
        viewModelScope.launch {
            (authRepo.me() as? RespuestaRed.Exito)?.let { _membresiaActiva.value = it.data.membresiaActiva }
        }
    }

    /** Entra a «modo trabajo»: cambia el contexto (token nuevo) y navega al shell de gestión. */
    fun entrarATrabajar() {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = membresiaRepo.cambiarContexto(CambiarContextoRequest.Modo.TRABAJO)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoPerfil.IrAGestion)
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** El empleado se desvincula de su tienda: queda solo cliente. */
    fun desvincularme() {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = membresiaRepo.desvincularme()) {
                is RespuestaRed.Exito -> {
                    _membresiaActiva.value = null
                    _eventos.tryEmit(EventoPerfil.Info("Te desvinculaste de la tienda. Ahora sos solo cliente."))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** Refresca el perfil desde la red hacia Room (la UI se actualiza por el Flow de [observarPerfil]). */
    private fun cargarPerfil() {
        viewModelScope.launch {
            when (perfilRepo.refrescarPerfil()) {
                is RespuestaRed.Exito -> Unit // los datos llegan por el Flow de Room
                // Si el perfil no carga y aún no hay caché, al menos se muestra el correo de la sesion.
                is RespuestaRed.Fallo ->
                    if (_email.value == null) {
                        (authRepo.me() as? RespuestaRed.Exito)?.let { _email.value = it.data.email }
                    }
            }
        }
    }

    /** Sube la foto de perfil elegida y refresca el avatar con la URL nueva. */
    fun subirFoto(bytes: ByteArray, mime: String, nombre: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = perfilRepo.subirFoto(bytes, mime, nombre)) {
                is RespuestaRed.Exito -> {
                    _perfil.value = r.data
                    _eventos.tryEmit(EventoPerfil.Info("Foto de perfil actualizada."))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** Guarda nombre y telefono. Vacio borra el dato: son opcionales. */
    fun guardarDatos(nombre: String, telefono: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = perfilRepo.actualizar(nombre, telefono)) {
                is RespuestaRed.Exito -> {
                    _perfil.value = r.data
                    _eventos.tryEmit(EventoPerfil.Info("Datos guardados."))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** Cambia la contrasena; el backend exige la actual aunque la sesion este abierta. */
    fun cambiarContrasena(actual: String, nueva: String, repetida: String) {
        if (_procesando.value) return
        when {
            actual.isBlank() || nueva.isBlank() ->
                return _eventos.tryEmit(EventoPerfil.Error("Completa las dos contrasenas.")).let { }
            nueva != repetida ->
                return _eventos.tryEmit(EventoPerfil.Error("La contrasena nueva no coincide.")).let { }
            nueva.length < 8 ->
                return _eventos.tryEmit(
                    EventoPerfil.Error("La contrasena nueva debe tener al menos 8 caracteres."),
                ).let { }
        }
        viewModelScope.launch {
            _procesando.value = true
            when (val r = perfilRepo.cambiarContrasena(actual, nueva)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoPerfil.Info("Contrasena actualizada."))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    /** El usuario activa las push en este dispositivo (registra el token). El permiso lo pide la pantalla. */
    fun activarNotificaciones() {
        viewModelScope.launch { pushRepo.activar() }
    }

    /** El usuario apaga las push en este dispositivo (borra el token de Firebase). */
    fun desactivarNotificaciones() {
        viewModelScope.launch { pushRepo.desactivar() }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            authRepo.logout()
            _eventos.tryEmit(EventoPerfil.SesionCerrada)
        }
    }

    fun registrarTienda(nombre: String, ubicacion: String?, contacto: String?) {
        if (_procesando.value) return
        if (nombre.isBlank()) {
            _eventos.tryEmit(EventoPerfil.Error("Ingresa el nombre de tu tienda."))
            return
        }
        viewModelScope.launch {
            _procesando.value = true
            when (val r = cuentaRepo.registrarTienda(nombre.trim(), ubicacion?.trim(), contacto?.trim())) {
                is RespuestaRed.Exito -> _eventos.tryEmit(
                    EventoPerfil.Info(
                        "¡Solicitud enviada! Cuando aprueben tu tienda, vuelve a iniciar sesion para gestionarla.",
                    ),
                )
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPerfil.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
