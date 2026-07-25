package com.costumi.app.ui.gestion.inventario

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.databinding.FragmentInventarioBinding
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.PrendaResponse
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/**
 * Inventario — dos vistas sobre el mismo listado:
 * 1) Gestión (sin filtros): lista paginada de prendas con acciones (editar/stock/archivar).
 * 2) Explorar por CATEGORÍA + filtros por etiqueta: al elegir una categoría o filtros, muestra los stocks
 *    con su STOCK disponible y sus valores de etiqueta (color/talla).
 */
@AndroidEntryPoint
class InventarioFragment : Fragment(R.layout.fragment_inventario) {

    private val vm: InventarioViewModel by viewModels()
    private var _binding: FragmentInventarioBinding? = null
    private val binding get() = _binding!!

    private val adapter = PrendaAdapter(
        alEditar = { abrirFormularioEdicion(it) },
        alVerStock = { abrirGruposStock(it.id, it.nombre) },
        alAlternarArchivado = { vm.alternarArchivado(it) },
    )
    private val catalogoAdapter = CatalogoStockAdapter(
        alTocar = { abrirGruposStock(it.id, it.nombre) },
    )
    private val pagingConFooter: RecyclerView.Adapter<*> by lazy {
        adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })
    }

    private var refreshActual: LoadState = LoadState.Loading

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInventarioBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar prenda por nombre"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }

        binding.lista.adapter = pagingConFooter

        setFragmentResultListener(PrendaFormFragment.RESULT_GUARDADA) { _, _ ->
            adapter.refresh()
            vm.cargarStockBajo()
            if (vm.filtroActivo.value) vm.recargarCatalogo()
        }

        binding.toolbar.inflateMenu(R.menu.menu_inventario)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.accionCategorias -> { findNavController().navigate(R.id.categoriasFragment); true }
                R.id.accionTiposEtiqueta -> { findNavController().navigate(R.id.tiposEtiquetaFragment); true }
                R.id.accionDisfraces -> { findNavController().navigate(R.id.disfracesFragment); true }
                else -> false
            }
        }

        binding.fabNueva.setOnClickListener { findNavController().navigate(R.id.prendaFormFragment) }
        binding.chipFiltros.setOnClickListener { mostrarDialogoFiltros() }

        observar(vm.prendas) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }

        observar(adapter.loadStateFlow) { estados ->
            refreshActual = estados.refresh
            if (!vm.filtroActivo.value) pintarEstadoGestion(estados.refresh)
        }

        observar(vm.categorias) { pintarFiltroCategoria(it) }
        observar(vm.etiquetasSel) { seleccion ->
            val cuenta = seleccion.values.sumOf { it.size }
            binding.chipFiltros.text = if (cuenta > 0) "Filtros ($cuenta)" else "Filtros"
        }
        observar(vm.filtroActivo) { aplicarModo(it) }
        observar(vm.catalogo) { estado -> if (vm.filtroActivo.value) pintarCatalogo(estado) }

        observar(vm.prendasConStockBajo) { adapter.prendasConStockBajo = it }

        observar(vm.stockBajo) { cuenta ->
            binding.bannerStockBajo.isVisible = cuenta > 0
            binding.textoStockBajo.text = if (cuenta == 1) "1 variante con stock bajo" else "$cuenta variantes con stock bajo"
        }

        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoInventario.Info -> {
                    mostrarMensaje(evento.mensaje)
                    adapter.refresh()
                    vm.cargarStockBajo()
                }
                is EventoInventario.Error -> mostrarMensaje(evento.mensaje)
            }
        }

        // Al (re)crear la vista —incluye VOLVER de la pantalla de Grupos de stock— refresca el aviso de
        // stock bajo y, si hay un filtro activo, el catálogo (para que el stock recién creado se vea).
        vm.cargarStockBajo()
        if (vm.filtroActivo.value) vm.recargarCatalogo()
    }

    /** Alterna entre la lista de gestión (paginada) y el catálogo filtrado (con stock). */
    private fun aplicarModo(filtrando: Boolean) {
        if (filtrando) {
            if (binding.lista.adapter !== catalogoAdapter) binding.lista.adapter = catalogoAdapter
            pintarCatalogo(vm.catalogo.value)
        } else {
            if (binding.lista.adapter !== pagingConFooter) binding.lista.adapter = pagingConFooter
            pintarEstadoGestion(refreshActual)
        }
    }

    private fun pintarCatalogo(estado: UiState<List<PrendaDeCatalogoResponse>>) {
        catalogoAdapter.nombresDeValores = vm.nombresDeValores()
        binding.stateView.mostrar(estado, vacio = "No hay prendas con esos filtros.") { lista ->
            catalogoAdapter.submitList(lista)
        }
    }

    /**
     * Un solo control para la categoría: dice cuál está activa y abre una lista **con buscador**. Una fila
     * de chips se vuelve inusable en cuanto hay muchas categorías (hay que desplazarla a ciegas); esto
     * ocupa lo mismo con 3 que con 300.
     */
    private fun pintarFiltroCategoria(categorias: List<CategoriaResponse>) {
        val elegida = categorias.firstOrNull { it.id == vm.categoriaSel.value }
        binding.chipCategoria.text = "Categoria: ${elegida?.nombre ?: "Todas"}"
        binding.chipCategoria.setOnClickListener { abrirSelectorDeCategoria(categorias) }
    }

    private fun abrirSelectorDeCategoria(categorias: List<CategoriaResponse>) {
        val opciones = listOf(OpcionBuscable(TODAS, "Todas")) +
            categorias.mapNotNull { c -> c.id?.let { OpcionBuscable(it.toString(), c.nombre.orEmpty()) } }
        ListaBuscable.unaOpcion(
            requireContext(),
            "Elegir categoria",
            opciones,
            vm.categoriaSel.value?.toString() ?: TODAS,
        ) { id ->
            vm.seleccionarCategoria(if (id == null || id == TODAS) null else UUID.fromString(id))
            pintarFiltroCategoria(categorias)
        }
    }

    private fun mostrarDialogoFiltros() {
        val tipos = vm.tipos.value
        if (tipos.isEmpty()) {
            mostrarMensaje("No hay tipos de etiqueta para filtrar.")
            return
        }
        val ctx = requireContext()
        val margen = (16 * resources.displayMetrics.density).toInt()
        val contenedor = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margen + margen / 2, margen / 2, margen + margen / 2, 0)
        }
        val grupos = LinkedHashMap<UUID, ChipGroup>()
        val seleccionActual = vm.etiquetasSel.value
        tipos.forEach { tv ->
            val tipoId = tv.tipo.id ?: return@forEach
            contenedor.addView(TextView(ctx).apply {
                text = tv.tipo.nombre.orEmpty()
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
                setPadding(0, margen / 2, 0, 0)
            })
            val cg = ChipGroup(ctx).apply { isSingleSelection = false }
            tv.valores.forEach { valor ->
                val vid = valor.id ?: return@forEach
                val chip = layoutInflater.inflate(R.layout.chip_filtro, cg, false) as Chip
                chip.text = valor.valor.orEmpty()
                chip.tag = vid
                chip.isChecked = seleccionActual[tipoId]?.contains(vid) == true
                cg.addView(chip)
            }
            grupos[tipoId] = cg
            contenedor.addView(cg)
        }
        val scroll = ScrollView(ctx).apply { addView(contenedor) }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Filtrar por etiqueta")
            .setView(scroll)
            .setPositiveButton("Aplicar") { _, _ ->
                val seleccion = grupos.mapValues { (_, cg) ->
                    (0 until cg.childCount).mapNotNull { i ->
                        (cg.getChildAt(i) as Chip).takeIf { it.isChecked }?.tag as? UUID
                    }.toSet()
                }
                vm.aplicarEtiquetas(seleccion)
            }
            .setNeutralButton("Limpiar") { _, _ -> vm.aplicarEtiquetas(emptyMap()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirFormularioEdicion(prenda: PrendaResponse) {
        val id = prenda.id ?: return
        findNavController().navigate(
            R.id.prendaFormFragment,
            PrendaFormFragment.argsEditar(
                id = id,
                nombre = prenda.nombre,
                categoriaId = prenda.categoriaId,
                tipo = prenda.tipoArticulo?.value,
                precioVenta = prenda.precioVenta,
                precioRenta = prenda.precioRenta,
                costo = prenda.costoAdquisicion,
                deposito = prenda.depositoSugerido,
                valorReposicion = prenda.valorReposicion,
                valorDano = prenda.valorDano,
                fotoUrl = prenda.fotoUrl,
                etiquetas = prenda.etiquetas.orEmpty(),
            ),
        )
    }

    private fun abrirGruposStock(id: UUID?, nombre: String?) {
        id ?: return
        findNavController().navigate(R.id.gruposStockFragment, GruposStockFragment.args(id, nombre))
    }

    private fun pintarEstadoGestion(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudo cargar el inventario.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) {
                    binding.stateView.vacio("Todavia no hay prendas. Crea la primera con el boton +.")
                } else {
                    binding.stateView.ocultar()
                }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** Cuántas categorías se muestran como chip antes de mandar el resto a "Ver todas". */
        const val TODAS = "__todas__"
    }
}
