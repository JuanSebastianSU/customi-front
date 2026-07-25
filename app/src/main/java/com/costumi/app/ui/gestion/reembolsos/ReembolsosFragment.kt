package com.costumi.app.ui.gestion.reembolsos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.DialogTextoBinding
import com.costumi.app.databinding.FragmentReembolsosBinding
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.gestion.mostrarDesglose
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Bandeja de solicitudes de reembolso: aprobar / rechazar con motivo, y registrar nuevas solicitudes. */
@AndroidEntryPoint
class ReembolsosFragment : Fragment(R.layout.fragment_reembolsos) {

    private val vm: ReembolsosViewModel by viewModels()
    private var _binding: FragmentReembolsosBinding? = null
    private val binding get() = _binding!!
    private val adapter = ReembolsoAdapter(
        alTocar = { vm.verDetalle(it) },
        alDecidir = { solicitud, accion -> decidir(solicitud, accion) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReembolsosBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por motivo"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNueva.setOnClickListener { findNavController().navigate(R.id.solicitarReembolsoFragment) }

        binding.chipsEstado.setOnCheckedStateChangeListener { _, ids ->
            vm.filtrar(
                when (ids.firstOrNull()) {
                    R.id.chipResueltas -> ReembolsosViewModel.Filtro.RESUELTAS
                    R.id.chipTodas -> ReembolsosViewModel.Filtro.TODAS
                    else -> ReembolsosViewModel.Filtro.PENDIENTES
                },
            )
        }

        setFragmentResultListener(SolicitarReembolsoFragment.RESULT_SOLICITADA) { _, _ -> vm.cargar() }

        observar(vm.estado) { estado ->
            val vacio = when (binding.chipsEstado.checkedChipId) {
                R.id.chipResueltas -> "No hay reembolsos resueltos."
                R.id.chipTodas -> "No hay solicitudes de reembolso."
                else -> "No hay reembolsos pendientes. Todo al dia."
            }
            binding.stateView.mostrar(estado, vacio = vacio) { adapter.submitList(it) }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoReembolso.Info -> mostrarMensaje(evento.mensaje)
                is EventoReembolso.Error -> mostrarMensaje(evento.mensaje)
                is EventoReembolso.Detalle -> mostrarDetalle(evento)
            }
        }
    }

    /** Desglose de la operación reembolsada: artículos con foto + monto/estado/motivos del reembolso. */
    private fun mostrarDetalle(e: EventoReembolso.Detalle) {
        val s = e.solicitud
        val tipo = if (s.tipoConcepto?.value == "RENTA") "Renta" else "Venta"
        val lineas = e.articulos.map { a ->
            val precio = a.precio.comoPrecio()
            val detalle = buildString {
                append("Cantidad: ${a.cantidad}")
                if (precio != null) append("  ·  $precio${if (a.porDia) "/dia" else " c/u"}")
            }
            LineaDesglose(fotoUrl = a.fotoUrl, nombre = a.nombre, detalle = detalle, monto = a.subtotal.comoPrecio())
        }
        val pie = buildList {
            add("Reembolso solicitado" to (s.monto.comoPrecio() ?: "-"))
            s.estado?.value?.let { add("Estado" to it) }
            s.motivoSolicitud?.takeIf { it.isNotBlank() }?.let { add("Motivo" to it) }
            s.motivoDecision?.takeIf { it.isNotBlank() }?.let { add("Decision" to it) }
        }
        mostrarDesglose(titulo = "$tipo · reembolso", lineas = lineas, pie = pie)
    }

    private fun decidir(solicitud: SolicitudDeReembolsoResponse, accion: AccionReembolso) {
        val id = solicitud.id ?: return
        if (accion == AccionReembolso.REGISTRAR_DEVOLUCION) {
            registrarDevolucion(solicitud)
            return
        }
        val aprobar = accion == AccionReembolso.APROBAR
        val d = DialogTextoBinding.inflate(layoutInflater)
        d.til.hint = "Motivo (obligatorio)"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (aprobar) "Aprobar reembolso" else "Rechazar solicitud")
            .setView(d.root)
            .setPositiveButton(if (aprobar) "Aprobar" else "Rechazar") { _, _ ->
                val motivo = d.editTexto.text?.toString()?.trim().orEmpty()
                if (motivo.isBlank()) { mostrarMensaje("El motivo es obligatorio"); return@setPositiveButton }
                if (aprobar) vm.aprobar(id, motivo) else vm.rechazar(id, motivo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Registrar la devolucion es la precondicion para aprobar (el backend responde 409 si el item no
     * volvio). Se confirma porque reingresa stock y no se deshace desde aqui.
     */
    private fun registrarDevolucion(solicitud: SolicitudDeReembolsoResponse) {
        val esRenta = solicitud.tipoConcepto?.value == "RENTA"
        val que = if (esRenta) "la renta" else "todas las unidades pendientes de la venta"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar devolucion")
            .setMessage(
                "Se marcara $que como devuelta y el stock volvera al inventario. " +
                    "Despues vas a poder aprobar el reembolso.",
            )
            .setPositiveButton("Registrar") { _, _ -> vm.registrarDevolucion(solicitud) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
