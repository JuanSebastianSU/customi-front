package com.costumi.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.costumi.app.MainActivity
import com.costumi.app.R

/**
 * ##Todo lo de notificaciones push que vive del lado del sistema (no del backend):
 * - el **canal** (Android 8+ exige uno; sin canal propio la push cae en un canal fabricante que algunos
 *   equipos esconden o descartan),
 * - **dibujar** el aviso cuando la app está en primer plano (Firebase solo lo muestra solo en segundo
 *   plano; con la app abierta no aparecía nada),
 * - la **preferencia local** de activado/desactivado por dispositivo (lo que el usuario prende/apaga).
 */
object Notificaciones {

    /** Id del canal; se referencia también desde el manifiesto como canal por defecto de FCM. */
    const val CANAL_AVISOS_ID = "avisos"
    private const val CANAL_AVISOS_NOMBRE = "Avisos de Costumi"
    private const val PREFS = "notificaciones"
    private const val KEY_ACTIVADAS = "activadas"

    /** Crea el canal de avisos (idempotente). Se llama al iniciar la app. */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_AVISOS_ID,
            CANAL_AVISOS_NOMBRE,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Recordatorios de rentas, avisos de tu tienda y novedades." }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(canal)
    }

    /**
     * Dibuja un aviso propio (para cuando la app está en primer plano). Si el permiso no está concedido,
     * [NotificationManagerCompat.notify] no muestra nada y no rompe: el chequeo lo hace el sistema.
     */
    fun mostrarAviso(context: Context, titulo: String, cuerpo: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val abrir = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val aviso = NotificationCompat.Builder(context, CANAL_AVISOS_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(cuerpo.hashCode(), aviso)
        } catch (_: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS en Android 13+: no se muestra, y no debe romper nada.
        }
    }

    /**
     * ¿El usuario quiere recibir push en ESTE dispositivo? Preferencia local (por defecto sí). Es la parte
     * que el usuario controla con el toggle; el permiso del sistema es aparte (lo maneja Android).
     */
    fun activadasPorUsuario(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVADAS, true)

    fun fijarActivadas(context: Context, activadas: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACTIVADAS, activadas).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
