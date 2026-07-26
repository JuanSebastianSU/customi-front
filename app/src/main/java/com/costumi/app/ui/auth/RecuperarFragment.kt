package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentRecuperarBinding
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecuperarFragment : Fragment(R.layout.fragment_recuperar) {

    private val vm: RecuperarViewModel by viewModels()
    private var _binding: FragmentRecuperarBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRecuperarBinding.bind(view)

        binding.botonEnviar.setOnClickListener {
            vm.enviar(binding.editEmail.text?.toString().orEmpty())
        }
        binding.botonYaTengoCodigo.setOnClickListener {
            findNavController().navigate(R.id.restablecerFragment)
        }

        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonEnviar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoAuth.Info -> {
                    mostrarMensaje(evento.mensaje)
                    findNavController().navigate(R.id.restablecerFragment)
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
