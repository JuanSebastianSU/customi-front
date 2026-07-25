package com.costumi.app.ui.cliente.pago

/** Eventos de una sola vez de la pantalla de pago. */
sealed interface EventoPago {
    /** Abrir el checkout de la pasarela (tarjeta) en el navegador. */
    data class AbrirCheckout(val url: String) : EventoPago

    /** El cliente eligió pagar en la tienda (efectivo): se confirma y va a Mis Pedidos. */
    data object PagoEnTienda : EventoPago

    data class Error(val mensaje: String) : EventoPago
}
