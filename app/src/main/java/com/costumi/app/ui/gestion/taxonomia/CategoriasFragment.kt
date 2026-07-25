package com.costumi.app.ui.gestion.taxonomia

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogTextoBinding
import com.costumi.app.databinding.FragmentCategoriasBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.CategoriaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Taxonomía — Categorías: alta, renombrar y archivar/activar (con conteo de prendas). */
@AndroidEntryPoint
class CategoriasFragment : Fragment(R.layout.fragment_categorias) {

    private val vm: CategoriasViewModel by viewModels()
    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!

    private val adapter = CategoriaAdapter(
        alRenombrar = { dialogoRenombrar(it) },
        alAlternarArchivado = { alternar(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCategoriasBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { dialogoCrear() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay categorias. Crea la primera con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCategoria.Info -> mostrarMensaje(evento.mensaje)
                is EventoCategoria.Error -> mostrarMensaje(evento.mensaje)
                is EventoCategoria.ConfirmarArchivar -> confirmarArchivar(evento.categoria, evento.prendas)
            }
        }
    }

    private fun alternar(cat: CategoriaResponse) {
        val id = cat.id ?: return
        if (cat.archivada == true) vm.activar(id) else vm.solicitarArchivar(cat)
    }

    private fun dialogoCrear() {
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Nombre de la categoria"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva categoria")
            .setView(d.root)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = d.editTexto.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) mostrarMensaje("Ingresa un nombre") else vm.crear(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoRenombrar(cat: CategoriaResponse) {
        val id = cat.id ?: return
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Nuevo nombre"
        d.editTexto.setText(cat.nombre.orEmpty())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Renombrar categoria")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = d.editTexto.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) mostrarMensaje("Ingresa un nombre") else vm.renombrar(id, nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarArchivar(cat: CategoriaResponse, prendas: Int?) {
        val id = cat.id ?: return
        val detalle = when {
            prendas == null -> "No se pudo verificar cuantas prendas la usan."
            prendas == 0 -> "No hay prendas activas en esta categoria."
            prendas == 1 -> "1 prenda activa usa esta categoria."
            else -> "$prendas prendas activas usan esta categoria."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archivar \"${cat.nombre}\"?")
            .setMessage("$detalle\n\nArchivar la oculta del alta de prendas. Podras reactivarla luego.")
            .setPositiveButton("Archivar") { _, _ -> vm.archivar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
