package com.costumi.app.ui.superadmin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.databinding.FragmentSuperadminHomeBinding
import com.costumi.app.ui.SesionViewModel
import com.costumi.app.ui.irALogin
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** Panel SUPERADMIN: solicitudes de tienda (aprobar/rechazar) + empresas (suspender/reactivar). */
@AndroidEntryPoint
class SuperAdminHomeFragment : Fragment(R.layout.fragment_superadmin_home) {

    private val vm: SuperAdminViewModel by viewModels()
    private val sesion: SesionViewModel by viewModels()
    private var _binding: FragmentSuperadminHomeBinding? = null
    private val binding get() = _binding!!
    private val adapter = PanelSuperAdminAdapter(
        onAprobar = { confirmarSolicitud(it, aprobar = true) },
        onRechazar = { confirmarSolicitud(it, aprobar = false) },
        onSuspender = { confirmarEmpresa(it, suspender = true) },
        onReactivar = { confirmarEmpresa(it, suspender = false) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSuperadminHomeBinding.bind(view)
        binding.toolbar.inflateMenu(R.menu.menu_superadmin)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accionLogout) { sesion.cerrarSesion(); true } else false
        }
        binding.lista.adapter = adapter
        binding.swipe.setOnRefreshListener { vm.cargar() }

        observar(vm.estado) { estado ->
            binding.swipe.isRefreshing = estado is UiState.Loading && adapter.itemCount > 0
            binding.stateView.mostrar(estado, vacio = "No hay solicitudes ni empresas.") {
                adapter.submitList(it)
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoSuperAdmin.Info -> mostrarMensaje(evento.mensaje)
                is EventoSuperAdmin.Error -> mostrarMensaje(evento.mensaje)
            }
        }
        observar(sesion.cerrada) { findNavController().irALogin() }
    }

    private fun confirmarSolicitud(e: EmpresaPendienteResponse, aprobar: Boolean) {
        val id = e.id ?: return
        val nombre = e.nombre.orEmpty()
        val (titulo, cuerpo, accion) = if (aprobar) {
            Triple("Aprobar tienda?", "Se aprobara \"$nombre\": el solicitante pasa a Dueno y se crea su Casa Matriz.", "Aprobar")
        } else {
            Triple("Rechazar tienda?", "Se rechazara la solicitud de \"$nombre\".", "Rechazar")
        }
        confirmar(titulo, cuerpo, accion) {
            if (aprobar) vm.aprobar(id, nombre) else vm.rechazar(id, nombre)
        }
    }

    private fun confirmarEmpresa(e: EmpresaResumenResponse, suspender: Boolean) {
        val id = e.id ?: return
        val nombre = e.nombre.orEmpty()
        val (titulo, cuerpo, accion) = if (suspender) {
            Triple("Suspender empresa?", "\"$nombre\" no podra operar hasta reactivarla.", "Suspender")
        } else {
            Triple("Reactivar empresa?", "\"$nombre\" volvera a operar normalmente.", "Reactivar")
        }
        confirmar(titulo, cuerpo, accion) {
            if (suspender) vm.suspender(id, nombre) else vm.reactivar(id, nombre)
        }
    }

    private fun confirmar(titulo: String, cuerpo: String, accion: String, alConfirmar: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(cuerpo)
            .setPositiveButton(accion) { _, _ -> alConfirmar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
