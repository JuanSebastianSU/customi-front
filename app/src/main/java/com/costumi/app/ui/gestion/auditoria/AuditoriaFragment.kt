package com.costumi.app.ui.gestion.auditoria

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.databinding.FragmentAuditoriaBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** Auditoria: trail de eventos de la empresa (solo lectura). */
@AndroidEntryPoint
class AuditoriaFragment : Fragment(R.layout.fragment_auditoria) {

    private val vm: AuditoriaViewModel by viewModels()
    private var _binding: FragmentAuditoriaBinding? = null
    private val binding get() = _binding!!
    private val adapter = AuditoriaAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAuditoriaBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por accion o detalle"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.swipe.setOnRefreshListener { vm.cargar() }

        observar(vm.estado) { estado ->
            binding.swipe.isRefreshing = estado is UiState.Loading && adapter.itemCount > 0
            binding.stateView.mostrar(estado, vacio = "No hay eventos con este filtro.") {
                adapter.submitList(it)
            }
        }
        observar(vm.categorias) { pintarChips(it) }
    }

    /**
     * Construye los chips de filtro desde las categorías presentes en los datos (más "Todas"). El flujo
     * emite primero una lista vacía (init) y luego la real: por eso se espera a la primera NO vacía, en
     * vez de a la primera emisión (que dejaría solo "Todas").
     */
    private var chipsListos = false
    private fun pintarChips(categorias: List<String>) {
        if (chipsListos || categorias.isEmpty()) return
        chipsListos = true
        agregarChip("Todas", null, marcado = true)
        categorias.forEach { agregarChip(it, it, marcado = false) }
    }

    private fun agregarChip(texto: String, categoria: String?, marcado: Boolean) {
        val chip = com.google.android.material.chip.Chip(requireContext()).apply {
            this.text = texto
            isCheckable = true
            isChecked = marcado
            setEnsureMinTouchTargetSize(false)
            setOnClickListener { vm.filtrar(categoria) }
        }
        binding.chipsCategoria.addView(chip)
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
