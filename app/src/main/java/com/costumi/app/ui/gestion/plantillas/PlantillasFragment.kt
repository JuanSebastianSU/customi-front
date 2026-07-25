package com.costumi.app.ui.gestion.plantillas

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogEditarPlantillaBinding
import com.costumi.app.databinding.FragmentPlantillasBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.PlantillaResponse
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Mensajes automaticos: editar el texto y encender/apagar cada una de las 6 plantillas (RF-11). */
@AndroidEntryPoint
class PlantillasFragment : Fragment(R.layout.fragment_plantillas) {

    private val vm: PlantillasViewModel by viewModels()
    private var _binding: FragmentPlantillasBinding? = null
    private val binding get() = _binding!!
    private val adapter = PlantillaAdapter(
        onEditar = { dialogoEditar(it) },
        onToggle = { p, activa -> vm.cambiarActiva(p.tipo.orEmpty(), p.texto.orEmpty(), activa) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPlantillasBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay plantillas.") { adapter.submitList(it) }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPlantilla.Info -> mostrarMensaje(evento.mensaje)
                is EventoPlantilla.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun dialogoEditar(p: PlantillaResponse) {
        val d = DialogEditarPlantillaBinding.inflate(layoutInflater)
        d.editTexto.setText(p.texto)
        // Vista previa con datos de ejemplo: se ve cómo le llega el mensaje al cliente mientras se edita.
        fun renderPreview() { d.preview.text = CategoriaMensaje.previsualizar(d.editTexto.text?.toString().orEmpty()) }
        d.editTexto.doAfterTextChanged { renderPreview() }
        renderPreview()
        // Chips de variables disponibles: al tocar, inserta {variable} en la posicion del cursor.
        CategoriaMensaje.de(p.tipo).variables.forEach { variable ->
            val chip = Chip(requireContext()).apply {
                text = "{$variable}"
                isCheckable = false
                setOnClickListener {
                    val pos = d.editTexto.selectionStart.coerceAtLeast(0)
                    d.editTexto.text?.insert(pos, "{$variable}")
                }
            }
            d.chipsVariables.addView(chip)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(CategoriaMensaje.de(p.tipo).etiqueta)
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val texto = d.editTexto.text?.toString()?.trim().orEmpty()
                if (texto.isBlank()) { mostrarMensaje("El mensaje no puede quedar vacio"); return@setPositiveButton }
                vm.guardarTexto(p.tipo.orEmpty(), texto, p.activa == true)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
