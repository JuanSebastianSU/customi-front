package com.costumi.app.ui.gestion.sucursales

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogSucursalBinding
import com.costumi.app.databinding.FragmentSucursalesBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Sucursales de la empresa: listar, alta, editar y archivar/activar. */
@AndroidEntryPoint
class SucursalesFragment : Fragment(R.layout.fragment_sucursales) {

    private val vm: SucursalesViewModel by viewModels()
    private var _binding: FragmentSucursalesBinding? = null
    private val binding get() = _binding!!
    private val adapter = SucursalAdapter { sucursal, accion -> accionar(sucursal, accion) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSucursalesBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { dialogoSucursal(null) }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay sucursales. Crea la primera.") { adapter.submitList(it) }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoSucursal.Info -> mostrarMensaje(evento.mensaje)
                is EventoSucursal.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun accionar(sucursal: SucursalResponse, accion: AccionSucursal) {
        val id = sucursal.id ?: return
        when (accion) {
            AccionSucursal.EDITAR -> dialogoSucursal(sucursal)
            AccionSucursal.ARCHIVAR -> confirmarArchivar(id, sucursal.nombre.orEmpty())
            AccionSucursal.ACTIVAR -> vm.activar(id)
        }
    }

    /** null = alta; con sucursal = edición (prefill). */
    private fun dialogoSucursal(sucursal: SucursalResponse?) {
        val d = DialogSucursalBinding.inflate(layoutInflater)
        // Afford­ance de foto de la tienda (maquetado): el endpoint para subirla aún no existe.
        d.botonFoto.setOnClickListener {
            mostrarMensaje("La foto de la tienda estará disponible pronto.")
        }
        sucursal?.let {
            d.editNombre.setText(it.nombre)
            d.editDireccion.setText(it.direccion)
            d.editMaps.setText(it.ubicacionMaps)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (sucursal == null) "Nueva sucursal" else "Editar sucursal")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = d.editNombre.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) { mostrarMensaje("El nombre es obligatorio"); return@setPositiveButton }
                val direccion = d.editDireccion.text?.toString()?.trim()
                val maps = d.editMaps.text?.toString()?.trim()
                if (sucursal?.id == null) vm.crear(nombre, direccion, maps) else vm.editar(sucursal.id!!, nombre, direccion, maps)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarArchivar(id: java.util.UUID, nombre: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archivar sucursal?")
            .setMessage("$nombre dejara de estar disponible para operar. Podras reactivarla luego.")
            .setPositiveButton("Archivar") { _, _ -> vm.archivar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
