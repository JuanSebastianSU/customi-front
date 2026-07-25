package com.costumi.app.ui.cliente.favoritos

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentFavoritosBinding
import com.costumi.app.ui.cliente.detalle.DetalleDisfrazFragment
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** "Mis guardados": disfraces que el cliente marcó con el corazón. */
@AndroidEntryPoint
class FavoritosFragment : Fragment(R.layout.fragment_favoritos) {

    private val vm: FavoritosViewModel by viewModels()
    private var _binding: FragmentFavoritosBinding? = null
    private val binding get() = _binding!!
    private val adapter = FavoritoAdapter(
        alTocar = { fav ->
            findNavController().navigate(
                R.id.detalleDisfrazFragment,
                bundleOf(
                    DetalleDisfrazFragment.ARG_EMPRESA_ID to fav.empresaId,
                    DetalleDisfrazFragment.ARG_DISFRAZ_ID to fav.disfrazId,
                    DetalleDisfrazFragment.ARG_NOMBRE to fav.nombre,
                ),
            )
        },
        alQuitar = { fav -> vm.quitar(fav) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentFavoritosBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        observar(vm.favoritos) { favoritos ->
            adapter.submitList(favoritos)
            if (favoritos.isEmpty()) {
                binding.stateView.vacio("Todavia no guardaste disfraces. Tocá el corazón en un disfraz para guardarlo.")
            } else {
                binding.stateView.ocultar()
            }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
