package com.costumi.app.ui.gestion.devoluciones

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentDevolucionesBinding
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.gestion.mostrarDesglose
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.DevolucionResponse
import dagger.hilt.android.AndroidEntryPoint

/** Historial de devoluciones liquidadas (solo lectura). Se registran desde la pantalla de Rentas. */
@AndroidEntryPoint
class DevolucionesFragment : Fragment(R.layout.fragment_devoluciones) {

    private val vm: DevolucionesViewModel by viewModels()
    private var _binding: FragmentDevolucionesBinding? = null
    private val binding get() = _binding!!
    private val adapter = DevolucionAdapter { mostrarDesgloseDevolucion(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDevolucionesBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por descripcion de una pieza"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(
                estado,
                vacio = "Aun no hay devoluciones. Registralas desde una renta activa.",
            ) { adapter.submitList(it) }
        }
    }

    /** Desglose de la devolución: estado de cada pieza (bien/dañada/perdida/pendiente) y liquidación. */
    private fun mostrarDesgloseDevolucion(u: DevolucionUi) {
        val d = u.dev
        val lineas = d.piezas.orEmpty().map { p ->
            val flags = buildList {
                add(if (p.llego == false) "no llego" else "llego")
                if (p.resuelta == false) add("pendiente")
                if (p.perdidaCobrada == true) add("perdida cobrada")
            }.joinToString(" · ")
            // El nombre de la prenda encabeza; el estado (Bien/Dañada/…) y los flags van en el detalle.
            val detalle = listOfNotNull(estadoLegible(p.estado), p.descripcion?.takeIf { it.isNotBlank() }, flags)
                .joinToString("  ·  ")
            LineaDesglose(fotoUrl = p.fotoUrl, nombre = p.nombre ?: "Pieza", detalle = detalle, monto = null)
        }
        val pie = buildList {
            d.deposito?.let { add("Deposito" to (it.comoPrecio() ?: "-")) }
            d.cargoPorDanos?.takeIf { it.signum() > 0 }?.let { add("Cargo por danos" to (it.comoPrecio() ?: "-")) }
            d.cargoPorRetraso?.takeIf { it.signum() > 0 }?.let { add("Cargo por retraso" to (it.comoPrecio() ?: "-")) }
            d.remanente?.let { add("Remanente devuelto" to (it.comoPrecio() ?: "-")) }
            d.multa?.takeIf { it.signum() > 0 }?.let { add("Multa" to (it.comoPrecio() ?: "-")) }
        }
        val pendientes = d.piezas?.count { it.resuelta == false } ?: 0
        val quien = listOfNotNull(u.clienteNombre?.takeIf { it.isNotBlank() }, u.codigoRetiro).joinToString(" · ")
        mostrarDesglose(
            titulo = "Devolucion" + if (pendientes > 0) " · $pendientes pendiente(s)" else " · liquidada",
            subtitulo = quien.ifBlank { null },
            lineas = lineas,
            pie = pie,
        )
    }

    private fun estadoLegible(estado: String?): String = when (estado?.uppercase()) {
        "BIEN" -> "Bien"
        "DANADA" -> "Danada"
        "EN_LIMPIEZA" -> "En limpieza"
        "PERDIDA" -> "Perdida"
        else -> estado ?: "Pieza"
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
