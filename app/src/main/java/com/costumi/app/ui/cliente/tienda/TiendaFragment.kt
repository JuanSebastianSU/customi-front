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
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
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

    private var ultimoDisfraces: UiState<List<DisfrazResponse>> = UiState.Loading
    private var ultimoPrendas: UiState<List<PrendaVitrinaResponse>> = UiState.Loading

    // Cada pestaña recuerda su propia categoría: son taxonomías distintas (disfraz vs. prenda).
    private var categoriaDisfraz: String? = null
    private var categoriaPrenda: String? = null

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
    }

    private fun enDisfraces(): Boolean = binding.tabs.selectedTabPosition != 1

    private fun render() {
        val b = _binding ?: return
        if (enDisfraces()) {
            if (b.lista.adapter !== disfrazAdapter) b.lista.adapter = disfrazAdapter
            b.stateView.mostrar(ultimoDisfraces, vacio = "Esta tienda aun no tiene disfraces.") { disfraces ->
                pintarFiltroCategoria(
                    categorias = disfraces.mapNotNull { it.categoria?.takeIf(String::isNotBlank) },
                    seleccionada = categoriaDisfraz,
                    alElegir = { categoriaDisfraz = it },
                )
                disfrazAdapter.submitList(filtrarDisfraces(disfraces))
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
                prendaAdapter.submitList(filtrar(prendas))
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
        b.barraCategoria.isVisible = distintas.size >= 2
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

    private fun filtrar(prendas: List<PrendaVitrinaResponse>): List<PrendaVitrinaResponse> {
        val cat = categoriaPrenda ?: return prendas
        return prendas.filter { it.categoria == cat }
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
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TODAS = "__todas__"
    }
}
