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
 * Los avisos que manda el backend viajan con bloque `notification`, asi que **Android los muestra solo**
 * cuando la app esta en segundo plano; este servicio no tiene que dibujarlas. Lo que si atiende es el
 * cambio de token: Firebase lo rota (reinstalar, limpiar datos, restaurar el equipo) y si no se reenvia,
 * el backend queda con uno viejo y las notificaciones dejan de llegar en silencio.
 */
@AndroidEntryPoint
class ServicioDeMensajeria : FirebaseMessagingService() {

    @Inject
    lateinit var push: PushRepository

    /** El servicio vive fuera de cualquier pantalla: necesita su propio alcance. */
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        alcance.launch { push.registrar(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        super.onMessageReceived(mensaje)
        // Con la app abierta no se interrumpe al usuario con una notificacion del sistema: los avisos ya
        // se ven en la pantalla de Notificaciones. Aqui solo quedaria mostrar un aviso propio si hiciera
        // falta en el futuro.
    }
}
