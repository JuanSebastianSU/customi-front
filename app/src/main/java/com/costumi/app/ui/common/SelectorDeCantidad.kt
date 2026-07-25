package com.costumi.app.ui.common

import androidx.core.view.isVisible
import com.costumi.app.databinding.WidgetCantidadBinding

/**
 * Enlaza el stepper de cantidad (`widget_cantidad`) con un valor. Unico en la app: cliente, punto de
 * venta, armar renta y agregar disfraz piden la cantidad igual, con el mismo tope de stock a la vista.
 *
 * [maximo] normalmente es el stock disponible: al llegar ahi el "+" se apaga y se dice por que, en vez
 * de dejar escribir 99 y fallar al cobrar.
 */
class SelectorDeCantidad(
    private val binding: WidgetCantidadBinding,
    private val minimo: Int = 1,
    maximo: Int? = null,
    inicial: Int = 1,
    private val alCambiar: (Int) -> Unit = {},
) {
    var maximo: Int? = maximo
        set(valor) {
            field = valor
            if (valor != null && cantidad > valor) cantidad = valor else pintar()
        }

    var cantidad: Int = inicial.coerceAtLeast(minimo)
        set(valor) {
            val acotado = valor.coerceAtLeast(minimo).let { v -> maximo?.let { minOf(v, it) } ?: v }
            if (field == acotado) { pintar(); return }
            field = acotado
            pintar()
            alCambiar(acotado)
        }

    init {
        binding.botonMenos.setOnClickListener { cantidad -= 1 }
        binding.botonMas.setOnClickListener { cantidad += 1 }
        pintar()
    }

    /** Texto de la izquierda (por defecto "Cantidad"). */
    fun etiqueta(texto: String) {
        binding.etiqueta.text = texto
    }

    private fun pintar() {
        binding.textoCantidad.text = cantidad.toString()
        binding.botonMenos.isEnabled = cantidad > minimo
        binding.botonMenos.alpha = if (binding.botonMenos.isEnabled) 1f else 0.4f
        val tope = maximo
        val enElTope = tope != null && cantidad >= tope
        binding.botonMas.isEnabled = !enElTope
        binding.botonMas.alpha = if (enElTope) 0.4f else 1f
        binding.limite.isVisible = tope != null
        binding.limite.text = when {
            tope == null -> ""
            enElTope -> "es todo el stock"
            else -> "hay $tope"
        }
    }
}
