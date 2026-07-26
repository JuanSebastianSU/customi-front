package com.costumi.app.ui.cliente.explorar

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.core.view.isVisible
import com.costumi.app.R
import com.costumi.app.databinding.FragmentExplorarBinding
import com.costumi.app.ui.cliente.detalle.DetalleDisfrazFragment
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** Home del modo CLIENTE: descubrir tiendas del marketplace. Toca una tienda para ver su catalogo. */
@AndroidEntryPoint
class ExplorarFragment : Fragment(R.layout.fragment_explorar) {

    private val vm: ExplorarViewModel by viewModels()
    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!

    private val adapter = EmpresaAdapter { empresa ->
        findNavController().navigate(
            R.id.tiendaFragment,
            bundleOf(ARG_EMPRESA_ID to empresa.id, ARG_NOMBRE to empresa.nombre),
        )
    }

    private val destacadoAdapter = DestacadoAdapter { d ->
        findNavController().navigate(
            R.id.detalleDisfrazFragment,
            bundleOf(
                DetalleDisfrazFragment.ARG_EMPRESA_ID to d.empresaId?.toString(),
                DetalleDisfrazFragment.ARG_DISFRAZ_ID to d.disfrazId?.toString(),
                DetalleDisfrazFragment.ARG_NOMBRE to (d.nombre ?: "Disfraz"),
            ),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentExplorarBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar tienda por nombre"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.lista.adapter = adapter
        binding.listaDestacados.adapter = destacadoAdapter

        observar(vm.destacados) { lista ->
            val hay = lista.isNotEmpty()
            binding.tituloDestacados.isVisible = hay
            binding.listaDestacados.isVisible = hay
            destacadoAdapter.submitList(lista)
        }

        binding.swipe.setOnRefreshListener { vm.refrescar() }

        observar(vm.estado) { estado ->
            binding.swipe.isRefreshing = false
            binding.stateView.mostrar(estado, vacio = "Aun no hay tiendas disponibles.") { empresas ->
                adapter.submitList(empresas)
            }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        binding.listaDestacados.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_NOMBRE = "nombre"
    }
}
