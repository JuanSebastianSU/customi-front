package com.costumi.app.ui.gestion.pagos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.app.databinding.FragmentPagosBinding
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** Selector de operaciones (venta/renta) para gestionar sus cobros. */
@AndroidEntryPoint
class PagosFragment : Fragment(R.layout.fragment_pagos) {

    private val vm: PagosViewModel by viewModels()
    private var _binding: FragmentPagosBinding? = null
    private val binding get() = _binding!!
    private val adapter = OperacionPagoAdapter { abrirConcepto(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPagosBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por codigo de retiro"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })

        binding.toggleTipo.check(R.id.botonVentas)
        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                vm.cambiarTipo(if (checkedId == R.id.botonRentas) TipoConcepto.RENTA else TipoConcepto.VENTA)
            }
        }

        setFragmentResultListener(PagoConceptoFragment.RESULT_COBRADO) { _, _ -> adapter.refresh() }

        observar(vm.operaciones) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }
    }

    private fun abrirConcepto(op: OperacionPago) {
        val tipo = if (op.tipo == TipoConcepto.VENTA) "Venta" else "Renta"
        findNavController().navigate(
            R.id.pagoConceptoFragment,
            PagoConceptoFragment.args(
                tipo = op.tipo.name,
                conceptoId = op.id,
                sucursalId = op.sucursalId,
                total = op.total,
                titulo = "$tipo · ${op.total.comoPrecio() ?: "-"}",
            ),
        )
    }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar las operaciones.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) {
                    binding.stateView.vacio("No hay operaciones de este tipo.")
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
}
