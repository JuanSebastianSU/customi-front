package com.costumi.app.ui.gestion.taxonomia

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogTextoBinding
import com.costumi.app.databinding.DialogTipoEtiquetaBinding
import com.costumi.app.databinding.FragmentTiposEtiquetaBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Taxonomía — Tipos de etiqueta: alta (con flags), renombrar, archivar/activar y navegación a valores. */
@AndroidEntryPoint
class TiposEtiquetaFragment : Fragment(R.layout.fragment_tipos_etiqueta) {

    private val vm: TiposEtiquetaViewModel by viewModels()
    private var _binding: FragmentTiposEtiquetaBinding? = null
    private val binding get() = _binding!!

    private val adapter = TipoEtiquetaAdapter(
        alAbrirValores = { abrirValores(it) },
        alRenombrar = { dialogoRenombrar(it) },
        alAlternarArchivado = { alternar(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTiposEtiquetaBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { dialogoCrear() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay tipos de etiqueta. Crea el primero con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoTipo.Info -> mostrarMensaje(evento.mensaje)
                is EventoTipo.Error -> mostrarMensaje(evento.mensaje)
                is EventoTipo.ConfirmarArchivar -> confirmarArchivar(evento.tipo, evento.prendas)
            }
        }
    }

    private fun abrirValores(tipo: TipoEtiquetaResponse) {
        val id = tipo.id ?: return
        findNavController().navigate(
            R.id.valoresFragment,
            bundleOf(
                ValoresFragment.ARG_TIPO_ID to id.toString(),
                ValoresFragment.ARG_TIPO_NOMBRE to tipo.nombre,
            ),
        )
    }

    private fun alternar(tipo: TipoEtiquetaResponse) {
        val id = tipo.id ?: return
        if (tipo.archivada == true) vm.activar(id) else vm.solicitarArchivar(tipo)
    }

    private fun dialogoCrear() {
        val d = DialogTipoEtiquetaBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo tipo de etiqueta")
            .setView(d.root)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = d.editNombre.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) mostrarMensaje("Ingresa un nombre")
                else vm.crear(nombre, d.switchVariante.isChecked, d.switchCliente.isChecked)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoRenombrar(tipo: TipoEtiquetaResponse) {
        val id = tipo.id ?: return
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Nuevo nombre"
        d.editTexto.setText(tipo.nombre.orEmpty())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Renombrar tipo")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = d.editTexto.text?.toString()?.trim().orEmpty()
                if (nombre.isBlank()) mostrarMensaje("Ingresa un nombre") else vm.renombrar(id, nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarArchivar(tipo: TipoEtiquetaResponse, prendas: Int?) {
        val id = tipo.id ?: return
        val detalle = when {
            prendas == null -> "No se pudo verificar cuantas prendas lo usan."
            prendas == 0 -> "No hay prendas activas con este tipo."
            prendas == 1 -> "1 prenda activa usa este tipo."
            else -> "$prendas prendas activas usan este tipo."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archivar \"${tipo.nombre}\"?")
            .setMessage("$detalle\n\nPodras reactivarlo luego.")
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
