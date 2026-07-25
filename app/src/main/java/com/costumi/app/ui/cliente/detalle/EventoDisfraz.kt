package com.costumi.app.ui.cliente.detalle

import com.costumi.apiclient.models.AgregarItemRequest

/** Eventos de una sola vez del detalle del disfraz: agregado al carrito o error. */
sealed interface EventoDisfraz {
    /** El disfraz se agregó al carrito: hay que ir al carrito de ese tipo para ver el total y confirmar. */
    data class Agregado(
        val sucursalId: String,
        val empresaId: String,
        val tipo: AgregarItemRequest.Tipo,
    ) : EventoDisfraz

    data class Error(val mensaje: String) : EventoDisfraz
}
