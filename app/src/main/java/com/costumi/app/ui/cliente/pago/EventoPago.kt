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

    /**
     * Pago con tarjeta **aprobado (simulado)**: se creó la orden y este es su código de retiro. La
     * pasarela real (MercadoPago) no está configurada; el pago con tarjeta se simula del lado de la app
     * para la demo (los datos de la tarjeta no salen del dispositivo). [codigo] null si se crearon varias
     * rentas.
     */
    data class TarjetaAprobada(val codigo: String?) : EventoPago

    data class Error(val mensaje: String) : EventoPago
}
