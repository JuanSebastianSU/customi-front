package com.costumi.app.ui.gestion.clientes

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentClienteHistorialBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Historial de compras/rentas de un cliente (solo lectura). */
@AndroidEntryPoint
class ClienteHistorialFragment : Fragment(R.layout.fragment_cliente_historial) {

    private val vm: ClienteHistorialViewModel by viewModels()
    private var _binding: FragmentClienteHistorialBinding? = null
    private val binding get() = _binding!!
    private val adapter = HistorialAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClienteHistorialBinding.bind(view)
        binding.toolbar.title = vm.clienteNombre
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Este cliente no tiene operaciones todavia.") {
                adapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "clienteId"
        const val ARG_NOMBRE = "clienteNombre"

        fun args(id: UUID, nombre: String?) = bundleOf(
            ARG_ID to id.toString(),
            ARG_NOMBRE to nombre,
        )
    }
}
