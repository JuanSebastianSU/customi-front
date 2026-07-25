package com.costumi.app.ui.gestion.plantillas

/**
 * Metadatos de presentacion de cada tipo de mensaje automatico (RF-11): etiqueta legible, orden y las
 * variables que aplican a esa categoria (para mostrarlas como chips en el editor). El tipo tecnico
 * (MULTA_GENERADA, etc.) viene del backend.
 */
object CategoriaMensaje {

    data class Meta(val etiqueta: String, val descripcion: String, val orden: Int, val variables: List<String>)

    private val POR_TIPO = mapOf(
        "COMPRA_REALIZADA" to Meta("Agradecimiento por compra",
            "Se envia al confirmarse una venta a un cliente.", 1,
            listOf("cliente", "direccion", "maps")),
        "RENTA_CONFIRMADA" to Meta("Confirmacion de renta",
            "Se envia al entregar una renta.", 2,
            listOf("cliente", "fecha_devolucion", "direccion", "maps")),
        "RECORDATORIO_DEVOLUCION" to Meta("Recordatorio de devolucion",
            "Se envia unos dias antes de que venza la renta.", 3,
            listOf("cliente", "fecha_devolucion", "dias_restantes", "direccion", "maps")),
        "RENTA_VENCIDA" to Meta("Renta vencida",
            "Se envia cuando la renta ya vencio.", 4,
            listOf("cliente", "fecha_devolucion")),
        "MULTA_GENERADA" to Meta("Multa generada",
            "Se envia cuando una devolucion genera una multa.", 5,
            listOf("cliente", "monto")),
        "DEUDA_SALDADA" to Meta("Deuda saldada",
            "Se envia cuando el cliente termina de pagar una multa.", 6,
            listOf("cliente", "monto")),
    )

    private val DEFAULT = Meta("Mensaje", "", 99, listOf("cliente"))

    fun de(tipo: String?): Meta = POR_TIPO[tipo] ?: DEFAULT

    /** Valores de ejemplo para la vista previa del editor: así se ve cómo le llega al cliente. */
    private val EJEMPLOS = mapOf(
        "cliente" to "Ana Torres",
        "direccion" to "Calle 80 #12-34, Bogota",
        "maps" to "maps.google.com/?q=Calle+80",
        "fecha_devolucion" to "15 ago",
        "dias_restantes" to "3",
        "monto" to "$ 100.000",
    )

    private val VARIABLE = Regex("\\{([a-z_]+)\\}")

    /** Reemplaza cada `{variable}` por un valor de ejemplo; las desconocidas se dejan tal cual. */
    fun previsualizar(texto: String): String =
        VARIABLE.replace(texto) { m -> EJEMPLOS[m.groupValues[1]] ?: m.value }
}
