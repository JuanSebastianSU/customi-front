package com.costumi.app.ui.gestion.reembolsos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.costumi.app.R
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.app.databinding.DialogSolicitarReembolsoBinding
import com.costumi.app.databinding.FragmentSolicitarReembolsoBinding
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.gestion.pagos.OperacionPagoAdapter
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Registra una solicitud de reembolso: elige la operación (venta/renta) y el monto/motivo. */
@AndroidEntryPoint
class SolicitarReembolsoFragment : Fragment(R.layout.fragment_solicitar_reembolso) {

    private val vm: SolicitarReembolsoViewModel by viewModels()
    private var _binding: FragmentSolicitarReembolsoBinding? = null
    private val binding get() = _binding!!
    private val adapter = OperacionPagoAdapter { dialogoSolicitar(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSolicitarReembolsoBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })

        binding.toggleTipo.check(R.id.botonVentas)
        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                vm.cambiarTipo(if (checkedId == R.id.botonRentas) TipoConcepto.RENTA else TipoConcepto.VENTA)
            }
        }

        observar(vm.operaciones) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoReembolso.Info -> {
                    setFragmentResult(RESULT_SOLICITADA, Bundle.EMPTY)
                    mostrarMensaje(evento.mensaje)
                    findNavController().popBackStack()
                }
                is EventoReembolso.Error -> mostrarMensaje(evento.mensaje)
                is EventoReembolso.Detalle -> Unit // no aplica al alta de solicitud
            }
        }
    }

    private fun dialogoSolicitar(op: OperacionPago) {
        val d = DialogSolicitarReembolsoBinding.inflate(layoutInflater)
        op.total?.let { d.editMonto.setText(it.toPlainString()) }
        val tipo = if (op.tipo == TipoConcepto.RENTA) "Renta" else "Venta"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reembolso de $tipo")
            .setView(d.root)
            .setPositiveButton("Registrar") { _, _ ->
                val monto = d.editMonto.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
                if (monto == null || monto.signum() <= 0) { mostrarMensaje("Monto invalido"); return@setPositiveButton }
                // El backend exige motivo (NotBlank).
                val motivo = d.editMotivo.text?.toString()?.trim().orEmpty()
                if (motivo.isBlank()) { mostrarMensaje("El motivo es obligatorio"); return@setPositiveButton }
                vm.solicitar(op, monto, motivo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar las operaciones.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) binding.stateView.vacio("No hay operaciones de este tipo.")
                else binding.stateView.ocultar()
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_SOLICITADA = "reembolso_solicitado"
    }
}
