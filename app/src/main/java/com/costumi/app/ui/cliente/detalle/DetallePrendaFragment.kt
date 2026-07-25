package com.costumi.app.ui.cliente.detalle

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentDetallePrendaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.cliente.carrito.CarritoFragment
import com.costumi.app.ui.common.SelectorDeCantidad
import com.costumi.app.ui.common.SelectorDePeriodo
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.SucursalVitrinaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

/** Detalle de una prenda: elegir tipo (renta/compra), cantidad y fechas, y agregar al carrito. */
@AndroidEntryPoint
class DetallePrendaFragment : Fragment(R.layout.fragment_detalle_prenda) {

    private val vm: DetallePrendaViewModel by viewModels()
    private var _binding: FragmentDetallePrendaBinding? = null
    private val binding get() = _binding!!

    private var tipo: AgregarItemRequest.Tipo = AgregarItemRequest.Tipo.VENTA
    private var periodo: SelectorDePeriodo? = null
    private var cantidad: SelectorDeCantidad? = null
    /** La cantidad elegida sobrevive a recrear la vista (volver del carrito, girar la pantalla). */
    private var cantidadElegida: Int = 1
    /** Delegan en el ViewModel: la vista se recrea al volver atras y ahi se perdian. */
    private var retiro: LocalDate?
        get() = vm.fechaRetiro
        set(valor) { vm.fechaRetiro = valor }
    private var devolucion: LocalDate?
        get() = vm.fechaDevolucion
        set(valor) { vm.fechaDevolucion = valor }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDetallePrendaBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.foto.cargarFoto(vm.fotoUrl)
        binding.nombre.text = vm.nombre
        binding.categoria.isVisible = !vm.categoria.isNullOrBlank()
        binding.categoria.text = vm.categoria

        binding.botonRenta.isVisible = vm.permiteRenta
        binding.botonVenta.isVisible = vm.permiteVenta

        // Tipo inicial: renta si esta disponible, si no compra.
        tipo = if (vm.permiteRenta) AgregarItemRequest.Tipo.RENTA else AgregarItemRequest.Tipo.VENTA
        binding.grupoTipo.check(if (tipo == AgregarItemRequest.Tipo.RENTA) R.id.botonRenta else R.id.botonVenta)
        aplicarTipo()

        binding.grupoTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipo = if (checkedId == R.id.botonRenta) {
                AgregarItemRequest.Tipo.RENTA
            } else {
                AgregarItemRequest.Tipo.VENTA
            }
            aplicarTipo()
        }

        cantidad = SelectorDeCantidad(binding.cantidad, inicial = cantidadElegida) { cantidadElegida = it }
        periodo = SelectorDePeriodo(this, binding.periodo, "Fechas de renta") { desde, hasta ->
            retiro = desde
            devolucion = hasta
        }
        periodo?.fijar(retiro, devolucion)
        binding.botonAgregar.setOnClickListener {
            vm.agregar(tipo, cantidadElegida, retiro, devolucion)
        }

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonAgregar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoDetalle.Agregado -> irAlCarrito(evento)
                is EventoDetalle.Error -> mostrarMensaje(evento.mensaje)
            }
        }
        observar(vm.sucursales) { lista ->
            binding.seccionRetiro.isVisible = lista.isNotEmpty()
            // Solo se puede elegir si hay más de una sucursal; con una, es solo informativo.
            binding.botonSucursal.setOnClickListener(
                if (lista.size > 1) View.OnClickListener { elegirSucursal(lista) } else null,
            )
        }
        observar(vm.sucursalSeleccionada) { sucursal ->
            binding.botonSucursal.text = sucursal?.nombre ?: "Elegir sucursal"
        }
    }

    private fun elegirSucursal(lista: List<SucursalVitrinaResponse>) {
        val nombres = lista.map { s -> listOfNotNull(s.nombre, s.direccion).joinToString("  -  ") }.toTypedArray()
        val actual = lista.indexOfFirst { it.id == vm.sucursalSeleccionada.value?.id }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elige donde retirar")
            .setSingleChoiceItems(nombres, actual) { dialog, which ->
                lista[which].id?.toString()?.let { vm.seleccionarSucursal(it) }
                dialog.dismiss()
            }
            .show()
    }

    private fun aplicarTipo() {
        val esRenta = tipo == AgregarItemRequest.Tipo.RENTA
        binding.seccionFechas.isVisible = esRenta
        // La cantidad aplica a las dos: se pueden rentar varias unidades de la misma prenda en un solo
        // pedido (el backend siempre acepto cantidad en renta).
        binding.precio.text = if (esRenta) vm.precioRentaTexto else vm.precioVentaTexto
    }

    private fun irAlCarrito(evento: EventoDetalle.Agregado) {
        mostrarMensaje("Agregado al carrito")
        findNavController().navigate(
            R.id.carritoFragment,
            bundleOf(
                CarritoFragment.ARG_EMPRESA_ID to requireArguments().getString(ARG_EMPRESA_ID),
                CarritoFragment.ARG_SUCURSAL_ID to evento.sucursalId,
                CarritoFragment.ARG_TIPO to evento.tipo.value,
            ),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_PRENDA_ID = "prendaId"
        const val ARG_NOMBRE = "nombre"
        const val ARG_CATEGORIA = "categoria"
        const val ARG_PRECIO_RENTA = "precioRenta"
        const val ARG_PRECIO_VENTA = "precioVenta"
        const val ARG_FOTO_URL = "fotoUrl"
    }
}
