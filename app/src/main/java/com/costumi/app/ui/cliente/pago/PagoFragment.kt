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
            binding.stateView.mostrar(estado, vacio = "Tu carrito esta vacio.") { ui ->
                val deposito = ui.deposito?.takeIf { it.signum() > 0 }
                binding.resumen.text = if (deposito != null) {
                    "${ui.tipoTexto}  ·  incluye depósito ${deposito.comoPrecio()}"
                } else {
                    ui.tipoTexto
                }
                binding.total.text = ui.total.comoPrecio() ?: "Total no disponible"
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
                is EventoPago.Reservado -> confirmarEfectivo(evento.codigo)
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

    /** El pedido se creó recién ahora (al confirmar): por eso el código se muestra en este momento. */
    private fun confirmarEfectivo(codigo: String?) {
        val mensaje = buildString {
            append("Tu pedido quedó reservado. ")
            if (codigo != null) {
                append("Tu código de retiro es:\n\n$codigo\n\n")
            } else {
                append("Revisa tu código en Mis Pedidos. ")
            }
            append("Pasa por la tienda con tu código a pagar y retirar. Tienes 24 horas.")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pago en la tienda")
            .setMessage(mensaje)
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
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_SUCURSAL_ID = "sucursalId"
    }
}
