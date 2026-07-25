package com.costumi.app.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.costumi.app.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Barra de filtros COMPACTA reutilizable: una sola fila horizontal con un chip por dimensión. Tocar un chip
 * abre un selector CON BUSCADOR (multi-selección). Escala a cientos de valores sin muro de chips. La usan
 * el selector de prendas (armar disfraz, punto de venta) y el picker de disfraces del POS.
 */
object FiltroCompacto {

    /** Una dimensión de la barra: su nombre, los valores seleccionados (para el rótulo), y qué hacer al tocarla. */
    data class Dimension(val nombre: String, val seleccion: List<String>, val alTocar: () -> Unit)

    /** Pinta la barra (fila de chips con scroll horizontal) dentro de [contenedor]; se oculta si no hay dimensiones. */
    fun pintarBarra(inflater: LayoutInflater, contenedor: LinearLayout, dimensiones: List<Dimension>) {
        contenedor.removeAllViews()
        val grupo = ChipGroup(contenedor.context).apply { isSingleLine = true }
        for (dim in dimensiones) {
            val chip = inflater.inflate(R.layout.chip_filtro, null) as Chip
            chip.isCheckable = false
            chip.text = if (dim.seleccion.isEmpty()) {
                dim.nombre
            } else {
                val valores = dim.seleccion.joinToString(", ")
                dim.nombre + ": " + if (valores.length > 22) valores.take(20) + "…" else valores
            }
            chip.isChecked = dim.seleccion.isNotEmpty()
            chip.setOnClickListener { dim.alTocar() }
            grupo.addView(chip)
        }
        contenedor.addView(
            HorizontalScrollView(contenedor.context).apply {
                isHorizontalScrollBarEnabled = false
                addView(grupo)
            },
        )
        contenedor.isVisible = grupo.childCount > 0
    }

    /**
     * Abre el selector con buscador (multi) de una dimensión y devuelve la nueva selección de ids. [opciones]
     * son los valores de la dimensión (con su conteo como subtítulo); [seleccion] los ya marcados.
     */
    fun abrirDimension(
        context: Context,
        titulo: String,
        opciones: List<OpcionBuscable>,
        seleccion: Set<String>,
        alAplicar: (Set<String>) -> Unit,
    ) {
        ListaBuscable.variasOpciones(context, titulo, opciones, seleccion, alListo = alAplicar)
    }
}
