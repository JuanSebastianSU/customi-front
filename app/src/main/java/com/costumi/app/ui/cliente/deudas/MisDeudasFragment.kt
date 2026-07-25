package com.costumi.app.ui.cliente.deudas

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentMisDeudasBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

/**
 * Las multas y saldos del propio cliente (RF-7/11.5).
 *
 * El estado de cuenta ya existia, pero es por empresa y lo mira la tienda: el cliente no tenia donde ver
 * que le cobraron ni por que.
 */
@AndroidEntryPoint
class MisDeudasFragment : Fragment(R.layout.fragment_mis_deudas) {

    private val vm: MisDeudasViewModel by viewModels()
    private var _binding: FragmentMisDeudasBinding? = null
    private val binding get() = _binding!!
    private val adapter = DeudaAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMisDeudasBinding.bind(view)
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            if (estado !is UiState.Success<*>) adapter.submitList(emptyList())
            binding.stateView.mostrar(
                estado,
                vacio = "No tenes multas ni saldos pendientes.",
            ) { deudas ->
                adapter.submitList(deudas)
            }
        }

        observar(vm.saldoTotal) { total ->
            // La cabecera solo aparece si de verdad debe algo: una multa ya pagada sigue en la lista, pero
            // un encabezado en rojo diciendo "$0" seria alarmante y falso.
            binding.cabeceraDeuda.isVisible = total.signum() > 0
            binding.totalDeuda.text = total.comoPrecio()
        }
    }

    /** Al volver de pagar, lo que debe pudo cambiar. */
    override fun onResume() {
        super.onResume()
        vm.cargar()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
