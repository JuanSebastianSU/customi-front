package com.costumi.app.ui.cliente.carrito

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentCarritoBinding
import com.costumi.app.ui.cliente.pago.PagoFragment
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.CarritoResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Carrito de la tienda (por tipo): revisa las lineas y finaliza el pedido (checkout). */
@AndroidEntryPoint
class CarritoFragment : Fragment(R.layout.fragment_carrito) {

    private val vm: CarritoViewModel by viewModels()
    private var _binding: FragmentCarritoBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy { CarritoLineaAdapter(vm.esRenta) { confirmarQuitar(it) } }

    /** Hay lineas que dejaron de poder cumplirse: no se finaliza hasta quitarlas. */
    private var checkoutBloqueado = false

    /** El boton se habilita solo si no se esta procesando y no hay lineas no disponibles. */
    private fun refrescarBotonFinalizar() {
        binding.botonFinalizar.isEnabled = !vm.procesando.value && !checkoutBloqueado
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCarritoBinding.bind(view)
        binding.toolbar.title = if (vm.esRenta) "Tu renta" else "Tu carrito"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.lista.adapter = adapter
        binding.botonFinalizar.setOnClickListener { vm.finalizar() }

        observar(vm.retiro) { retiro ->
            binding.cabeceraTienda.isVisible = !retiro.isNullOrBlank()
            binding.textoRetiro.text = retiro.orEmpty()
        }
        observar(vm.estado) { estado ->
            val hayDatos = estado is com.costumi.app.core.UiState.Success<CarritoResponse>
            binding.pie.isVisible = hayDatos
            // Al quedar vacio hay que limpiar la lista: si no, las lineas viejas siguen pintadas
            // debajo del estado vacio.
            if (!hayDatos) adapter.submitList(emptyList())
            binding.stateView.mostrar(estado, vacio = "Tu carrito esta vacio.") { carrito ->
                adapter.submitList(carrito.lineas.orEmpty())
                // Total real calculado por el backend (misma lógica que el checkout).
                binding.textoTotal.text = carrito.total.comoPrecio() ?: "-"
                binding.notaCarrito.text = "Eliges como pagar (tarjeta o en la tienda) en el siguiente paso."
                // Fail-fast: si alguna linea dejo de poder cumplirse, no se puede finalizar hasta quitarla.
                val noDisponibles = carrito.lineas.orEmpty().count { !it.motivoNoDisponible.isNullOrBlank() }
                checkoutBloqueado = noDisponibles > 0
                binding.avisoBloqueo.isVisible = checkoutBloqueado
                binding.avisoBloqueo.text = if (noDisponibles == 1) {
                    "Quita el articulo no disponible para continuar."
                } else {
                    "Quita los $noDisponibles articulos no disponibles para continuar."
                }
                refrescarBotonFinalizar()
            }
        }
        observar(vm.procesando) { procesando ->
            binding.progresoCheckout.isVisible = procesando
            refrescarBotonFinalizar()
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCheckout.IrAPago -> irAPago(evento)
                is EventoCheckout.Exito -> mostrarExito(evento.mensaje)
                is EventoCheckout.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Quitar es destructivo y no se puede deshacer: se confirma antes. */
    private fun confirmarQuitar(linea: com.costumi.apiclient.models.LineaDeCarritoResponse) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quitar del carrito")
            .setMessage("Se quitara \"${linea.nombre ?: "este articulo"}\" de tu carrito.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Quitar") { _, _ -> vm.quitar(linea) }
            .show()
    }

    private fun irAPago(evento: EventoCheckout.IrAPago) {
        findNavController().navigate(
            R.id.pagoFragment,
            bundleOf(
                PagoFragment.ARG_TIPO to evento.tipo,
                PagoFragment.ARG_CONCEPTO_ID to evento.conceptoId,
                PagoFragment.ARG_EMPRESA_ID to evento.empresaId,
                PagoFragment.ARG_SUCURSAL_ID to evento.sucursalId,
            ),
            // Al pagar se limpia el flujo de compra hasta Explorar.
            NavOptions.Builder().setPopUpTo(R.id.carritoFragment, true).build(),
        )
    }

    private fun mostrarExito(mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¡Listo!")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Entendido") { _, _ ->
                findNavController().popBackStack(R.id.explorarFragment, false)
            }
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_SUCURSAL_ID = "sucursalId"
        const val ARG_TIPO = "tipo"
    }
}
