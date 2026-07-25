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
class RestablecerViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoAuth>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun restablecer(token: String, password: String, confirmar: String) {
        if (_cargando.value) return
        when {
            token.isBlank() || password.isBlank() ->
                _eventos.tryEmit(EventoAuth.Error("Completa el codigo y la nueva contrasena."))
            password.length < 8 ->
                _eventos.tryEmit(EventoAuth.Error("La contrasena debe tener al menos 8 caracteres."))
            password != confirmar ->
                _eventos.tryEmit(EventoAuth.Error("Las contrasenas no coinciden."))
            else -> viewModelScope.launch {
                _cargando.value = true
                when (val r = repo.restablecer(token.trim(), password)) {
                    is RespuestaRed.Exito -> _eventos.tryEmit(
                        EventoAuth.Info("Tu contrasena se actualizo. Ya puedes iniciar sesion."),
                    )
                    is RespuestaRed.Fallo -> _eventos.tryEmit(EventoAuth.Error(r.error.mensaje))
                }
                _cargando.value = false
            }
        }
    }
}
