package com.costumi.app.ui.gestion.ventas

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.costumi.app.R
import com.costumi.app.databinding.FragmentVentasBinding
import com.costumi.app.databinding.ItemDevolverLineaBinding
import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.gestion.mostrarDesglose
import com.costumi.app.ui.gestion.pagos.PagoConceptoFragment
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.costumi.apiclient.models.LineaADevolver
import com.costumi.apiclient.models.VentaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Ventas — listado paginado con alta (POS) y devolución por línea. */
@AndroidEntryPoint
class VentasFragment : Fragment(R.layout.fragment_ventas) {

    private val vm: VentasViewModel by viewModels()
    private var _binding: FragmentVentasBinding? = null
    private val binding get() = _binding!!
    private val adapter = VentaAdapter(
        alTocar = { mostrarDesgloseVenta(it) },
        alDevolver = { dialogoDevolver(it) },
        alCobrar = { abrirCobros(it) },
    )

    /** Desglose de la venta: sus artículos (foto+nombre+cantidad+subtotal), total y código de retiro. */
    private fun mostrarDesgloseVenta(v: VentaResponse) {
        // Las piezas que salieron de un mismo disfraz se muestran como UN artículo (el nombre del disfraz).
        val (deDisfraz, sueltas) = v.lineas.orEmpty().partition { it.disfrazGrupo != null }
        val disfraces = deDisfraz.groupBy { it.disfrazGrupo!! }.map { (_, piezas) ->
            val primera = piezas.first()
            val subtotal = piezas.mapNotNull { it.subtotal }
                .fold(java.math.BigDecimal.ZERO) { a, b -> a + b }
            val devueltoN = piezas.sumOf { it.cantidadDevuelta ?: 0 }
            val devuelto = if (devueltoN > 0) " · devuelto $devueltoN" else ""
            LineaDesglose(
                fotoUrl = piezas.firstOrNull { !it.fotoUrl.isNullOrBlank() }?.fotoUrl,
                nombre = primera.disfrazNombre ?: "Disfraz",
                detalle = "Cantidad: ${primera.disfrazCantidad ?: 1} · ${piezas.size} piezas$devuelto",
                monto = subtotal.comoPrecio(),
            )
        }
        val lineasSueltas = sueltas.map { l ->
            val devuelto = (l.cantidadDevuelta ?: 0).takeIf { it > 0 }?.let { " · devuelto $it" } ?: ""
            LineaDesglose(
                fotoUrl = l.fotoUrl,
                nombre = l.nombre ?: "Articulo",
                detalle = "Cantidad: ${l.cantidad ?: 1}$devuelto  ·  ${l.precioUnitario.comoPrecio() ?: "-"} c/u",
                monto = l.subtotal.comoPrecio(),
            )
        }
        val lineas = disfraces + lineasSueltas
        val pie = buildList {
            v.descuento?.takeIf { it.signum() > 0 }?.let { add("Descuento" to (it.comoPrecio() ?: "-")) }
            v.total?.let { add("Total" to (it.comoPrecio() ?: "-")) }
            v.montoReembolsado?.takeIf { it.signum() > 0 }?.let { add("Devuelto" to (it.comoPrecio() ?: "-")) }
        }
        mostrarDesglose(
            titulo = "Compra · ${EstadoDeVenta.etiqueta(v)}",
            codigoRetiro = v.codigoRetiro,
            lineas = lineas,
            pie = pie,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentVentasBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por codigo de retiro (V-...)"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.lista.adapter = adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })

        setFragmentResultListener(VentaPosFragment.RESULT_REGISTRADA) { _, _ -> adapter.refresh() }
        setFragmentResultListener(PagoConceptoFragment.RESULT_COBRADO) { _, _ -> adapter.refresh() }
        binding.fabNueva.setOnClickListener { findNavController().navigate(R.id.ventaPosFragment) }

        // Toggle Ventas/Rentas: Ventas es esta pantalla; tocar Rentas abre su gestión.
        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId == R.id.botonRentas) {
                findNavController().navigate(
                    R.id.rentasFragment,
                    null,
                    androidx.navigation.navOptions {
                        launchSingleTop = true
                        popUpTo(R.id.ventasFragment) { inclusive = false }
                    },
                )
            }
        }

