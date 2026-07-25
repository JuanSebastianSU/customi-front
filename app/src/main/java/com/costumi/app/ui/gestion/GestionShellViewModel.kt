package com.costumi.app.ui.gestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.Rol
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.ContextoGestionRepository
import com.costumi.app.data.repo.MembresiaRepository
import com.costumi.apiclient.models.CambiarContextoRequest
import com.costumi.apiclient.models.SucursalResponse
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
    private val contexto: ContextoGestionRepository,
) : ViewModel() {

    private val _rol = MutableStateFlow<Rol?>(null)
    val rol = _rol.asStateFlow()

    private val _cerrada = MutableStateFlow(false)
    val cerrada = _cerrada.asStateFlow()

    /** true cuando el cambio a modo compra ya guardó el token; el shell navega al modo cliente. */
    private val _irAComprar = MutableStateFlow(false)
    val irAComprar = _irAComprar.asStateFlow()

    /** Secciones a las que el usuario tiene acceso (paso 5); null mientras carga → no filtra. */
    private val _misSecciones = MutableStateFlow<Set<String>?>(null)
    val misSecciones = _misSecciones.asStateFlow()

    /** Sucursales de la empresa para el selector (A3); vacío = sin multi-sucursal o sin permiso. */
    private val _sucursales = MutableStateFlow<List<SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    init {
        viewModelScope.launch {
            (repo.rolActual() as? RespuestaRed.Exito)?.let { _rol.value = it.data }
        }
        viewModelScope.launch {
            (contexto.misSecciones() as? RespuestaRed.Exito)?.let { _misSecciones.value = it.data }
        }
        viewModelScope.launch {
            (contexto.sucursales() as? RespuestaRed.Exito)?.let { _sucursales.value = it.data }
        }
    }

    fun sucursalActivaId(): String? = contexto.sucursalActivaId

    /** Fija la sucursal activa (null = todas). El interceptor la manda como X-Sucursal-Id. */
    fun elegirSucursal(id: String?) {
        contexto.sucursalActivaId = id
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
