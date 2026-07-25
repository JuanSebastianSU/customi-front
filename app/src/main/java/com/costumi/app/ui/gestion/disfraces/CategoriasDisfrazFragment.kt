package com.costumi.app.ui.gestion.disfraces

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
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Categorías de DISFRAZ (taxonomía propia): alta, renombrar y archivar/activar. Reusa el layout genérico. */
@AndroidEntryPoint
class CategoriasDisfrazFragment : Fragment(R.layout.fragment_categorias) {

    private val vm: CategoriasDisfrazViewModel by viewModels()
    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!

    private val adapter = CategoriaDisfrazAdapter(
        alRenombrar = { dialogoRenombrar(it) },
        alAlternarArchivado = { alternar(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCategoriasBinding.bind(view)
        binding.toolbar.title = "Categorias de disfraz"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.text = "Nueva categoria"
        binding.fabNueva.setOnClickListener { dialogoCrear() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay categorias de disfraz. Crea la primera con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCategoriaDisfraz.Info -> mostrarMensaje(evento.mensaje)
                is EventoCategoriaDisfraz.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun alternar(cat: CategoriaDeDisfrazResponse) {
        val id = cat.id ?: return
        if (cat.archivada == true) vm.activar(id) else confirmarArchivar(cat)
    }

    private fun dialogoCrear() {
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Nombre de la categoria"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva categoria de disfraz")
            .setView(d.root)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = d.editTexto.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) mostrarMensaje("Ingresa un nombre") else vm.crear(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoRenombrar(cat: CategoriaDeDisfrazResponse) {
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

    private fun confirmarArchivar(cat: CategoriaDeDisfrazResponse) {
        val id = cat.id ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archivar \"${cat.nombre}\"?")
            .setMessage("Archivarla la oculta al clasificar disfraces. Podras reactivarla luego.")
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
