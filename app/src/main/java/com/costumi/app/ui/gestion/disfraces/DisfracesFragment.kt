package com.costumi.app.ui.gestion.disfraces

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentDisfracesBinding
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Disfraces — lista con disponibilidad y archivar/activar; alta desde el FAB. */
@AndroidEntryPoint
class DisfracesFragment : Fragment(R.layout.fragment_disfraces) {

    private val vm: DisfracesViewModel by viewModels()
    private var _binding: FragmentDisfracesBinding? = null
    private val binding get() = _binding!!

    private val adapter = DisfrazAdapter(
        alVerDisponibilidad = { vm.consultarDisponibilidad(it) },
        alEditar = { abrirEdicion(it) },
        alAsignar = { abrirAsignar(it) },
        alAlternarArchivado = { vm.alternarArchivado(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDisfracesBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar disfraz por nombre"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.inflateMenu(R.menu.menu_disfraces)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.accionCategoriasDisfraz -> {
                    findNavController().navigate(R.id.categoriasDisfrazFragment); true
                }
                else -> false
            }
        }
        // Los disfraces se rentan/venden (mezclados con prendas) desde "Nueva renta" y "Nueva venta".
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { findNavController().navigate(R.id.disfrazFormFragment) }

        setFragmentResultListener(DisfrazFormFragment.RESULT_GUARDADO) { _, _ -> vm.cargar() }

        observar(vm.categorias) { pintarFiltroCategoria(it) }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay disfraces en esta vista. Arma uno con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoDisfraz.Info -> mostrarMensaje(evento.mensaje)
                is EventoDisfraz.Error -> mostrarMensaje(evento.mensaje)
                is EventoDisfraz.Disponibilidad -> mostrarMensaje(
                    if (evento.disponible) "\"${evento.nombre}\": disponible para armar."
                    else "\"${evento.nombre}\": sin stock para armarlo completo.",
                )
            }
        }

        // Al (re)crear la vista —incluye VOLVER de "Categorias de disfraz"— recarga para reflejar
        // categorías nuevas/renombradas/archivadas en los chips.
        vm.cargar()
    }

    /**
     * Un solo control para la categoría: dice cuál está activa y abre una lista **con buscador**. Una fila
     * de chips se vuelve inusable en cuanto hay muchas categorías (hay que desplazarla a ciegas); esto
     * ocupa lo mismo con 3 que con 300.
     */
    private fun pintarFiltroCategoria(categorias: List<CategoriaDeDisfrazResponse>) {
        val elegida = categorias.firstOrNull { it.id == vm.categoriaSeleccionada }
        binding.chipCategoria.text = "Categoria: ${elegida?.nombre ?: "Todas"}"
        binding.chipCategoria.setOnClickListener { abrirSelectorDeCategoria(categorias) }
    }

    private fun abrirSelectorDeCategoria(categorias: List<CategoriaDeDisfrazResponse>) {
        val opciones = listOf(OpcionBuscable(TODAS, "Todas")) +
            categorias.mapNotNull { c -> c.id?.let { OpcionBuscable(it.toString(), c.nombre.orEmpty()) } }
        ListaBuscable.unaOpcion(
            requireContext(),
            "Elegir categoria de disfraz",
            opciones,
            vm.categoriaSeleccionada?.toString() ?: TODAS,
        ) { id ->
            vm.seleccionarCategoria(if (id == null || id == TODAS) null else UUID.fromString(id))
            pintarFiltroCategoria(categorias)
        }
    }

    private fun abrirEdicion(disfraz: com.costumi.apiclient.models.DisfrazResponse) {
        val id = disfraz.id ?: return
        findNavController().navigate(R.id.disfrazFormFragment, DisfrazFormFragment.args(id))
    }

    private fun abrirAsignar(disfraz: com.costumi.apiclient.models.DisfrazResponse) {
        val id = disfraz.id ?: return
        findNavController().navigate(
            R.id.disfrazAsignarFragment,
            DisfrazAsignarFragment.args(id, disfraz.nombre ?: "Disfraz"),
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
