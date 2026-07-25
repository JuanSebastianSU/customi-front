package com.costumi.app.ui.cliente.carrito

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.data.remote.CarritoAbiertoDto
import com.costumi.app.databinding.FragmentMisCarritosBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/**
 * Los carritos que el cliente dejo abiertos, en cualquier tienda.
 *
 * Es la puerta que faltaba: el carrito solo se alcanzaba justo despues de agregar un articulo, asi que
 * al cambiar de tienda habia que agregar algo otra vez para reencontrarlo.
 */
@AndroidEntryPoint
class MisCarritosFragment : Fragment(R.layout.fragment_mis_carritos) {

    private val vm: MisCarritosViewModel by viewModels()
    private var _binding: FragmentMisCarritosBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy { CarritoAbiertoAdapter { abrir(it) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMisCarritosBinding.bind(view)
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            // Al quedar vacio hay que limpiar la lista: si no, las tarjetas viejas siguen pintadas
            // debajo del estado vacio (mismo cuidado que en el carrito).
            if (estado !is com.costumi.app.core.UiState.Success<*>) adapter.submitList(emptyList())
            binding.stateView.mostrar(
                estado,
                vacio = "No tenes carritos abiertos.\nAgrega algo desde una tienda.",
            ) { carritos ->
                adapter.submitList(carritos)
            }
        }
    }

    /** Al volver de comprar o de la tienda, los carritos pudieron cambiar. */
    override fun onResume() {
        super.onResume()
        vm.cargar()
    }

    private fun abrir(c: CarritoAbiertoDto) {
        val empresa = c.empresaId?.toString() ?: return
        val sucursal = c.sucursalId?.toString() ?: return
        findNavController().navigate(
            R.id.carritoFragment,
            bundleOf(
                CarritoFragment.ARG_EMPRESA_ID to empresa,
                CarritoFragment.ARG_SUCURSAL_ID to sucursal,
                CarritoFragment.ARG_TIPO to (c.tipo ?: "VENTA"),
            ),
        )
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
