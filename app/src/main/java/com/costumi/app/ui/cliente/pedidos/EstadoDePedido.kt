package com.costumi.app.ui.cliente.pedidos

import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.HistorialItem

/**
 * Cómo se lee un pedido del cliente (renta o venta unificadas): etiqueta legible del estado, tono de la
 * pastilla y a qué **filtro** de la pantalla pertenece.
 *
 * Estados posibles — renta: `RESERVADA`, `ACTIVA`, `DEVUELTA`, `CERRADA`, `CANCELADA`;
 * venta: `CONFIRMADA`, `PARCIALMENTE_DEVUELTA`, `DEVUELTA`; y `REEMBOLSADA`.
 *
 * Pago: `HistorialItem` expone `estadoPago` y `saldoPendiente`; con eso se arma el filtro «Por pagar» y la
 * etiqueta de pago de cada pedido.
 */
object EstadoDePedido {

    enum class Filtro(val etiqueta: String) {
        TODOS("Todos"),
        POR_PAGAR("Por pagar"),
        POR_RETIRAR("Por retirar"),
        ACTIVOS("Activos"),
        CERRADOS("Cerrados"),
    }

    /** ¿El pedido tiene saldo pendiente? (define el filtro «Por pagar»). */
    fun tieneSaldo(item: HistorialItem): Boolean = (item.saldoPendiente?.signum() ?: 0) > 0

    /** Etiqueta de pago para la fila: "Falta $X" si hay saldo, "Pagado" si está saldado; null si no aplica. */
    fun pagoEtiqueta(item: HistorialItem): String? = when {
        tieneSaldo(item) -> "Falta ${item.saldoPendiente.comoPrecio()}"
        item.estadoPago?.uppercase() == "PAGADO" -> "Pagado"
        else -> null
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
    fun aplicar(pedidos: List<HistorialItem>, filtro: Filtro): List<HistorialItem> = when (filtro) {
        Filtro.TODOS -> pedidos
        Filtro.POR_PAGAR -> pedidos.filter { tieneSaldo(it) }
        else -> pedidos.filter { bucket(it.estado) == filtro }
    }
}
