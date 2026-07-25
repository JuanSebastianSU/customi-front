package com.costumi.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentSplashBinding
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.irALogin
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** Decide el destino inicial: consulta /auth/me y enruta por rol, o va a Login. */
@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val vm: SplashViewModel by viewModels()
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSplashBinding.bind(view)

        observar(vm.destino) { destino ->
            when (destino) {
                is DestinoSplash.Home -> findNavController().irAHome(destino.modo)
                DestinoSplash.Login -> findNavController().irALogin()
            }
        }
        observar(vm.error) { mensaje ->
            if (mensaje == null) return@observar
            binding.contenidoCarga.isVisible = false
            binding.stateView.error(mensaje) {
                binding.stateView.ocultar()
                binding.contenidoCarga.isVisible = true
                vm.evaluar()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
