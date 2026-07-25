package com.costumi.app.ui.gestion.clientes

import androidx.fragment.app.Fragment
import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.gestion.mostrarDesglose
import com.costumi.apiclient.models.EstadoDeCuentaResponse

/**
 * Desglose de la deuda de un cliente: una línea por renta (importe, multa, pagado y lo que queda
 * debiendo) más los totales. Vive aquí porque lo abren DOS pantallas —la lista de clientes y su
 * ficha—, y tienen que contar exactamente lo mismo.
 */
fun Fragment.mostrarEstadoDeCuenta(nombreCliente: String, estado: EstadoDeCuentaResponse) {
    val lineas = estado.lineas.orEmpty().map { l ->
        val partes = listOfNotNull(
            l.importe?.comoPrecio()?.let { "Renta $it" },
            l.multa?.takeIf { it.signum() > 0 }?.comoPrecio()?.let { "multa $it" },
            l.pagado?.takeIf { it.signum() > 0 }?.comoPrecio()?.let { "pagado $it" },
        ).joinToString("  ·  ")
        val titulo = listOfNotNull(l.codigoRetiro, l.fechaRetiro?.toString()).joinToString("  ·  ")
            .ifBlank { "Renta" }
        LineaDesglose(
            fotoUrl = null,
            nombre = titulo,
            detalle = partes.ifBlank { l.estado },
            monto = l.saldo?.takeIf { it.signum() > 0 }?.comoPrecio()?.let { "Debe $it" },
        )
    }
    val pie = buildList {
        add("Saldo total" to (estado.saldoTotal.comoPrecio() ?: "$0"))
        estado.multaTotal?.takeIf { it.signum() > 0 }?.let { add("Multa total" to (it.comoPrecio() ?: "-")) }
    }
    mostrarDesglose(
        titulo = "Estado de cuenta · $nombreCliente",
        subtitulo = if (lineas.isEmpty()) "Este cliente no tiene deuda." else "Detalle por renta",
        lineas = lineas,
        pie = pie,
    )
}
