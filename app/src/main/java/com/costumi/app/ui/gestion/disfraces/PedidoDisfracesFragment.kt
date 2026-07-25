package com.costumi.app.ui.gestion.disfraces

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentPedidoDisfracesBinding
import com.costumi.app.databinding.ItemPedidoDisfrazBinding
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Arma un pedido de VARIOS disfraces (carrito) y lo confirma como una sola renta/venta. */
@AndroidEntryPoint
class PedidoDisfracesFragment : Fragment(R.layout.fragment_pedido_disfraces) {

    private val vm: PedidoDisfracesViewModel by viewModels()
    private var _binding: FragmentPedidoDisfracesBinding? = null
    private val binding get() = _binding!!

    private var clientes: List<ClienteResponse> = emptyList()
    private var sucursales: List<SucursalResponse> = emptyList()
    private var disfraces: List<DisfrazResponse> = emptyList()
    private val formatoFecha = DateTimeFormatter.ofPattern("dd MMM", Locale("es"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPedidoDisfracesBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.grupoModo.check(if (vm.modoRenta) R.id.botonRentar else R.id.botonVender)
        binding.seccionFechas.isVisible = vm.modoRenta
        binding.grupoModo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            vm.modoRenta = checkedId == R.id.botonRentar
            binding.seccionFechas.isVisible = vm.modoRenta
        }

        binding.botonFechas.setOnClickListener { elegirFechas() }
        binding.botonAgregar.setOnClickListener { elegirDisfraz() }
        binding.botonConfirmar.setOnClickListener { vm.confirmar() }
        binding.textoFechas.text = fechasTexto()

        observar(vm.clientes) { clientes = it; configurarClientes() }
        observar(vm.sucursales) { sucursales = it; configurarSucursales() }
        observar(vm.disfraces) { disfraces = it }
        observar(vm.items) { pintarItems(it) }
        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonConfirmar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPedido.Exito -> {
                    mostrarMensaje(evento.mensaje)
                    findNavController().popBackStack()
                }
                is EventoPedido.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun configurarClientes() {
        binding.dropCliente.setSimpleItems(clientes.map { it.nombre.orEmpty() }.toTypedArray())
        clientes.firstOrNull { it.id == vm.clienteId }?.let { binding.dropCliente.setText(it.nombre.orEmpty(), false) }
        binding.dropCliente.setOnItemClickListener { _, _, pos, _ -> vm.clienteId = clientes.getOrNull(pos)?.id }
    }

    private fun configurarSucursales() {
        binding.dropSucursal.setSimpleItems(sucursales.map { it.nombre.orEmpty() }.toTypedArray())
        sucursales.firstOrNull { it.id == vm.sucursalId }?.let { binding.dropSucursal.setText(it.nombre.orEmpty(), false) }
        binding.dropSucursal.setOnItemClickListener { _, _, pos, _ -> vm.sucursalId = sucursales.getOrNull(pos)?.id }
    }

    private fun pintarItems(items: List<PedidoDisfracesStore.Item>) {
        val c = _binding?.contenedorItems ?: return
        c.removeAllViews()
        binding.vacio.isVisible = items.isEmpty()
        items.forEachIndexed { indice, item ->
            val fila = ItemPedidoDisfrazBinding.inflate(layoutInflater, c, false)
            fila.texto.text = "${item.nombre}  ·  x${item.cantidad}"
            fila.botonQuitar.setOnClickListener { vm.quitar(indice) }
            c.addView(fila.root)
        }
    }

    private fun elegirDisfraz() {
        if (disfraces.isEmpty()) {
            mostrarMensaje("No hay disfraces para agregar.")
            return
        }
        val nombres = disfraces.map { it.nombre.orEmpty().ifBlank { "Disfraz" } }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elige un disfraz")
            .setItems(nombres) { _, which ->
                val d = disfraces[which]
                val id = d.id ?: return@setItems
                findNavController().navigate(
                    R.id.disfrazAsignarFragment,
                    DisfrazAsignarFragment.argsPedido(id, d.nombre ?: "Disfraz"),
                )
            }
            .show()
    }

    private fun elegirFechas() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Fechas de renta")
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build(),
            )
            .build()
        picker.addOnPositiveButtonClickListener { rango ->
            vm.retiro = aLocalDate(rango.first)
            vm.devolucion = aLocalDate(rango.second)
            binding.textoFechas.text = fechasTexto()
        }
        picker.show(childFragmentManager, "fechas")
    }

    private fun fechasTexto(): String {
        val r = vm.retiro ?: return ""
        val d = vm.devolucion ?: return ""
        return "${r.format(formatoFecha)} - ${d.format(formatoFecha)}"
    }

    private fun aLocalDate(millis: Long?): LocalDate? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

    override fun onDestroyView() {
        binding.contenedorItems.removeAllViews()
        _binding = null
        super.onDestroyView()
    }
}
