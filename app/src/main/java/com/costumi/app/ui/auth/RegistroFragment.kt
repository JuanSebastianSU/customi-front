package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentRegistroBinding
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistroFragment : Fragment(R.layout.fragment_registro) {

    private val vm: RegistroViewModel by viewModels()
    private var _binding: FragmentRegistroBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRegistroBinding.bind(view)

        binding.botonRegistrar.setOnClickListener {
            vm.registrar(
                binding.editEmail.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
                binding.editConfirmar.text?.toString().orEmpty(),
            )
        }
        binding.botonVolver.setOnClickListener { findNavController().popBackStack() }

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonRegistrar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAuth.Navegar -> findNavController().irAHome(evento.modo)
                is EventoAuth.Error -> mostrarMensaje(evento.mensaje)
                is EventoAuth.Info -> mostrarMensaje(evento.mensaje)
                is EventoAuth.InvitacionLista -> Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
