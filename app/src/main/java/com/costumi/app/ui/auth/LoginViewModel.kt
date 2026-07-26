package com.costumi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.InvitacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val invitaciones: InvitacionRepository,
) : ViewModel() {

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoAuth>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun login(email: String, password: String) {
        if (_cargando.value) return
        if (email.isBlank() || password.isBlank()) {
            _eventos.tryEmit(EventoAuth.Error("Ingresa tu correo y contrasena."))
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            when (val r = repo.login(email.trim(), password)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoAuth.Navegar(r.data.modo))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoAuth.Error(r.error.mensaje))
            }
            _cargando.value = false
        }
    }

    /** Del texto pegado (enlace o código) saca el token: si es una URL, el último segmento. */
    private fun tokenDe(texto: String): String =
        texto.trim().substringAfterLast('/').substringBefore('?').trim()

    /** Previsualiza una invitación por su enlace/código, para que la persona vea a qué acepta. */
    fun verInvitacion(textoEnlace: String) {
        if (_cargando.value) return
        val token = tokenDe(textoEnlace)
        if (token.isBlank()) { _eventos.tryEmit(EventoAuth.Error("Pegá el enlace o código de la invitación.")); return }
        viewModelScope.launch {
            _cargando.value = true
            when (val r = invitaciones.ver(token)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoAuth.InvitacionLista(r.data, token))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoAuth.Error(r.error.mensaje))
            }
            _cargando.value = false
        }
    }

    /** Acepta la invitación (T&C + contraseña) y navega al home según el rol de trabajo. */
    fun aceptarInvitacion(token: String, password: String, aceptaTerminos: Boolean) {
        if (_cargando.value) return
        if (!aceptaTerminos) { _eventos.tryEmit(EventoAuth.Error("Tenés que aceptar los términos para unirte.")); return }
        if (password.length < 8) { _eventos.tryEmit(EventoAuth.Error("La contraseña debe tener al menos 8 caracteres.")); return }
        viewModelScope.launch {
            _cargando.value = true
            when (val r = invitaciones.aceptar(token, password, aceptaTerminos)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoAuth.Navegar(r.data.modo))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoAuth.Error(r.error.mensaje))
            }
            _cargando.value = false
        }
    }
}
