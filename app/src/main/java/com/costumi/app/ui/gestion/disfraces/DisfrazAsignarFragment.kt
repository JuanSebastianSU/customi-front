package com.costumi.app.ui.gestion.disfraces

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.databinding.FragmentDisfrazAsignarBinding
import com.costumi.app.databinding.ViewSlotSeccionBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.cliente.detalle.SlotOpcionAdapter
import com.costumi.app.ui.cliente.detalle.SlotUi
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.app.ui.common.SelectorDeCantidad
import com.costumi.app.ui.common.SelectorDePeriodo
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Vender/rentar un disfraz a un cliente desde mostrador: arma las piezas (ruleta) y confirma. */
@AndroidEntryPoint
class DisfrazAsignarFragment : Fragment(R.layout.fragment_disfraz_asignar) {

    private val vm: DisfrazAsignarViewModel by viewModels()
    private var _binding: FragmentDisfrazAsignarBinding? = null
    private val binding get() = _binding!!

    private var modoRenta = true
    private var periodo: SelectorDePeriodo? = null
    private var cantidad: SelectorDeCantidad? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDisfrazAsignarBinding.bind(view)
        binding.toolbar.title = vm.nombre
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.grupoModo.check(R.id.botonRentar)
        aplicarModo()
        binding.grupoModo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            modoRenta = checkedId == R.id.botonRentar
            aplicarModo()
        }
        periodo = SelectorDePeriodo(this, binding.periodo, "Fechas de renta")
        cantidad = SelectorDeCantidad(binding.cantidad, inicial = vm.cantidad) { vm.cantidad = it }
        cantidad?.etiqueta("Cuantos iguales")
        binding.botonConfirmar.setOnClickListener {
            when {
                vm.esPedido -> vm.agregarAlPedido()
                modoRenta -> vm.rentar(periodo?.retiro, periodo?.devolucion)
                else -> vm.vender()
            }
        }
        if (vm.esPedido) aplicarModoPedido()

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Este disfraz no se puede asignar.") { ui -> pintar(ui) }
            // Solo se puede confirmar/agregar cuando las opciones cargaron (evita agregar el disfraz sin elegir sus piezas).
            binding.botonConfirmar.isEnabled = estado is UiState.Success && !vm.cargando.value
        }
        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonConfirmar.isEnabled = !cargando && vm.estado.value is UiState.Success
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAsignar.Exito -> {
                    mostrarMensaje(evento.mensaje)
                    findNavController().popBackStack()
                }
                is EventoAsignar.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun pintar(ui: AsignarUi) {
        binding.foto.cargarFoto(ui.disfraz.fotoUrl)
        binding.disponibilidad.text = if (ui.disponible) "Disponible" else "No disponible ahora"
        construirSlots(ui.slots)

        val nombresCliente = ui.clientes.map { it.nombre.orEmpty() }.toTypedArray()
        binding.dropCliente.setSimpleItems(nombresCliente)
        binding.dropCliente.setOnItemClickListener { _, _, pos, _ ->
            vm.clienteId = ui.clientes.getOrNull(pos)?.id
        }

        val nombresSucursal = ui.sucursales.map { it.nombre.orEmpty() }.toTypedArray()
        binding.dropSucursal.setSimpleItems(nombresSucursal)
        ui.sucursales.firstOrNull()?.let {
            binding.dropSucursal.setText(it.nombre.orEmpty(), false)
            vm.sucursalId = it.id
        }
        binding.dropSucursal.setOnItemClickListener { _, _, pos, _ ->
            vm.sucursalId = ui.sucursales.getOrNull(pos)?.id
        }
    }

    private fun construirSlots(slots: List<SlotUi>) {
        val contenedor = binding.contenedorSlots
        contenedor.removeAllViews()
        for (slot in slots) {
            val seccion = ViewSlotSeccionBinding.inflate(layoutInflater, contenedor, false)
            val etiqueta = if (slot.personalizable) "elige uno" else "incluido"
            val opcionalTxt = if (slot.opcional) " (opcional)" else ""
            seccion.titulo.text = "${slot.nombre}  ·  $etiqueta$opcionalTxt"
            if (slot.opciones.isEmpty()) {
                seccion.vacio.isVisible = true
                seccion.opciones.isVisible = false
            } else {
                val adapter = SlotOpcionAdapter { opcion -> vm.seleccionar(slot.orden, opcion.prendaId) }
                seccion.opciones.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                seccion.opciones.adapter = adapter
                adapter.submitList(slot.opciones)
                adapter.seleccionadaId = slot.seleccionInicial
            }
            contenedor.addView(seccion.root)
        }
    }

    private fun aplicarModo() {
        binding.seccionFechas.isVisible = modoRenta
        binding.botonConfirmar.text = if (modoRenta) "Rentar a cliente" else "Vender a cliente"
    }

    /** Modo pedido: el cliente/sucursal/fechas/modo los pone el carrito; aquí solo se arma y se agrega. */
    private fun aplicarModoPedido() {
        binding.tilCliente.isVisible = false
        binding.tilSucursal.isVisible = false
        binding.grupoModo.isVisible = false
        binding.seccionFechas.isVisible = false
        binding.botonConfirmar.text = "Agregar al pedido"
        binding.toolbar.title = "Agregar disfraz al pedido"
    }

    override fun onDestroyView() {
        periodo = null
        cantidad = null
        binding.contenedorSlots.removeAllViews()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_DISFRAZ_ID = "disfrazId"
        const val ARG_NOMBRE = "nombre"
        const val ARG_PEDIDO = "pedido"

        fun args(id: UUID, nombre: String) =
            bundleOf(ARG_DISFRAZ_ID to id.toString(), ARG_NOMBRE to nombre)

        /** Modo pedido: configura este disfraz y lo agrega al carrito (no renta/vende directo). */
        fun argsPedido(id: UUID, nombre: String) =
            bundleOf(ARG_DISFRAZ_ID to id.toString(), ARG_NOMBRE to nombre, ARG_PEDIDO to true)
    }
}
