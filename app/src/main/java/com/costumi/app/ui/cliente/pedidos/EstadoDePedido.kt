package com.costumi.app.ui.cliente.pedidos

import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.HistorialItem

/**
 * Cómo se lee un pedido del cliente (renta o venta unificadas): etiqueta legible del estado, tono de la
 * pastilla y a qué **filtro** de la pantalla pertenece.
 *
 * Estados posibles — renta: `RESERVADA`, `ACTIVA`, `DEVUELTA`, `CERRADA`, `CANCELADA`;
 * venta: `CONFIRMADA`, `PARCIALMENTE_DEVUELTA`, `DEVUELTA`; y `REEMBOLSADA`.
 *
 * NOTA: el filtro **«Por pagar»** de la spec no está — `HistorialItem` no expone si el pedido está pagado.
 * Se agrega cuando el backend traiga `estadoPago`/`saldoPendiente` (ver lote de backend en PROGRESS.md).
 */
object EstadoDePedido {

    enum class Filtro(val etiqueta: String) {
        TODOS("Todos"),
        POR_RETIRAR("Por retirar"),
        ACTIVOS("Activos"),
        CERRADOS("Cerrados"),
    }

    fun etiqueta(estado: String?): String = when (estado?.uppercase()) {
        "RESERVADA" -> "Por retirar"
        "ACTIVA" -> "Activo"
        "CONFIRMADA" -> "Confirmado"
        "DEVUELTA" -> "Devuelto"
        "PARCIALMENTE_DEVUELTA" -> "Devuelto en parte"
        "CERRADA" -> "Cerrado"
        "CANCELADA" -> "Cancelado"
        "REEMBOLSADA" -> "Reembolsado"
        else -> estado?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-"
    }

    fun tono(estado: String?): Tono = when (estado?.uppercase()) {
        "RESERVADA" -> Tono.INFO
        "ACTIVA", "CONFIRMADA" -> Tono.EXITO
        "PARCIALMENTE_DEVUELTA" -> Tono.ALERTA
        "CANCELADA" -> Tono.ERROR
        else -> Tono.NEUTRO // DEVUELTA, CERRADA, REEMBOLSADA
    }

    fun bucket(estado: String?): Filtro = when (estado?.uppercase()) {
        "RESERVADA" -> Filtro.POR_RETIRAR
        "ACTIVA", "CONFIRMADA" -> Filtro.ACTIVOS
        else -> Filtro.CERRADOS
    }

    /** Aplica el filtro (TODOS = sin filtrar) sobre la lista completa del historial. */
    fun aplicar(pedidos: List<HistorialItem>, filtro: Filtro): List<HistorialItem> =
        if (filtro == Filtro.TODOS) pedidos else pedidos.filter { bucket(it.estado) == filtro }
}
