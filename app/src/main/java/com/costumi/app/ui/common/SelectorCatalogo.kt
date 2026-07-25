package com.costumi.app.ui.common

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.costumi.app.data.repo.TipoConValores
import com.costumi.app.databinding.SheetElegirPiezaBinding
import com.costumi.app.ui.alBuscar
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.UUID

/**
 * Selector visual REUTILIZABLE del catálogo de prendas: grilla (foto, precio, stock) con buscador y barra de
 * filtros compacta (Categoría + Talla/Color, con conteo dinámico). Lo usan **armar disfraz** y el **punto de
 * venta**, para que la experiencia sea idéntica. Modo [multiple]: se marcan varias y se confirma con "Listo";
 * si no, un toque elige y cierra. Filtrado en memoria (sin llamadas extra).
 */
object SelectorCatalogo {

    fun abrirPrendas(
        fragment: Fragment,
        catalogo: List<PrendaDeCatalogoResponse>,
        categorias: List<CategoriaResponse>,
        etiquetas: List<TipoConValores>,
        multiple: Boolean,
        titulo: String,
        yaElegidas: Set<UUID> = emptySet(),
        onElegir: (PrendaDeCatalogoResponse) -> Unit = {},
        onConfirmar: (List<PrendaDeCatalogoResponse>) -> Unit = {},
    ) {
        val ctx = fragment.requireContext()
        val nombreCategoria = categorias.associate { it.id to it.nombre.orEmpty() }
        val nombreTipo = etiquetas.associate { it.tipo.id to it.tipo.nombre.orEmpty() }
        val nombreValor = etiquetas.flatMap { it.valores }.associate { it.id to it.valor.orEmpty() }

        val categoriasSel = linkedSetOf<UUID>()
        val etiquetaSel = linkedMapOf<UUID, MutableSet<UUID>>()
        var texto = ""
        val elegidas = yaElegidas.toMutableSet()

        val sheetBinding = SheetElegirPiezaBinding.inflate(fragment.layoutInflater)
        val hoja = BottomSheetDialog(ctx)
        hoja.setContentView(sheetBinding.root)
        // Sheet alto y expandido: sin esto, arrastrar la grilla arrastraba/cerraba el sheet.
        (sheetBinding.root.parent as? View)?.let { contenedorSheet ->
            BottomSheetBehavior.from(contenedorSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                // No arrastrable: deslizar SOLO scrollea la grilla; el sheet no se mueve ni se cierra al
                // deslizar (se cierra con atrás o tocando afuera). Sin esto el sheet era muy sensible.
                isDraggable = false
            }
            contenedorSheet.layoutParams = contenedorSheet.layoutParams.apply {
                height = (fragment.resources.displayMetrics.heightPixels * 0.92).toInt()
            }
        }
        sheetBinding.titulo.text = titulo
        sheetBinding.botonListo.isVisible = multiple

        var adapter: PrendaCatalogoGrillaAdapter? = null

        fun render() {
            val filtradas = catalogo.filter { cumpleFiltro(it, categoriasSel, etiquetaSel) }
            val visibles = if (texto.isBlank()) {
                filtradas
            } else {
                filtradas.filter { it.nombre.orEmpty().contains(texto, ignoreCase = true) }
            }
            adapter?.seleccionados = elegidas.toSet()
            adapter?.submitList(visibles)
            sheetBinding.subtitulo.text =
                (if (filtradas.size == 1) "1 prenda" else "${filtradas.size} prendas") +
                    if (multiple) " · marca las que quieras" else " · toca una"
            val vacio = visibles.isEmpty()
            sheetBinding.listaOpciones.isVisible = !vacio
            sheetBinding.textoVacio.isVisible = vacio
            sheetBinding.textoVacio.text =
                if (filtradas.isEmpty() && (categoriasSel.isNotEmpty() || etiquetaSel.isNotEmpty())) {
                    "Ninguna prenda con esos filtros. Quita alguno."
                } else {
                    "Ninguna prenda coincide con lo que buscaste."
                }
            sheetBinding.botonLimpiarFiltros.isVisible = categoriasSel.isNotEmpty() || etiquetaSel.isNotEmpty()
            if (multiple) {
                sheetBinding.botonListo.text = if (elegidas.isEmpty()) "Listo" else "Listo (${elegidas.size})"
            }

            val dimensiones = mutableListOf<FiltroCompacto.Dimension>()
            val categoriasPresentes = catalogo.mapNotNull { it.categoriaId }.distinct()
            if (categoriasPresentes.size >= 2) {
                dimensiones.add(
                    FiltroCompacto.Dimension("Categoria", categoriasSel.map { nombreCategoria[it].orEmpty() }) {
                        // Conteo dinámico: excluye el propio filtro de categoría.
                        val base = catalogo.filter { cumpleFiltro(it, emptySet(), etiquetaSel) }
                        val opciones = categoriasPresentes.sortedBy { nombreCategoria[it].orEmpty().lowercase() }.map { c ->
                            val n = base.count { it.categoriaId == c }
                            OpcionBuscable(c.toString(), nombreCategoria[c].orEmpty(), "$n ${if (n == 1) "prenda" else "prendas"}")
                        }
                        FiltroCompacto.abrirDimension(ctx, "Categoria", opciones, categoriasSel.map { it.toString() }.toSet()) { ids ->
                            categoriasSel.clear()
                            categoriasSel.addAll(ids.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() })
                            render()
                        }
                    },
                )
            }
            val tipos = catalogo.flatMap { it.etiquetas.orEmpty().mapNotNull { e -> e.tipoEtiquetaId } }.distinct()
            for (tipo in tipos) {
                val valores = catalogo
                    .mapNotNull { p -> p.etiquetas.orEmpty().firstOrNull { it.tipoEtiquetaId == tipo }?.valorEtiquetaId }
                    .distinct()
                if (valores.isEmpty()) continue
                val sel = etiquetaSel[tipo].orEmpty()
                dimensiones.add(
                    FiltroCompacto.Dimension(nombreTipo[tipo].orEmpty(), sel.map { nombreValor[it].orEmpty() }) {
                        val base = catalogo.filter { cumpleFiltro(it, categoriasSel, etiquetaSel.filterKeys { k -> k != tipo }) }
                        val opciones = valores.sortedBy { nombreValor[it].orEmpty().lowercase() }.map { v ->
                            val n = base.count { p -> p.etiquetas.orEmpty().any { it.tipoEtiquetaId == tipo && it.valorEtiquetaId == v } }
                            OpcionBuscable(v.toString(), nombreValor[v].orEmpty(), "$n ${if (n == 1) "prenda" else "prendas"}")
                        }
                        FiltroCompacto.abrirDimension(ctx, nombreTipo[tipo].orEmpty(), opciones, sel.map { it.toString() }.toSet()) { ids ->
                            val nuevos = ids.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }.toMutableSet()
                            if (nuevos.isEmpty()) etiquetaSel.remove(tipo) else etiquetaSel[tipo] = nuevos
                            render()
                        }
                    },
                )
            }
            FiltroCompacto.pintarBarra(fragment.layoutInflater, sheetBinding.contenedorFiltros, dimensiones)
        }

        adapter = PrendaCatalogoGrillaAdapter { prenda ->
            val id = prenda.id
            if (!multiple) {
                onElegir(prenda)
                hoja.dismiss()
            } else if (id != null) {
                if (!elegidas.add(id)) elegidas.remove(id)
                render()
            }
        }
        sheetBinding.listaOpciones.adapter = adapter

        sheetBinding.botonListo.setOnClickListener {
            onConfirmar(catalogo.filter { it.id in elegidas })
            hoja.dismiss()
        }
        sheetBinding.botonLimpiarFiltros.setOnClickListener {
            categoriasSel.clear(); etiquetaSel.clear(); render()
        }
        sheetBinding.editBuscar.alBuscar { t -> texto = t; render() }
        render()
        hoja.show()
    }

    private fun cumpleFiltro(
        p: PrendaDeCatalogoResponse,
        categorias: Set<UUID>,
        etiquetas: Map<UUID, Set<UUID>>,
    ): Boolean {
        if (categorias.isNotEmpty() && p.categoriaId !in categorias) return false
        for ((tipo, valores) in etiquetas) {
            val valor = p.etiquetas.orEmpty().firstOrNull { it.tipoEtiquetaId == tipo }?.valorEtiquetaId
            if (valor == null || valor !in valores) return false
        }
        return true
    }
}
