package com.costumi.app.data.repo

import android.content.Context
import android.util.Log
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.app.push.Notificaciones
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.models.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro del dispositivo para notificaciones push (RF-18.11).
 *
 * El token lo emite Firebase y hay que mandarlo al backend, que lo guarda en las fichas del usuario. Se
 * registra al iniciar sesion y cada vez que Firebase lo rota (ver `ServicioDeMensajeria`), salvo que el
 * usuario haya **apagado** las push en este dispositivo (toggle de Perfil/Configuracion).
 */
@Singleton
class PushRepository @Inject constructor(
    private val api: ClienteControllerApi,
    private val sesion: SesionLocal,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context,
) {
    /**
     * Pide el token a Firebase y lo registra. Sin sesion no hace nada: el backend lo asocia al usuario
     * del token de autenticacion, asi que registrarlo antes de entrar no tendria a quien asociarlo. Si el
     * usuario apago las push en este dispositivo, tampoco registra.
     *
     * Un fallo aca **no se le muestra al usuario**: no poder registrar el dispositivo no debe romper el
     * inicio de sesion; a lo sumo no llegan las push hasta el proximo intento.
     */
    suspend fun registrarDispositivo() {
        if (!sesion.haySesion()) return
        if (!Notificaciones.activadasPorUsuario(context)) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase no entrego un token de dispositivo", e)
            return
        }
        registrar(token)
    }

    /**
     * El usuario ACTIVA las push en este dispositivo: reanuda la auto-inicializacion de Firebase y
     * registra el token en el backend. (El permiso del sistema lo pide la pantalla; esto es la parte de
     * datos.)
     */
    suspend fun activar() {
        Notificaciones.fijarActivadas(context, true)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        registrarDispositivo()
    }

    /**
     * El usuario DESACTIVA las push en este dispositivo: borra el token de Firebase (deja de recibir push
     * aunque el permiso del sistema siga concedido) y apaga la auto-inicializacion para que no se regenere
     * solo. No hay endpoint para "des-registrar" en el backend; borrar el token del lado del dispositivo
     * corta la entrega igual.
     */
    suspend fun desactivar() {
        Notificaciones.fijarActivadas(context, false)
        FirebaseMessaging.getInstance().isAutoInitEnabled = false
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo borrar el token de Firebase", e)
        }
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
