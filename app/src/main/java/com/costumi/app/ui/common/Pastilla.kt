package com.costumi.app.ui.common

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.costumi.app.R
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

/**
 * Tono de una pastilla de estado. El color no decide nada: solo repite lo que ya dice el texto, para
 * que una lista larga se lea de un vistazo. Mismo tono = mismo significado en toda la app.
 */
enum class Tono {
    /** Terminado, sin nada que hacer (cerrada, cancelada, archivada). */
    NEUTRO,

    /** En curso, esperando al cliente (reservada, pendiente). */
    INFO,

    /** Todo en orden (activa en fecha, pagada, disponible). */
    EXITO,

    /** Requiere atención pronto (vence hoy o mañana, falta cerrar). */
    ALERTA,

    /** Problema: hay que actuar ya (vencida, sin stock, rechazada). */
    ERROR,
}

/** Color del texto que acompaña a la pastilla (la frase que explica el estado), en el mismo tono. */
fun Tono.colorDeTexto(vista: View): Int = when (this) {
    Tono.ERROR -> MaterialColors.getColor(vista, androidx.appcompat.R.attr.colorError)
    Tono.ALERTA -> ContextCompat.getColor(vista.context, R.color.estado_alerta_texto)
    Tono.EXITO -> ContextCompat.getColor(vista.context, R.color.estado_exito_texto)
    Tono.INFO -> MaterialColors.getColor(vista, com.google.android.material.R.attr.colorOnSurface)
    Tono.NEUTRO -> MaterialColors.getColor(vista, com.google.android.material.R.attr.colorOnSurfaceVariant)
}

/** Pinta el chip como pastilla de estado, sin que sea seleccionable ni clickeable. */
fun Chip.pintarPastilla(texto: String, tono: Tono) {
    // Éxito y alerta no salen del tema: Material no tiene "bien" ni "ojo", y usar sus contenedores de
    // marca pintaba de rosa una renta que va perfecta.
    val fondo = when (tono) {
        Tono.NEUTRO -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant)
        Tono.INFO -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondaryContainer)
        Tono.EXITO -> ContextCompat.getColor(context, R.color.estado_exito_fondo)
        Tono.ALERTA -> ContextCompat.getColor(context, R.color.estado_alerta_fondo)
        Tono.ERROR -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorErrorContainer)
    }
    val letra = when (tono) {
        Tono.NEUTRO -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
        Tono.INFO -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSecondaryContainer)
        Tono.EXITO -> ContextCompat.getColor(context, R.color.estado_exito_texto)
        Tono.ALERTA -> ContextCompat.getColor(context, R.color.estado_alerta_texto)
        Tono.ERROR -> MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnErrorContainer)
    }
    this.text = texto
    chipBackgroundColor = ColorStateList.valueOf(fondo)
    setTextColor(letra)
    isClickable = false
    isCheckable = false
}
