package com.costumi.app.ui.gestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.Rol
import com.costumi.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del shell de Gestion: resuelve el [Rol] del usuario (via /auth/me) para filtrar la
 * navegacion inferior, y expone el cierre de sesion.
 */
@HiltViewModel
class GestionShellViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _rol = MutableStateFlow<Rol?>(null)
    val rol = _rol.asStateFlow()

    private val _cerrada = MutableStateFlow(false)
    val cerrada = _cerrada.asStateFlow()

    init {
        viewModelScope.launch {
            (repo.rolActual() as? RespuestaRed.Exito)?.let { _rol.value = it.data }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            repo.logout()
            _cerrada.value = true
        }
    }
}
