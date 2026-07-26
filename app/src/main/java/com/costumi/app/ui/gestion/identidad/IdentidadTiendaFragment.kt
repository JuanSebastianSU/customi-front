package com.costumi.app.ui.gestion.identidad

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogEditarTiendaBinding
import com.costumi.app.databinding.FragmentIdentidadTiendaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.extensionDeImagen
import com.costumi.app.ui.leerBytesDeImagen
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.app.ui.soloImagenes
import com.costumi.apiclient.models.EmpresaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Identidad de la tienda: cambiar el logo y la portada que ve el cliente en el marketplace. */
@AndroidEntryPoint
class IdentidadTiendaFragment : Fragment(R.layout.fragment_identidad_tienda) {

    private val vm: IdentidadTiendaViewModel by viewModels()
    private var _binding: FragmentIdentidadTiendaBinding? = null
    private val binding get() = _binding!!
    private var empresaActual: EmpresaResponse? = null

    private val elegirLogo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { leer(it) { b, m, n -> vm.subirLogo(b, m, n) } }
    }
    private val elegirPortada = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { leer(it) { b, m, n -> vm.subirPortada(b, m, n) } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIdentidadTiendaBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonLogo.setOnClickListener { elegirLogo.launch(soloImagenes) }
        binding.botonPortada.setOnClickListener { elegirPortada.launch(soloImagenes) }
        binding.botonEditar.setOnClickListener { dialogoEditar() }

        observar(vm.empresa) { empresa ->
            empresaActual = empresa
            binding.nombre.text = empresa?.nombre ?: "Mi tienda"
            binding.ciudad.text = empresa?.ciudad.orEmpty()
            binding.ciudad.isVisible = !empresa?.ciudad.isNullOrBlank()
            binding.descripcion.text = empresa?.descripcion.orEmpty()
            binding.descripcion.isVisible = !empresa?.descripcion.isNullOrBlank()
            binding.logo.cargarFoto(empresa?.logoUrl)
            binding.portada.cargarFoto(empresa?.portadaUrl)
        }
        observar(vm.procesando) { binding.progreso.isVisible = it }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoIdentidad.Info -> mostrarMensaje(evento.mensaje)
                is EventoIdentidad.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun dialogoEditar() {
        val d = DialogEditarTiendaBinding.inflate(layoutInflater)
        empresaActual?.let {
            d.editNombre.setText(it.nombre)
            d.editDescripcion.setText(it.descripcion)
            d.editCiudad.setText(it.ciudad)
            d.editUbicacion.setText(it.ubicacion)
            d.editContacto.setText(it.contacto)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar datos de la tienda")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                vm.editar(
                    d.editNombre.text?.toString().orEmpty(),
                    d.editDescripcion.text?.toString(),
                    d.editCiudad.text?.toString(),
                    d.editUbicacion.text?.toString(),
                    d.editContacto.text?.toString(),
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Lee los bytes de la imagen elegida (en IO, sin crashear) y ejecuta la subida correspondiente. */
    private fun leer(uri: Uri, subir: (ByteArray, String, String) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = requireContext().leerBytesDeImagen(uri)
            if (datos == null) { mostrarMensaje("No se pudo leer la imagen, intentá de nuevo"); return@launch }
            subir(datos.first, datos.second, "foto.${extensionDeImagen(datos.second)}")
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
