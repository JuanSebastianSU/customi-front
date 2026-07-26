package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.Rol
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.InvitacionControllerApi
import com.costumi.apiclient.models.AceptarInvitacionRequest
import com.costumi.apiclient.models.InvitacionVistaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aceptación de una invitación de trabajo (Fase B) DENTRO de la app: previsualiza la invitación por su
 * token y la acepta (T&C + contraseña). Al aceptar, el backend devuelve una sesión ya en modo trabajo
 * (empresa+rol de la membresía) que se guarda como cualquier login; el llamador re-navega por rol.
 */
@Singleton
class InvitacionRepository @Inject constructor(
    private val api: InvitacionControllerApi,
    private val sesion: SesionLocal,
    private val authRepo: AuthRepository,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Preview de la invitación: empresa, rol, email y si la persona necesita crear cuenta. */
    suspend fun ver(token: String): RespuestaRed<InvitacionVistaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.ver1(token) } }

    /** Acepta: guarda la sesión resultante y devuelve el rol para navegar. */
    suspend fun aceptar(token: String, password: String, aceptaTerminos: Boolean): RespuestaRed<Rol> =
        withContext(dispatchers.io) {
            when (val r = ejecutarLlamada(gson) {
                api.aceptar(AceptarInvitacionRequest(token = token, password = password, aceptaTerminos = aceptaTerminos))
            }) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> {
                    sesion.guardar(r.data)
                    authRepo.rolActual()
                }
            }
        }
}
