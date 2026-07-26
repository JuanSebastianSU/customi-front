package com.costumi.app.ui.cliente.tienda

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.databinding.FragmentTiendaBinding
import com.costumi.app.ui.cliente.detalle.DetalleDisfrazFragment
import com.costumi.app.ui.cliente.detalle.DetallePrendaFragment
import com.costumi.app.ui.cliente.explorar.ExplorarFragment
import com.costumi.app.ui.avisoDatosGuardados
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

/** Catalogo de una tienda con dos apartados: DISFRACES y PRENDAS. Toca un item para ver su detalle. */
@AndroidEntryPoint
class TiendaFragment : Fragment(R.layout.fragment_tienda) {

    private val vm: TiendaViewModel by viewModels()
    private var _binding: FragmentTiendaBinding? = null
    private val binding get() = _binding!!

    private val disfrazAdapter = DisfrazVitrinaAdapter { disfraz ->
        val empresaId = requireArguments().getString(ExplorarFragment.ARG_EMPRESA_ID)
        findNavController().navigate(
            R.id.detalleDisfrazFragment,
            bundleOf(
                DetalleDisfrazFragment.ARG_EMPRESA_ID to empresaId,
                DetalleDisfrazFragment.ARG_DISFRAZ_ID to disfraz.id?.toString(),
                DetalleDisfrazFragment.ARG_NOMBRE to (disfraz.nombre ?: "Disfraz"),
            ),
        )
    }

    private val prendaAdapter = PrendaAdapter { prenda -> navegarADetallePrenda(prenda) }
    private var avisoRed: Snackbar? = null

    private var ultimoDisfraces: UiState<List<DisfrazResponse>> = UiState.Loading
    private var ultimoPrendas: UiState<List<PrendaVitrinaResponse>> = UiState.Loading

    // Cada pestaña recuerda su propia categoría: son taxonomías distintas (disfraz vs. prenda).
    private var categoriaDisfraz: String? = null
    private var categoriaPrenda: String? = null

    /** Filtro por etiqueta de la pestaña Prendas: tipo (nombre) → valores (nombres) elegidos. */
    private val etiquetaFiltro = LinkedHashMap<String, MutableSet<String>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTiendaBinding.bind(view)
        binding.toolbar.title = vm.nombreTienda
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        observar(vm.descripcion) { binding.toolbar.subtitle = it }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                vm.pestanaActiva = binding.tabs.selectedTabPosition
                render()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        // Al volver del detalle la vista se recrea y el TabLayout arrancaba en Disfraces aunque
        // estuvieras en Prendas: se restaura la pestaña que quedó activa.
        binding.tabs.getTabAt(vm.pestanaActiva)?.takeIf { !it.isSelected }?.select()

