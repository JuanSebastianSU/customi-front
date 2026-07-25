package com.costumi.app.ui.gestion.taxonomia

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogTextoBinding
import com.costumi.app.databinding.FragmentValoresBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ValorEtiquetaResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Taxonomía — Valores de un tipo de etiqueta: alta, renombrar y archivar/activar (con conteo). */
@AndroidEntryPoint
class ValoresFragment : Fragment(R.layout.fragment_valores) {

    private val vm: ValoresViewModel by viewModels()
    private var _binding: FragmentValoresBinding? = null
    private val binding get() = _binding!!

    private val adapter = ValorEtiquetaAdapter(
        alRenombrar = { dialogoRenombrar(it) },
        alAlternarArchivado = { alternar(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentValoresBinding.bind(view)
        binding.toolbar.title = vm.tipoNombre
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { dialogoCrear() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Este tipo no tiene valores. Agrega el primero con +.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoValor.Info -> mostrarMensaje(evento.mensaje)
                is EventoValor.Error -> mostrarMensaje(evento.mensaje)
                is EventoValor.ConfirmarArchivar -> confirmarArchivar(evento.valor, evento.prendas)
            }
        }
    }

    private fun alternar(valor: ValorEtiquetaResponse) {
        val id = valor.id ?: return
        if (valor.archivada == true) vm.activar(id) else vm.solicitarArchivar(valor)
    }

    private fun dialogoCrear() {
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Valor (ej. Rojo, M, XL)"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo valor")
            .setView(d.root)
            .setPositiveButton("Agregar") { _, _ ->
                val valor = d.editTexto.text?.toString()?.trim().orEmpty()
                if (valor.isBlank()) mostrarMensaje("Ingresa un valor") else vm.crear(valor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoRenombrar(valor: ValorEtiquetaResponse) {
        val id = valor.id ?: return
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Nuevo valor"
        d.editTexto.setText(valor.valor.orEmpty())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Renombrar valor")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = d.editTexto.text?.toString()?.trim().orEmpty()
                if (nuevo.isBlank()) mostrarMensaje("Ingresa un valor") else vm.renombrar(id, nuevo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarArchivar(valor: ValorEtiquetaResponse, prendas: Int?) {
        val id = valor.id ?: return
        val detalle = when {
            prendas == null -> "No se pudo verificar cuantas prendas lo usan."
            prendas == 0 -> "No hay prendas activas con este valor."
            prendas == 1 -> "1 prenda activa usa este valor."
            else -> "$prendas prendas activas usan este valor."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archivar \"${valor.valor}\"?")
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

    companion object {
        const val ARG_TIPO_ID = "tipoId"
        const val ARG_TIPO_NOMBRE = "tipoNombre"
    }
}
