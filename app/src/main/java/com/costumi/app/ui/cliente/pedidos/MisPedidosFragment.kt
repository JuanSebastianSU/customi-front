package com.costumi.app.ui.cliente.pedidos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.DialogMotivoBinding
import com.costumi.app.databinding.FragmentMisPedidosBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.HistorialItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** "Mis Pedidos": historial de compras/rentas del cliente en todas las tiendas del marketplace. */
@AndroidEntryPoint
class MisPedidosFragment : Fragment(R.layout.fragment_mis_pedidos) {

    private val vm: MisPedidosViewModel by viewModels()
    private var _binding: FragmentMisPedidosBinding? = null
    private val binding get() = _binding!!
    private val adapter = PedidoAdapter { pedido -> mostrarDialogoReembolso(pedido) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMisPedidosBinding.bind(view)
        binding.lista.adapter = adapter
        pintarChipsFiltro()

        observar(vm.estado) { estado ->
            // Si el filtro deja la lista vacia (o hay error/carga), hay que LIMPIAR las filas previas:
            // el StateView es transparente y, sin esto, se veian las de antes debajo del "Todavia no
            // tienes pedidos" (el filtro "Activos" mostrando "Por retirar" por detras).
            if (estado !is UiState.Success) adapter.submitList(emptyList())
            binding.stateView.mostrar(estado, vacio = "Todavia no tienes pedidos.") { pedidos ->
                adapter.submitList(pedidos)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPedido.Info -> mostrarMensaje(evento.mensaje)
                is EventoPedido.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Chips fijos de filtro por estado (Todos / Por retirar / Activos / Cerrados). */
    private fun pintarChipsFiltro() {
        EstadoDePedido.Filtro.entries.forEach { filtro ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = filtro.etiqueta
                isCheckable = true
                isChecked = filtro == EstadoDePedido.Filtro.TODOS
                setEnsureMinTouchTargetSize(false)
                setOnClickListener { vm.filtrar(filtro) }
            }
            binding.chipsFiltro.addView(chip)
        }
    }

    private fun mostrarDialogoReembolso(pedido: HistorialItem) {
        val dialogo = DialogMotivoBinding.inflate(layoutInflater)
        val detalle = listOfNotNull(pedido.empresaNombre, pedido.monto.comoPrecio()).joinToString("  -  ")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Solicitar reembolso")
            .setMessage("$detalle\n\nCuentanos por que quieres el reembolso:")
            .setView(dialogo.root)
            .setPositiveButton("Enviar") { _, _ ->
                vm.solicitarReembolso(pedido, dialogo.editMotivo.text?.toString().orEmpty())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
