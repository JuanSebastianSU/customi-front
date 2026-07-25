package com.costumi.app.ui.cliente.pago

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentPagoBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Pantalla de pago: total + código de retiro; elegir tarjeta (en línea) o efectivo (en la tienda). */
@AndroidEntryPoint
class PagoFragment : Fragment(R.layout.fragment_pago) {

    private val vm: PagoViewModel by viewModels()
    private var _binding: FragmentPagoBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPagoBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.botonTarjeta.setOnClickListener { vm.pagarConTarjeta() }
        binding.botonEfectivo.setOnClickListener { vm.pagarEnTienda() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No encontramos tu pedido.") { ui ->
                binding.resumen.text = listOfNotNull(ui.tipoTexto, ui.tienda).joinToString("  ·  ")
                binding.total.text = ui.total.comoPrecio() ?: "Total no disponible"
                binding.codigo.text = ui.codigoRetiro ?: "—"
            }
        }
        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonTarjeta.isEnabled = !cargando
            binding.botonEfectivo.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPago.AbrirCheckout -> abrirCheckout(evento.url)
                is EventoPago.PagoEnTienda -> confirmarEfectivo()
                is EventoPago.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun abrirCheckout(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            mostrarMensaje("Completa el pago en el navegador. Tienes 24 horas.")
            irAMisPedidos()
        } catch (e: ActivityNotFoundException) {
            mostrarMensaje("No se pudo abrir el navegador para el pago.")
        }
    }

    private fun confirmarEfectivo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pago en la tienda")
            .setMessage("Tu pedido quedó reservado. Pasa por la tienda con tu código a pagar y retirar. Tienes 24 horas.")
            .setPositiveButton("Entendido") { _, _ -> irAMisPedidos() }
            .setCancelable(false)
            .show()
    }

    private fun irAMisPedidos() {
        findNavController().navigate(
            R.id.misPedidosFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.explorarFragment, false).build(),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_TIPO = "tipo"
        const val ARG_CONCEPTO_ID = "conceptoId"
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_SUCURSAL_ID = "sucursalId"
    }
}
