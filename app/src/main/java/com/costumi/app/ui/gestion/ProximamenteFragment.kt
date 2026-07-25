package com.costumi.app.ui.gestion

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.costumi.app.R
import com.costumi.app.databinding.FragmentProximamenteBinding

/**
 * Placeholder de las secciones de Gestion que aun no se implementan (Inventario, Ventas,
 * Clientes, etc.). Recibe el titulo por argumento `titulo`. Se reemplaza fase por fase.
 */
class ProximamenteFragment : Fragment(R.layout.fragment_proximamente) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentProximamenteBinding.bind(view)
        val titulo = arguments?.getString(ARG_TITULO).orEmpty()
        binding.toolbar.title = titulo
        binding.textoTitulo.text = titulo
    }

    companion object {
        const val ARG_TITULO = "titulo"
    }
}