        observar(vm.disfraces) { ultimoDisfraces = it; if (enDisfraces()) render() }
        observar(vm.prendas) { ultimoPrendas = it; if (!enDisfraces()) render() }
        observar(vm.sinConexion) { off ->
            avisoRed = avisoDatosGuardados(avisoRed, off) { vm.cargarPrendas(); vm.cargarDisfraces() }
        }
    }

    private fun enDisfraces(): Boolean = binding.tabs.selectedTabPosition != 1

    private fun render() {
        val b = _binding ?: return
        if (enDisfraces()) {
            if (b.lista.adapter !== disfrazAdapter) b.lista.adapter = disfrazAdapter
            b.chipEtiquetas.isVisible = false // el filtro por etiqueta es solo de Prendas
            b.stateView.mostrar(ultimoDisfraces, vacio = "Esta tienda aun no tiene disfraces.") { disfraces ->
                pintarFiltroCategoria(
                    categorias = disfraces.mapNotNull { it.categoria?.takeIf(String::isNotBlank) },
                    seleccionada = categoriaDisfraz,
                    alElegir = { categoriaDisfraz = it },
                )
                disfrazAdapter.submitList(filtrarDisfraces(disfraces))
                b.barraCategoria.isVisible = b.chipCategoria.isVisible
            }
            if (ultimoDisfraces !is UiState.Success) b.barraCategoria.isVisible = false
        } else {
            if (b.lista.adapter !== prendaAdapter) b.lista.adapter = prendaAdapter
            b.stateView.mostrar(ultimoPrendas, vacio = "Esta tienda aun no tiene prendas.") { prendas ->
                pintarFiltroCategoria(
                    categorias = prendas.mapNotNull { it.categoria?.takeIf(String::isNotBlank) },
                    seleccionada = categoriaPrenda,
                    alElegir = { categoriaPrenda = it },
                )
                pintarFiltroEtiquetas(prendas)
                prendaAdapter.submitList(filtrar(prendas))
                b.barraCategoria.isVisible = b.chipCategoria.isVisible || b.chipEtiquetas.isVisible
            }
            if (ultimoPrendas !is UiState.Success) b.barraCategoria.isVisible = false
        }
    }

    /**
     * Un solo control para la categoría: dice cuál está activa y abre una lista **con buscador**. Una fila
     * de chips se vuelve inusable en cuanto la tienda tiene muchas categorías (hay que desplazarla a
     * ciegas); esto ocupa lo mismo con 3 que con 300. Sirve para las dos pestañas: las categorías salen
     * del propio catálogo que se está mostrando.
     */
    private fun pintarFiltroCategoria(
        categorias: List<String>,
        seleccionada: String?,
        alElegir: (String?) -> Unit,
    ) {
        val b = _binding ?: return
        val distintas = categorias.distinct().sorted()
        // Con una sola categoría el filtro no aporta nada.
        b.chipCategoria.isVisible = distintas.size >= 2
        if (distintas.size < 2) {
            alElegir(null)
            return
        }
        // Si la categoría elegida ya no esta en el catalogo, se vuelve a "Todas".
        val actual = seleccionada?.takeIf { it in distintas }
        if (actual != seleccionada) alElegir(actual)
        b.chipCategoria.text = "Categoria: ${actual ?: "Todas"}"
        b.chipCategoria.setOnClickListener { abrirSelectorDeCategoria(distintas, actual, alElegir) }
    }

    /**
     * Filtro por etiqueta de las PRENDAS (color/talla/estilo…), construido con las etiquetas que trae el
     * catálogo. Dos niveles como en el inventario: primero los tipos, luego sus valores. El chip solo
     * aparece si hay etiquetas y muestra cuántos valores hay elegidos.
     */
    private fun pintarFiltroEtiquetas(prendas: List<PrendaVitrinaResponse>) {
        val b = _binding ?: return
        // Disponibles: tipo (nombre) -> valores (nombres), tal como vienen de la vitrina.
        val disponibles = LinkedHashMap<String, MutableSet<String>>()
        prendas.forEach { p ->
            p.etiquetas.orEmpty().forEach { e ->
                val tipo = e.tipo?.takeIf { it.isNotBlank() } ?: return@forEach
                val valor = e.valor?.takeIf { it.isNotBlank() } ?: return@forEach
                disponibles.getOrPut(tipo) { sortedSetOf() }.add(valor)
            }
        }
        // Se descartan del filtro activo los valores que ya no existan en el catálogo actual.
        etiquetaFiltro.keys.retainAll(disponibles.keys)
        etiquetaFiltro.forEach { (tipo, sel) -> sel.retainAll(disponibles[tipo].orEmpty()) }
        etiquetaFiltro.entries.removeAll { it.value.isEmpty() }

        b.chipEtiquetas.isVisible = disponibles.isNotEmpty()
        if (disponibles.isEmpty()) return
        val cuenta = etiquetaFiltro.values.sumOf { it.size }
        b.chipEtiquetas.text = if (cuenta > 0) "Filtros ($cuenta)" else "Filtros"
        b.chipEtiquetas.setOnClickListener { abrirFiltroEtiquetas(disponibles) }
    }

    /** Diálogo de dos niveles (tipo → valores), igual criterio que el filtro del inventario. */
    private fun abrirFiltroEtiquetas(disponibles: Map<String, Set<String>>) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val margen = (24 * dp).toInt()
        // Copia editable; se confirma con "Aplicar".
        val seleccion = LinkedHashMap<String, MutableSet<String>>().apply {
            etiquetaFiltro.forEach { (k, v) -> put(k, v.toMutableSet()) }
        }
        val contenedor = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(margen, (8 * dp).toInt(), margen, 0)
        }
        disponibles.forEach { (tipo, valores) ->
            val fila = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
                val fondo = android.util.TypedValue()
                ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, fondo, true)
                setBackgroundResource(fondo.resourceId)
            }
            val textos = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            textos.addView(android.widget.TextView(ctx).apply {
                text = tipo
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            })
            val sub = android.widget.TextView(ctx).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
            }
            fun refrescar() {
                val n = seleccion[tipo]?.size ?: 0
                sub.text = if (n == 0) "Todos" else if (n == 1) "1 elegido" else "$n elegidos"
            }
            refrescar()
            textos.addView(sub)
            fila.addView(textos)
            fila.addView(android.widget.ImageView(ctx).apply {
                setImageResource(R.drawable.ic_chevron_right)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant),
                )
            })
            fila.setOnClickListener {
                val lista = valores.toList()
                val marcadas = lista.map { seleccion[tipo]?.contains(it) == true }.toBooleanArray()
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(tipo)
                    .setMultiChoiceItems(lista.toTypedArray(), marcadas) { _, which, ok -> marcadas[which] = ok }
                    .setPositiveButton("Listo") { _, _ ->
                        val nueva = lista.filterIndexed { i, _ -> marcadas[i] }.toMutableSet()
                        if (nueva.isEmpty()) seleccion.remove(tipo) else seleccion[tipo] = nueva
                        refrescar()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            contenedor.addView(fila)
        }
        val scroll = android.widget.ScrollView(ctx).apply { addView(contenedor) }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Filtrar por etiqueta")
            .setView(scroll)
            .setPositiveButton("Aplicar") { _, _ ->
                etiquetaFiltro.clear()
                seleccion.forEach { (k, v) -> if (v.isNotEmpty()) etiquetaFiltro[k] = v }
                render()
            }
            .setNeutralButton("Limpiar") { _, _ -> etiquetaFiltro.clear(); render() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirSelectorDeCategoria(
        categorias: List<String>,
        seleccionada: String?,
        alElegir: (String?) -> Unit,
    ) {
        val opciones = listOf(OpcionBuscable(TODAS, "Todas")) + categorias.map { OpcionBuscable(it, it) }
        ListaBuscable.unaOpcion(
            requireContext(),
            "Elegir categoria",
            opciones,
            seleccionada ?: TODAS,
        ) { id ->
            alElegir(id?.takeIf { it != TODAS })
            render()
        }
    }

    private fun filtrarDisfraces(disfraces: List<DisfrazResponse>): List<DisfrazResponse> {
        val cat = categoriaDisfraz ?: return disfraces
        return disfraces.filter { it.categoria == cat }
    }

    private fun filtrar(prendas: List<PrendaVitrinaResponse>): List<PrendaVitrinaResponse> =
        prendas.filter { p ->
            (categoriaPrenda == null || p.categoria == categoriaPrenda) && cumpleEtiquetas(p)
        }

    /** Pasa el filtro de etiquetas si, para CADA tipo elegido, tiene alguno de sus valores (AND entre tipos). */
    private fun cumpleEtiquetas(p: PrendaVitrinaResponse): Boolean {
        if (etiquetaFiltro.isEmpty()) return true
        val etqs = p.etiquetas.orEmpty()
        return etiquetaFiltro.all { (tipo, valores) ->
            etqs.any { it.tipo == tipo && it.valor in valores }
        }
    }

    private fun navegarADetallePrenda(prenda: PrendaVitrinaResponse) {
        findNavController().navigate(
            R.id.detallePrendaFragment,
            bundleOf(
                DetallePrendaFragment.ARG_EMPRESA_ID to requireArguments().getString(ExplorarFragment.ARG_EMPRESA_ID),
                DetallePrendaFragment.ARG_PRENDA_ID to prenda.id?.toString(),
                DetallePrendaFragment.ARG_NOMBRE to (prenda.nombre ?: "Articulo"),
                DetallePrendaFragment.ARG_CATEGORIA to prenda.categoria,
                DetallePrendaFragment.ARG_ETIQUETAS to prenda.etiquetas.orEmpty()
                    .filter { !it.tipo.isNullOrBlank() && !it.valor.isNullOrBlank() }
                    .joinToString("  ·  ") { "${it.tipo}: ${it.valor}" }
                    .ifBlank { null },
                DetallePrendaFragment.ARG_PRECIO_RENTA to prenda.precioRenta?.toPlainString(),
                DetallePrendaFragment.ARG_PRECIO_VENTA to prenda.precioVenta?.toPlainString(),
                DetallePrendaFragment.ARG_FOTO_URL to prenda.fotoUrl,
            ),
        )
    }

    override fun onDestroyView() {
        avisoRed?.dismiss()
        avisoRed = null
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TODAS = "__todas__"
    }
}
