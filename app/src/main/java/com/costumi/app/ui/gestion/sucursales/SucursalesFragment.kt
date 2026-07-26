package com.costumi.app.ui.gestion.sucursales

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogSucursalBinding
import com.costumi.app.databinding.FragmentSucursalesBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Sucursales de la empresa: listar, alta, editar y archivar/activar. */
@AndroidEntryPoint
class SucursalesFragment : Fragment(R.layout.fragment_sucursales) {

    private val vm: SucursalesViewModel by viewModels()
    private var _binding: FragmentSucursalesBinding? = null
    private val binding get() = _binding!!
    private val adapter = SucursalAdapter { sucursal, accion -> accionar(sucursal, accion) }

    // Foto de la tienda: la sucursal en edición y su vista previa en el diálogo abierto.
    private var fotoTarget: UUID? = null
    private var fotoPreview: ImageView? = null

    /** Selector de imagen del sistema; sube la foto a la sucursal en edición. */
    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { leerYSubirFoto(it) }
    }

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
        val sucursalId = sucursal?.id
        // Foto de la tienda: solo con una sucursal ya creada (el endpoint necesita su id).
        sucursal?.fotoUrl?.takeIf { it.isNotBlank() }?.let {
            d.foto.isVisible = true
            d.foto.cargarFoto(it)
            d.botonFoto.text = "Cambiar foto de la tienda"
        }
        d.botonFoto.setOnClickListener {
            if (sucursalId == null) {
                mostrarMensaje("Guardá la sucursal primero para agregarle una foto.")
            } else {
                fotoTarget = sucursalId
                fotoPreview = d.foto
                seleccionarFoto.launch("image/*")
            }
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

    /** Lee los bytes de la imagen elegida (en IO), muestra la vista previa y la sube a la sucursal. */
    private fun leerYSubirFoto(uri: Uri) {
        val target = fotoTarget ?: return
        fotoPreview?.let { it.isVisible = true; it.setImageURI(uri) }
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = withContext(Dispatchers.IO) {
                val cr = requireContext().contentResolver
                val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                bytes to (cr.getType(uri) ?: "image/*")
            }
            if (datos == null) { mostrarMensaje("No se pudo leer la imagen"); return@launch }
            val ext = when (datos.second) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            vm.subirFoto(target, datos.first, datos.second, "foto.$ext")
        }
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
