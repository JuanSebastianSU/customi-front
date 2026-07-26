package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentRestablecerBinding
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RestablecerFragment : Fragment(R.layout.fragment_restablecer) {

    private val vm: RestablecerViewModel by viewModels()
    private var _binding: FragmentRestablecerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRestablecerBinding.bind(view)

        binding.botonGuardar.setOnClickListener {
            vm.restablecer(
                binding.editCodigo.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
                binding.editConfirmar.text?.toString().orEmpty(),
            )
        }

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonGuardar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAuth.Info -> {
                    mostrarMensaje(evento.mensaje)
                    // Vuelve a Login para iniciar sesion con la nueva contrasena.
                    findNavController().popBackStack(R.id.loginFragment, false)
                }
                is EventoAuth.Error -> mostrarMensaje(evento.mensaje)
                is EventoAuth.Navegar -> Unit
                is EventoAuth.InvitacionLista -> Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
