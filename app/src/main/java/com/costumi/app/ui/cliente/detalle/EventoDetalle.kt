package com.costumi.app.ui.cliente.detalle

import com.costumi.apiclient.models.AgregarItemRequest

/** Resultado de "agregar al carrito": navegar al carrito de ese tipo, o mostrar error. */
sealed interface EventoDetalle {
    data class Agregado(val sucursalId: String, val tipo: AgregarItemRequest.Tipo) : EventoDetalle
    data class Error(val mensaje: String) : EventoDetalle
}
