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
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
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
}
