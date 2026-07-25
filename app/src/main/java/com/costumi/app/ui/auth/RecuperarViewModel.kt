package com.costumi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecuperarViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoAuth>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun enviar(email: String) {
        if (_cargando.value) return
        if (email.isBlank()) {
            _eventos.tryEmit(EventoAuth.Error("Ingresa tu correo."))
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            when (val r = repo.olvide(email.trim())) {
                is RespuestaRed.Exito -> _eventos.tryEmit(
                    EventoAuth.Info("Si el correo existe, te enviamos instrucciones para restablecer tu contrasena."),
                )
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoAuth.Error(r.error.mensaje))
            }
            _cargando.value = false
        }
    }
}
