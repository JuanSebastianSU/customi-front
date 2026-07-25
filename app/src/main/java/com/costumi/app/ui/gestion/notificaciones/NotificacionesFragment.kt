package com.costumi.app.ui.gestion.notificaciones

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.costumi.app.R
import com.costumi.app.databinding.DialogEnviarNotificacionBinding
import com.costumi.app.databinding.FragmentNotificacionesBinding
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.EnviarNotificacionRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Notificaciones: bandeja paginada de emitidas, envío manual y recordatorios. */
@AndroidEntryPoint
class NotificacionesFragment : Fragment(R.layout.fragment_notificaciones) {

    private val vm: NotificacionesViewModel by viewModels()
    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!
    private var clientes: List<ClienteResponse> = emptyList()
    private val adapter = NotificacionAdapter { id -> clientes.firstOrNull { it.id == id }?.nombre ?: "Cliente" }

    private val canales = listOf(
        "Aviso interno (app)" to EnviarNotificacionRequest.Canal.IN_APP,
        "WhatsApp" to EnviarNotificacionRequest.Canal.WHATSAPP,
        "Email" to EnviarNotificacionRequest.Canal.EMAIL,
        "Push (FCM)" to EnviarNotificacionRequest.Canal.FCM,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNotificacionesBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar en el mensaje"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        // El menú de un toolbar suelto hay que inflarlo por código.
        binding.toolbar.inflateMenu(R.menu.menu_notificaciones)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.accionRecordar -> { confirmarRecordar(); true }
                R.id.accionRecordarProximas -> { vm.recordarProximas(); true }
                R.id.accionStockBajo -> { vm.avisarStockBajo(); true }
                else -> false
            }
        }
        binding.lista.adapter = adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })
        binding.fabEnviar.setOnClickListener { dialogoEnviar() }

        observar(vm.notificaciones) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }
        observar(vm.clientes) { subs ->
            clientes = subs
            // Los nombres de destinatario se resuelven contra esta lista: al llegar, repintar lo visible.
            if (adapter.itemCount > 0) adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoNotif.Info -> { mostrarMensaje(evento.mensaje); adapter.refresh() }
                is EventoNotif.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar las notificaciones.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) binding.stateView.vacio("No hay notificaciones emitidas.")
                else binding.stateView.ocultar()
        }
    }

    private fun dialogoEnviar() {
        val d = DialogEnviarNotificacionBinding.inflate(layoutInflater)
        d.dropCanal.setSimpleItems(canales.map { it.first }.toTypedArray())
        d.dropCanal.setText(canales.first().first, false)

        val opcionesCliente = listOf("Sin cliente (general)") + clientes.map { it.nombre.orEmpty() }
        d.dropCliente.setSimpleItems(opcionesCliente.toTypedArray())
        d.dropCliente.setText(opcionesCliente.first(), false)
        var clienteSel: UUID? = null
        d.dropCliente.setOnItemClickListener { _, _, pos, _ ->
            clienteSel = if (pos == 0) null else clientes.getOrNull(pos - 1)?.id
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enviar notificacion")
            .setView(d.root)
            .setPositiveButton("Enviar") { _, _ ->
                val mensaje = d.editMensaje.text?.toString()?.trim().orEmpty()
                if (mensaje.isBlank()) { mostrarMensaje("El mensaje es obligatorio"); return@setPositiveButton }
                val canal = canales.firstOrNull { it.first == d.dropCanal.text?.toString() }?.second ?: canales.first().second
                vm.enviar(canal, clienteSel, mensaje)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarRecordar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recordar vencidas?")
            .setMessage("Se enviara un recordatorio a los clientes con rentas vencidas.")
            .setPositiveButton("Enviar recordatorios") { _, _ -> vm.recordarVencidas() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
