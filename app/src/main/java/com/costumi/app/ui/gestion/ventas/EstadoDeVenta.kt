package com.costumi.app.ui.gestion.ventas

import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.VentaResponse

/** Cómo se lee el estado de una venta en la lista: etiqueta legible + tono de la pastilla. */
object EstadoDeVenta {

    fun etiqueta(v: VentaResponse): String = when (v.estado?.uppercase()) {
        "CONFIRMADA" -> "Confirmada"
        "PARCIALMENTE_DEVUELTA" -> "Devuelta en parte"
        "DEVUELTA" -> "Devuelta"
        else -> v.estado.orEmpty()
    }

    fun tono(v: VentaResponse): Tono = when (v.estado?.uppercase()) {
        "CONFIRMADA" -> Tono.EXITO
        "PARCIALMENTE_DEVUELTA" -> Tono.ALERTA
        "DEVUELTA" -> Tono.NEUTRO
        else -> Tono.NEUTRO
    }

    /** Unidades que todavía se pueden devolver (0 = ya no hay nada que devolver). */
    fun unidadesDevolvibles(v: VentaResponse): Int =
        v.lineas.orEmpty().sumOf { ((it.cantidad ?: 0) - (it.cantidadDevuelta ?: 0)).coerceAtLeast(0) }
}
