package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentLoginBinding
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.costumi.apiclient.models.InvitacionVistaResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLoginBinding.bind(view)

        binding.botonEntrar.setOnClickListener {
            vm.login(
                binding.editEmail.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
            )
        }
        binding.botonOlvide.setOnClickListener {
            findNavController().navigate(R.id.recuperarFragment)
        }
        binding.botonRegistro.setOnClickListener {
            findNavController().navigate(R.id.registroFragment)
        }
        binding.botonInvitacion.setOnClickListener { dialogoInvitacion() }

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonEntrar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAuth.Navegar -> findNavController().irAHome(evento.modo)
                is EventoAuth.Error -> mostrarMensaje(evento.mensaje)
                is EventoAuth.Info -> mostrarMensaje(evento.mensaje)
                is EventoAuth.InvitacionLista -> dialogoAceptar(evento.vista, evento.token)
            }
        }
    }

    /** Paso 1: pegar el enlace/código de la invitación → se previsualiza. */
    private fun dialogoInvitacion() {
        val input = EditText(requireContext()).apply {
            hint = "Pegá el enlace o código de la invitación"
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aceptar invitación")
            .setView(input)
            .setPositiveButton("Continuar") { _, _ -> vm.verInvitacion(input.text?.toString().orEmpty()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Paso 2: mostrar a qué acepta + contraseña + T&C → aceptar. */
    private fun dialogoAceptar(vista: InvitacionVistaResponse, token: String) {
        val ctx = requireContext()
        val pass = EditText(ctx).apply {
            hint = if (vista.necesitaCuenta == true) "Creá una contraseña" else "Tu contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val terminos = CheckBox(ctx).apply { text = "Acepto los términos y condiciones" }
        val cont = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(pass); addView(terminos)
        }
        val empresa = vista.empresaNombre ?: "una tienda"
        val rol = vista.rol?.lowercase() ?: "trabajador"
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Unirte a $empresa")
            .setMessage("Te invitaron a trabajar en $empresa como $rol" +
                (vista.email?.let { " ($it)" } ?: "") + ".")
            .setView(cont)
            .setPositiveButton("Aceptar y entrar") { _, _ ->
                vm.aceptarInvitacion(token, pass.text?.toString().orEmpty(), terminos.isChecked)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
