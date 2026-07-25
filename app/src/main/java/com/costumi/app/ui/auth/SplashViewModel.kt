package com.costumi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.ModoApp
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Destino inicial que decide el Splash: entrar a un modo (sesion valida) o ir a Login. */
sealed interface DestinoSplash {
    data class Home(val modo: ModoApp) : DestinoSplash
    data object Login : DestinoSplash
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _destino = MutableSharedFlow<DestinoSplash>(replay = 1)
    val destino = _destino.asSharedFlow()

    // Mensaje de error recuperable (p. ej. sin conexion con sesion guardada) → mostrar Reintentar.
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        evaluar()
    }

    fun evaluar() {
        viewModelScope.launch {
            _error.value = null
            if (!repo.haySesion()) {
                _destino.emit(DestinoSplash.Login)
                return@launch
            }
            when (val r = repo.rolActual()) {
                is RespuestaRed.Exito -> _destino.emit(DestinoSplash.Home(r.data.modo))
                is RespuestaRed.Fallo -> when (r.error.tipo) {
                    TipoError.NO_AUTORIZADO -> _destino.emit(DestinoSplash.Login)
                    else -> _error.value = r.error.mensaje
                }
            }
        }
    }
}
