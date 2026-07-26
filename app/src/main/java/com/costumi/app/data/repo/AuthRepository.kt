package com.costumi.app.data.repo

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.Rol
import com.costumi.app.core.TipoError
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.dao.SucursalDao
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.models.LoginRequest
import com.costumi.apiclient.models.OlvideRequest
import com.costumi.apiclient.models.RefreshRequest
import com.costumi.apiclient.models.RegistroRequest
import com.costumi.apiclient.models.RestablecerRequest
import com.costumi.apiclient.models.UsuarioActualResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autenticacion: login/registro/olvide/restablecer/logout y resolucion del rol via /auth/me.
 * Al autenticar guarda los tokens en [SesionLocal]; el rol se obtiene del servidor para decidir
 * la navegacion. Todo corre en IO y pasa por [ejecutarLlamada] (errores RFC 7807 uniformes).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthControllerApi,
    private val sesion: SesionLocal,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
    private val push: PushRepository,
    private val miEmpresa: MiEmpresaRepository,
    private val favoritos: FavoritoDao,
    private val sucursales: SucursalDao,
) {
    fun haySesion(): Boolean = sesion.haySesion()

    suspend fun login(email: String, password: String): RespuestaRed<Rol> =
        withContext(dispatchers.io) {
            when (val r = ejecutarLlamada(gson) { api.login(LoginRequest(email, password)) }) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> {
                    sesion.guardar(r.data)
                    // Con sesion recien abierta ya se puede asociar el dispositivo al usuario (RF-18.11).
                    push.registrarDispositivo()
                    rolActual()
                }
            }
        }

    suspend fun registro(email: String, password: String): RespuestaRed<Rol> =
        withContext(dispatchers.io) {
            when (val r = ejecutarLlamada(gson) { api.registro(RegistroRequest(email, password)) }) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> {
                    sesion.guardar(r.data)
                    // Con sesion recien abierta ya se puede asociar el dispositivo al usuario (RF-18.11).
                    push.registrarDispositivo()
                    rolActual()
                }
            }
        }

    /** Datos del usuario autenticado (email/rol/empresa), p. ej. para el Perfil. */
    suspend fun me(): RespuestaRed<UsuarioActualResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { api.me() }
    }

    /** Consulta /auth/me y traduce el rol. Sirve para el Splash (sesion ya guardada) y tras login. */
    suspend fun rolActual(): RespuestaRed<Rol> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { api.me() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                val rol = Rol.desde(r.data.rol)
                if (rol == null) {
                    RespuestaRed.Fallo(
                        ErrorApi(TipoError.DESCONOCIDO, "Rol no reconocido: ${r.data.rol}"),
                    )
                } else {
                    RespuestaRed.Exito(rol)
                }
            }
        }
    }

    suspend fun olvide(email: String): RespuestaRed<Unit> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { api.olvide(OlvideRequest(email)) }
    }

    suspend fun restablecer(token: String, password: String): RespuestaRed<Unit> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.restablecer(RestablecerRequest(token, password)) }
        }

    /** Cierra sesion server-side (revoca la familia) best-effort y borra siempre los tokens locales. */
    suspend fun logout(): RespuestaRed<Unit> = withContext(dispatchers.io) {
        val refresh = sesion.refreshToken
        if (!refresh.isNullOrBlank()) {
            runCatching { ejecutarLlamada(gson) { api.logout(RefreshRequest(refresh)) } }
        }
        sesion.limpiar()
        // La tienda cacheada es de la sesion que se cierra: si no, el proximo dueno veria el nombre anterior.
        miEmpresa.limpiar()
        // Los favoritos guardados son de la cuenta que se cierra: no deben verse en la proxima sesion.
        favoritos.limpiar()
        // Las sucursales cacheadas son de la empresa de esta sesion (norma N1 del plan de Room).
        sucursales.limpiar()
        RespuestaRed.Exito(Unit)
    }
}