        pintarChipsEstado()

        // Filtro por período (A8): en el menú del toolbar.
        binding.toolbar.menu.add("Filtrar por fecha").setOnMenuItemClickListener { mostrarRango(); true }
        binding.toolbar.menu.add("Quitar filtro de fecha").setOnMenuItemClickListener { vm.fijarRango(null, null); true }
        observar(vm.rango) { r -> binding.toolbar.subtitle = r?.let { "${it.first} → ${it.second}" } }

        observar(vm.ventas) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoVenta.Info -> { mostrarMensaje(evento.mensaje); adapter.refresh() }
                is EventoVenta.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Chips de filtro por estado (server-side): Todas + los estados posibles de una venta. */
    private fun pintarChipsEstado() {
        val estados = listOf<Pair<String, com.costumi.apiclient.apis.VentaControllerApi.EstadoListar?>>(
            "Todas" to null,
            "Confirmada" to com.costumi.apiclient.apis.VentaControllerApi.EstadoListar.CONFIRMADA,
            "Devuelta en parte" to com.costumi.apiclient.apis.VentaControllerApi.EstadoListar.PARCIALMENTE_DEVUELTA,
            "Devuelta" to com.costumi.apiclient.apis.VentaControllerApi.EstadoListar.DEVUELTA,
        )
        val grupo = binding.chipsEstado
        estados.forEachIndexed { i, (etiqueta, estado) ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = etiqueta
                isCheckable = true
                isChecked = i == 0
                setEnsureMinTouchTargetSize(false)
                setOnClickListener { vm.filtrarEstado(estado); adapter.refresh() }
            }
            grupo.addView(chip)
        }
    }

    private fun dialogoDevolver(venta: VentaResponse) {
        val id = venta.id ?: return
        val devolvibles = venta.lineas.orEmpty().filter { (it.cantidad ?: 0) - (it.cantidadDevuelta ?: 0) > 0 }
        if (devolvibles.isEmpty()) {
            mostrarMensaje("Esta venta no tiene unidades por devolver.")
            return
        }
        val contenedor = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val filas = devolvibles.map { linea ->
            val max = (linea.cantidad ?: 0) - (linea.cantidadDevuelta ?: 0)
            val fila = ItemDevolverLineaBinding.inflate(layoutInflater, contenedor, false)
            fila.etiqueta.text = "${vm.nombrePrenda(linea.prendaId)} (max $max)"
            contenedor.addView(fila.root)
            Triple(linea.prendaId, max, fila)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Devolver unidades")
            .setView(contenedor)
            .setPositiveButton("Devolver") { _, _ ->
                val lineas = filas.mapNotNull { (prendaId, max, fila) ->
                    val cant = fila.editCantidad.text?.toString()?.toIntOrNull() ?: 0
                    if (prendaId != null && cant in 1..max) LineaADevolver(prendaId, cant) else null
                }
                vm.devolver(id, lineas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirCobros(venta: VentaResponse) {
        val id = venta.id ?: return
        findNavController().navigate(
            R.id.pagoConceptoFragment,
            PagoConceptoFragment.args(
                tipo = "VENTA",
                conceptoId = id,
                sucursalId = venta.sucursalId,
                total = venta.total,
                titulo = "Venta · ${venta.total.comoPrecio() ?: "-"}",
            ),
        )
    }

    private fun mostrarRango() {
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Rango de fechas").build()
        picker.addOnPositiveButtonClickListener { seleccion ->
            vm.fijarRango(aLocalDate(seleccion.first), aLocalDate(seleccion.second))
        }
        picker.show(childFragmentManager, "rango")
    }

    private fun aLocalDate(millis: Long?): LocalDate? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar las ventas.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) {
                    binding.stateView.vacio("No hay ventas. Registra la primera con el boton +.")
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
