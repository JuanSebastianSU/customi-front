package com.costumi.app.ui.gestion.caja

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.DialogAbrirTurnoBinding
import com.costumi.app.databinding.FragmentCajaBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TurnoResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Caja / Turno: lista de turnos (abiertos y cerrados) y apertura de un nuevo turno. */
@AndroidEntryPoint
class CajaFragment : Fragment(R.layout.fragment_caja) {

    private val vm: CajaViewModel by viewModels()
    private var _binding: FragmentCajaBinding? = null
    private val binding get() = _binding!!
    private var sucursales: List<SucursalResponse> = emptyList()
    private var ultimaLista: List<TurnoResponse> = emptyList()
    private val adapter = TurnoAdapter(
        nombreSucursal = { id -> sucursales.firstOrNull { it.id == id }?.nombre ?: "Turno de caja" },
        alElegir = { abrirDetalle(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCajaBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabAbrir.setOnClickListener { dialogoAbrir() }

        setFragmentResultListener(TurnoDetalleFragment.RESULT_CAMBIO) { _, _ -> vm.cargar() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay turnos. Abri el primero con el boton.") {
                ultimaLista = it
                adapter.submitList(it)
            }
        }
        observar(vm.sucursales) { subs ->
            sucursales = subs
            // Reponer nombres una vez llegan las sucursales.
            if (ultimaLista.isNotEmpty()) adapter.notifyDataSetChanged()
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCaja.Info -> mostrarMensaje(evento.mensaje)
                is EventoCaja.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun abrirDetalle(turno: TurnoResponse) {
        val id = turno.id ?: return
        findNavController().navigate(
            R.id.turnoDetalleFragment,
            TurnoDetalleFragment.args(id, sucursales.firstOrNull { it.id == turno.sucursalId }?.nombre),
        )
    }

    private fun dialogoAbrir() {
        if (sucursales.isEmpty()) {
            mostrarMensaje("No hay sucursales disponibles.")
            return
        }
        val d = DialogAbrirTurnoBinding.inflate(layoutInflater)
        d.dropSucursal.setSimpleItems(sucursales.map { it.nombre.orEmpty() }.toTypedArray())
        var seleccionada: SucursalResponse? = sucursales.singleOrNull()
        seleccionada?.let { d.dropSucursal.setText(it.nombre.orEmpty(), false) }
        d.dropSucursal.setOnItemClickListener { _, _, pos, _ -> seleccionada = sucursales.getOrNull(pos) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Abrir turno")
            .setView(d.root)
            .setPositiveButton("Abrir") { _, _ ->
                val sucursal = seleccionada?.id
                if (sucursal == null) { mostrarMensaje("Selecciona una sucursal"); return@setPositiveButton }
                val fondo = d.editFondo.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
                if (fondo == null || fondo.signum() < 0) { mostrarMensaje("Fondo invalido"); return@setPositiveButton }
                vm.abrir(sucursal, fondo)
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
