package com.costumi.app.ui.common

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.costumi.app.databinding.SheetElegirPiezaBinding
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.cliente.tienda.DisfrazVitrinaAdapter
import com.costumi.apiclient.models.DisfrazResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Selector visual REUTILIZABLE de disfraces: grilla de vitrina (foto, piezas, precio) con buscador y
 * filtro compacto por categoria. Lo usan **vender** y **rentar** para agregar un disfraz al pedido, de
 * modo que la experiencia sea la misma que al elegir un articulo suelto ([SelectorCatalogo]).
 */
object SelectorDisfraces {

    fun abrir(
        fragment: Fragment,
        disfraces: List<DisfrazResponse>,
        titulo: String = "Agregar disfraz",
        onElegir: (DisfrazResponse) -> Unit,
    ) {
        val ctx = fragment.requireContext()
        val sheetBinding = SheetElegirPiezaBinding.inflate(fragment.layoutInflater)
        val hoja = BottomSheetDialog(ctx)
        hoja.setContentView(sheetBinding.root)
        (sheetBinding.root.parent as? View)?.let { contenedorSheet ->
            BottomSheetBehavior.from(contenedorSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isDraggable = false // deslizar solo scrollea la grilla; el sheet no se cierra al deslizar.
            }
            contenedorSheet.layoutParams = contenedorSheet.layoutParams.apply {
                height = (fragment.resources.displayMetrics.heightPixels * 0.92).toInt()
            }
        }
        sheetBinding.titulo.text = titulo
        sheetBinding.botonListo.isVisible = false

        val categoriasSel = linkedSetOf<String>()
        var texto = ""
        val adapter = DisfrazVitrinaAdapter { disfraz ->
            hoja.dismiss()
            onElegir(disfraz)
        }
        sheetBinding.listaOpciones.adapter = adapter

        fun render() {
            val filtrados = disfraces.filter { categoriasSel.isEmpty() || (it.categoria ?: "") in categoriasSel }
            val visibles = if (texto.isBlank()) {
                filtrados
            } else {
                filtrados.filter { it.nombre.orEmpty().contains(texto, ignoreCase = true) }
            }
            adapter.submitList(visibles)
            sheetBinding.subtitulo.text =
                (if (filtrados.size == 1) "1 disfraz" else "${filtrados.size} disfraces") + " · toca para configurarlo"
            sheetBinding.listaOpciones.isVisible = visibles.isNotEmpty()
            sheetBinding.textoVacio.isVisible = visibles.isEmpty()
            sheetBinding.textoVacio.text =
                if (filtrados.isEmpty() && categoriasSel.isNotEmpty()) "Ningun disfraz con esa categoria."
                else "Ningun disfraz coincide con lo que buscaste."
            sheetBinding.botonLimpiarFiltros.isVisible = categoriasSel.isNotEmpty()

            val categorias = disfraces.mapNotNull { it.categoria?.takeIf { c -> c.isNotBlank() } }.distinct()
            val dimensiones = if (categorias.size >= 2) {
                listOf(
                    FiltroCompacto.Dimension("Categoria", categoriasSel.toList()) {
                        val opciones = categorias.sortedBy { it.lowercase() }.map { c ->
                            val n = disfraces.count { (it.categoria ?: "") == c }
                            OpcionBuscable(c, c, "$n ${if (n == 1) "disfraz" else "disfraces"}")
                        }
                        FiltroCompacto.abrirDimension(ctx, "Categoria", opciones, categoriasSel.toSet()) { ids ->
                            categoriasSel.clear(); categoriasSel.addAll(ids); render()
                        }
                    },
                )
            } else {
                emptyList()
            }
            FiltroCompacto.pintarBarra(fragment.layoutInflater, sheetBinding.contenedorFiltros, dimensiones)
        }
        sheetBinding.botonLimpiarFiltros.setOnClickListener { categoriasSel.clear(); render() }
        sheetBinding.editBuscar.alBuscar { t -> texto = t; render() }
        render()
        hoja.show()
    }
}
