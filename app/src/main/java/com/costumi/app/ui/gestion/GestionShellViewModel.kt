package com.costumi.app.ui.gestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.Rol
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.MembresiaRepository
import com.costumi.apiclient.models.CambiarContextoRequest
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
    private val membresiaRepo: MembresiaRepository,
) : ViewModel() {

    private val _rol = MutableStateFlow<Rol?>(null)
    val rol = _rol.asStateFlow()

    private val _cerrada = MutableStateFlow(false)
    val cerrada = _cerrada.asStateFlow()

    /** true cuando el cambio a modo compra ya guardó el token; el shell navega al modo cliente. */
    private val _irAComprar = MutableStateFlow(false)
    val irAComprar = _irAComprar.asStateFlow()

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

    /** Vuelve a «modo compra»: cambia el contexto (token de cliente) y navega al shell de cliente. */
    fun irAComprar() {
        viewModelScope.launch {
            if (membresiaRepo.cambiarContexto(CambiarContextoRequest.Modo.COMPRA) is RespuestaRed.Exito) {
                _irAComprar.value = true
            }
        }
    }
}
