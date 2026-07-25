package com.costumi.app.ui.gestion.inventario

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogAjusteBinding
import com.costumi.app.databinding.DialogCantidadBinding
import com.costumi.app.databinding.DialogMoverBinding
import com.costumi.app.databinding.DialogSeleccionCantidadBinding
import com.costumi.app.databinding.FragmentGruposStockBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.AjusteDeStockRequest
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.MoverUnidadesRequest
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Grupos de stock de una prenda: alta de variante y operaciones (entrada/ajuste/mover/transferir/borrar). */
@AndroidEntryPoint
class GruposStockFragment : Fragment(R.layout.fragment_grupos_stock) {

    private val vm: GruposStockViewModel by viewModels()
    private var _binding: FragmentGruposStockBinding? = null
    private val binding get() = _binding!!

    private val adapter = GrupoStockAdapter(
        nombreSucursal = { vm.nombreSucursal(it) },
        describirVariante = { vm.describirVariante(it) },
        alAbrirAcciones = { grupo, ancla -> abrirAcciones(grupo, ancla) },
    )

    private val estados = listOf(
        "Disponible" to AjusteDeStockRequest.Estado.DISPONIBLE,
        "Danada" to AjusteDeStockRequest.Estado.DANADA,
        "En limpieza" to AjusteDeStockRequest.Estado.EN_LIMPIEZA,
        "Perdida" to AjusteDeStockRequest.Estado.PERDIDA,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGruposStockBinding.bind(view)
        binding.toolbar.title = vm.prendaNombre
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        binding.fabNueva.setOnClickListener { dialogoNuevoGrupo() }

        observar(vm.grupos) { estado ->
            binding.stateView.mostrar(estado, vacio = "Esta prenda no tiene stock. Crea una variante con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoStock.Info -> mostrarMensaje(evento.mensaje)
                is EventoStock.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun abrirAcciones(grupo: GrupoDeStockResponse, ancla: View) {
        val menu = androidx.appcompat.widget.PopupMenu(requireContext(), ancla)
        menu.menu.add(0, 1, 0, "Entrada de stock")
        menu.menu.add(0, 2, 1, "Ajuste")
        menu.menu.add(0, 3, 2, "Mover entre estados")
        menu.menu.add(0, 4, 3, "Transferir a sucursal")
        menu.menu.add(0, 5, 4, "Eliminar grupo")
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> dialogoEntrada(grupo)
                2 -> dialogoAjuste(grupo)
                3 -> dialogoMover(grupo)
                4 -> dialogoTransferir(grupo)
                5 -> dialogoEliminar(grupo)
            }
            true
        }
        menu.show()
    }

    private fun dialogoNuevoGrupo() {
        val sucursales = vm.sucursales.value
        if (sucursales.isEmpty()) {
            mostrarMensaje("No hay sucursales disponibles.")
            return
        }
        val d = DialogSeleccionCantidadBinding.inflate(layoutInflater)
        d.tilSeleccion.hint = "Sucursal"
        d.dropSeleccion.setSimpleItems(sucursales.map { it.nombre.orEmpty() }.toTypedArray())
        var elegida: SucursalResponse? = sucursales.singleOrNull()
        elegida?.let { d.dropSeleccion.setText(it.nombre.orEmpty(), false) }
        d.dropSeleccion.setOnItemClickListener { _, _, pos, _ -> elegida = sucursales.getOrNull(pos) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva variante")
            .setView(d.root)
            .setPositiveButton("Crear") { _, _ ->
                val cantidad = d.editCantidad.text?.toString()?.toIntOrNull() ?: 0
                val suc = elegida?.id
                if (suc == null) mostrarMensaje("Selecciona una sucursal")
                else vm.crearGrupo(suc, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoEntrada(grupo: GrupoDeStockResponse) {
        val id = grupo.id ?: return
        val d = DialogCantidadBinding.inflate(layoutInflater)
        d.til.hint = "Cantidad a ingresar"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Entrada de stock")
            .setView(d.root)
            .setPositiveButton("Ingresar") { _, _ ->
                val cantidad = d.editCantidad.text?.toString()?.toIntOrNull()
                if (cantidad == null || cantidad <= 0) mostrarMensaje("Ingresa una cantidad valida")
                else vm.entrada(id, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoAjuste(grupo: GrupoDeStockResponse) {
        val id = grupo.id ?: return
        val d = DialogAjusteBinding.inflate(layoutInflater)
        d.dropEstado.setSimpleItems(estados.map { it.first }.toTypedArray())
        var estado = estados.first().second
        d.dropEstado.setText(estados.first().first, false)
        d.dropEstado.setOnItemClickListener { _, _, pos, _ -> estado = estados[pos].second }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ajuste de stock")
            .setView(d.root)
            .setPositiveButton("Aplicar") { _, _ ->
                val delta = d.editDelta.text?.toString()?.toIntOrNull()
                val motivo = d.editMotivo.text?.toString()?.trim().orEmpty()
                when {
                    delta == null || delta == 0 -> mostrarMensaje("Ingresa un delta distinto de cero")
                    motivo.isBlank() -> mostrarMensaje("El ajuste requiere un motivo")
                    else -> vm.ajuste(id, estado, delta, motivo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoMover(grupo: GrupoDeStockResponse) {
        val id = grupo.id ?: return
        val d = DialogMoverBinding.inflate(layoutInflater)
        val opciones = listOf(
            "Disponible" to 0, "Danada" to 1, "En limpieza" to 2, "Perdida" to 3,
        ).map { it.first }
        val desdeValores = MoverUnidadesRequest.Desde.entries
        val haciaValores = MoverUnidadesRequest.Hacia.entries
        d.dropDesde.setSimpleItems(opciones.toTypedArray())
        d.dropHacia.setSimpleItems(opciones.toTypedArray())
        var desde = MoverUnidadesRequest.Desde.DISPONIBLE
        var hacia = MoverUnidadesRequest.Hacia.EN_LIMPIEZA
        d.dropDesde.setText(opciones[0], false)
        d.dropHacia.setText(opciones[2], false)
        d.dropDesde.setOnItemClickListener { _, _, pos, _ -> desde = desdeValores[pos] }
        d.dropHacia.setOnItemClickListener { _, _, pos, _ -> hacia = haciaValores[pos] }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mover unidades")
            .setView(d.root)
            .setPositiveButton("Mover") { _, _ ->
                val cantidad = d.editCantidad.text?.toString()?.toIntOrNull()
                if (cantidad == null || cantidad <= 0) mostrarMensaje("Ingresa una cantidad valida")
                else vm.mover(id, desde, hacia, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoTransferir(grupo: GrupoDeStockResponse) {
        val id = grupo.id ?: return
        val destinos = vm.sucursales.value.filter { it.id != grupo.sucursalId }
        if (destinos.isEmpty()) {
            mostrarMensaje("No hay otra sucursal para transferir.")
            return
        }
        val d = DialogSeleccionCantidadBinding.inflate(layoutInflater)
        d.tilSeleccion.hint = "Sucursal destino"
        d.dropSeleccion.setSimpleItems(destinos.map { it.nombre.orEmpty() }.toTypedArray())
        var elegida: SucursalResponse? = destinos.singleOrNull()
        elegida?.let { d.dropSeleccion.setText(it.nombre.orEmpty(), false) }
        d.dropSeleccion.setOnItemClickListener { _, _, pos, _ -> elegida = destinos.getOrNull(pos) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Transferir stock")
            .setView(d.root)
            .setPositiveButton("Transferir") { _, _ ->
                val cantidad = d.editCantidad.text?.toString()?.toIntOrNull()
                val destino = elegida?.id
                when {
                    destino == null -> mostrarMensaje("Selecciona una sucursal")
                    cantidad == null || cantidad <= 0 -> mostrarMensaje("Ingresa una cantidad valida")
                    else -> vm.transferir(id, destino, cantidad)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoEliminar(grupo: GrupoDeStockResponse) {
        val id = grupo.id ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar grupo")
            .setMessage("Solo se puede eliminar un grupo vacio que no sea el ultimo de la prenda en la sucursal. ¿Continuar?")
            .setPositiveButton("Eliminar") { _, _ -> vm.eliminar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_PRENDA_ID = "prendaId"
        const val ARG_PRENDA_NOMBRE = "prendaNombre"

        fun args(prendaId: UUID, prendaNombre: String?) = bundleOf(
            ARG_PRENDA_ID to prendaId.toString(),
            ARG_PRENDA_NOMBRE to prendaNombre,
        )
    }
}
