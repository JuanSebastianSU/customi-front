package com.costumi.app.ui.cliente.perfil

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogCambiarContrasenaBinding
import com.costumi.app.databinding.DialogRegistrarTiendaBinding
import com.costumi.app.databinding.FragmentPerfilBinding
import com.costumi.app.core.ModoApp
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.irALogin
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Perfil: datos propios (nombre/telefono), cambio de contrasena, "Registrar mi tienda" y cerrar sesion. */
@AndroidEntryPoint
class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    private val vm: PerfilViewModel by viewModels()
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private var nombreActual: String? = null
    private var correoActual: String? = null

    /** Selector de imagen del sistema para la foto de perfil; al elegir, la sube. */
    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { leerYSubirFoto(it) }
    }

    /** Cabecera de identidad: nombre (o el usuario del correo) y la inicial en el avatar. */
    private fun actualizarCabecera() {
        val nombre = nombreActual?.takeIf { it.isNotBlank() }
        val correo = correoActual?.takeIf { it.isNotBlank() }
        binding.nombreHeader.text = nombre ?: correo?.substringBefore('@') ?: "Tu cuenta"
        binding.inicial.text = (nombre ?: correo ?: "?").trim().take(1).uppercase()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPerfilBinding.bind(view)

        // Foto de perfil: al tocar el avatar se elige una imagen y se sube a S3.
        binding.avatarContenedor.setOnClickListener { seleccionarFoto.launch("image/*") }
        binding.botonLogout.setOnClickListener { vm.cerrarSesion() }
        binding.botonRegistrarTienda.setOnClickListener { mostrarDialogoTienda() }
        // Las multas viven aca y no en una pestania propia: son informacion de la cuenta, no navegacion
        // primaria, y la barra ya tiene cuatro pestanias.
        binding.botonMisMultas.setOnClickListener { findNavController().navigate(R.id.misDeudasFragment) }
        binding.botonGuardados.setOnClickListener { findNavController().navigate(R.id.favoritosFragment) }
        binding.botonGuardar.setOnClickListener {
            vm.guardarDatos(
                binding.editNombre.text?.toString().orEmpty(),
                binding.editTelefono.text?.toString().orEmpty(),
            )
        }
        binding.botonContrasena.setOnClickListener { mostrarDialogoContrasena() }

        observar(vm.email) { correo ->
            correoActual = correo
            binding.email.text = correo ?: "-"
            actualizarCabecera()
        }
        observar(vm.perfil) { perfil ->
            // Solo se rellena si el campo esta vacio: no se pisa lo que el usuario esta escribiendo.
            if (binding.editNombre.text.isNullOrEmpty()) binding.editNombre.setText(perfil?.nombre.orEmpty())
            if (binding.editTelefono.text.isNullOrEmpty()) binding.editTelefono.setText(perfil?.telefono.orEmpty())
            nombreActual = perfil?.nombre
            actualizarCabecera()
            // Avatar: si hay foto la mostramos (encima de la inicial).
            val foto = perfil?.fotoUrl?.takeIf { it.isNotBlank() }
            binding.avatarFoto.isVisible = foto != null
            binding.inicial.isVisible = foto == null
            if (foto != null) binding.avatarFoto.cargarFoto(foto)
        }
        observar(vm.procesando) { procesando ->
            binding.progreso.isVisible = procesando
            binding.botonRegistrarTienda.isEnabled = !procesando
            binding.botonGuardar.isEnabled = !procesando
            binding.botonContrasena.isEnabled = !procesando
        }
        // Modo trabajo (Fase B): visible solo si la persona tiene una membresía de trabajo activa.
        binding.botonTrabajar.setOnClickListener { vm.entrarATrabajar() }
        binding.botonDesvincularme.setOnClickListener { confirmarDesvincularme() }
        observar(vm.membresiaActiva) { m ->
            binding.botonTrabajar.isVisible = m != null
            binding.botonDesvincularme.isVisible = m != null
            binding.botonTrabajar.text = m?.empresaNombre?.let { "Entrar a trabajar en $it" } ?: "Entrar a trabajar"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                EventoPerfil.SesionCerrada ->
                    requireActivity().findNavController(R.id.nav_host).irALogin()
                EventoPerfil.IrAGestion ->
                    requireActivity().findNavController(R.id.nav_host).irAHome(ModoApp.GESTION)
                is EventoPerfil.Info -> mostrarMensaje(evento.mensaje)
                is EventoPerfil.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Lee los bytes de la imagen elegida (en IO) y la sube como foto de perfil. */
    private fun leerYSubirFoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = withContext(Dispatchers.IO) {
                val cr = requireContext().contentResolver
                val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                bytes to (cr.getType(uri) ?: "image/*")
            }
            if (datos == null) { mostrarMensaje("No se pudo leer la imagen"); return@launch }
            val ext = when (datos.second) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            vm.subirFoto(datos.first, datos.second, "foto.$ext")
        }
    }

    private fun confirmarDesvincularme() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Desvincularte de la tienda?")
            .setMessage("Dejarás de trabajar ahí y quedarás como solo cliente. Para volver, la tienda deberá re-invitarte.")
            .setPositiveButton("Desvincularme") { _, _ -> vm.desvincularme() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoContrasena() {
        val dialogo = DialogCambiarContrasenaBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar contrasena")
            .setView(dialogo.root)
            .setPositiveButton("Cambiar") { _, _ ->
                vm.cambiarContrasena(
                    dialogo.editActual.text?.toString().orEmpty(),
                    dialogo.editNueva.text?.toString().orEmpty(),
                    dialogo.editRepetir.text?.toString().orEmpty(),
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoTienda() {
        val dialogo = DialogRegistrarTiendaBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar mi tienda")
            .setMessage("Solicita abrir tu tienda. Un administrador la revisa y, al aprobarla, te conviertes en su dueño.")
            .setView(dialogo.root)
            .setPositiveButton("Enviar") { _, _ ->
                vm.registrarTienda(
                    dialogo.editNombre.text?.toString().orEmpty(),
                    dialogo.editUbicacion.text?.toString(),
                    dialogo.editContacto.text?.toString(),
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
