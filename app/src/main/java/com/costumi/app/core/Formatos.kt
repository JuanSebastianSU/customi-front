package com.costumi.app.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val formatoMoneda: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("es", "CO"))
}

/** Formatea un monto como precio (p. ej. "$ 45.000"). Devuelve null si el monto es null. */
fun BigDecimal?.comoPrecio(): String? = this?.let { formatoMoneda.format(it) }
