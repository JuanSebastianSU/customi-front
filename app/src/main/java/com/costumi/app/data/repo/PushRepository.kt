package com.costumi.app.data.repo

import android.util.Log
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.models.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro del dispositivo para notificaciones push (RF-18.11).
 *
 * El token lo emite Firebase y hay que mandarlo al backend, que lo guarda en las fichas del usuario. Se
 * registra al iniciar sesion y cada vez que Firebase lo rota (ver `ServicioDeMensajeria`).
 */
@Singleton
class PushRepository @Inject constructor(
    private val api: ClienteControllerApi,
    private val sesion: SesionLocal,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /**
     * Pide el token a Firebase y lo registra. Sin sesion no hace nada: el backend lo asocia al usuario
     * del token de autenticacion, asi que registrarlo antes de entrar no tendria a quien asociarlo.
     *
     * Un fallo aca **no se le muestra al usuario**: no poder registrar el dispositivo no debe romper el
     * inicio de sesion; a lo sumo no llegan las push hasta el proximo intento.
     */
    suspend fun registrarDispositivo() {
        if (!sesion.haySesion()) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase no entrego un token de dispositivo", e)
            return
        }
        registrar(token)
    }

    /** Registra un token ya conocido (lo usa el servicio cuando Firebase lo rota). */
    suspend fun registrar(token: String) {
        if (!sesion.haySesion() || token.isBlank()) return
        withContext(dispatchers.io) {
            when (val r = ejecutarLlamada(gson) { api.registrarMiDeviceToken(DeviceTokenRequest(token)) }) {
                is RespuestaRed.Exito -> Log.i(TAG, "Dispositivo registrado para push")
                is RespuestaRed.Fallo -> Log.w(TAG, "No se pudo registrar el dispositivo: ${r.error.mensaje}")
            }
        }
    }

    private companion object {
        const val TAG = "Push"
    }
}
