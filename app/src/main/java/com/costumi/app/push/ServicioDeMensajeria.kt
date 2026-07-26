package com.costumi.app.push

import com.costumi.app.data.repo.PushRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recibe las notificaciones push de Firebase (RF-18.11).
 *
 * Los avisos del backend viajan con bloque `notification`. Android los muestra solo cuando la app esta en
 * **segundo plano**; con la app en **primer plano** ese bloque NO se dibuja solo (antes no aparecia nada),
 * asi que aca lo dibujamos nosotros. Tambien atiende el cambio de token: Firebase lo rota (reinstalar,
 * limpiar datos, restaurar el equipo) y si no se reenvia, el backend queda con uno viejo y las
 * notificaciones dejan de llegar en silencio.
 */
@AndroidEntryPoint
class ServicioDeMensajeria : FirebaseMessagingService() {

    @Inject
    lateinit var push: PushRepository

    /** El servicio vive fuera de cualquier pantalla: necesita su propio alcance. */
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Si el usuario apago las push en este dispositivo, no reenganchamos el token al backend.
        if (!Notificaciones.activadasPorUsuario(this)) return
        alcance.launch { push.registrar(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        super.onMessageReceived(mensaje)
        // App en primer plano: Firebase no dibuja el bloque `notification`, lo hacemos nosotros para que
        // el aviso se vea igual (antes, con la app abierta, no aparecia nada).
        val titulo = mensaje.notification?.title ?: mensaje.data["title"] ?: "Costumi"
        val cuerpo = mensaje.notification?.body ?: mensaje.data["body"] ?: return
        Notificaciones.mostrarAviso(this, titulo, cuerpo)
    }
}
