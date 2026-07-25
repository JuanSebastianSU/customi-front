package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentLoginBinding
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
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

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonEntrar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAuth.Navegar -> findNavController().irAHome(evento.modo)
                is EventoAuth.Error -> mostrarMensaje(evento.mensaje)
                is EventoAuth.Info -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
