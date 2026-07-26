package com.costumi.app.ui.cliente.pago

/** Eventos de una sola vez de la pantalla de pago. */
sealed interface EventoPago {
    /** Abrir el checkout de la pasarela (tarjeta) en el navegador. */
    data class AbrirCheckout(val url: String) : EventoPago

    /**
     * El pedido se materializó al confirmar "pagar en la tienda" (efectivo): recién ahí existe la orden y
     * su código de retiro. [codigo] es el código a mostrar (null si el checkout creó varias rentas).
     */
    data class Reservado(val codigo: String?) : EventoPago

    data class Error(val mensaje: String) : EventoPago
}
