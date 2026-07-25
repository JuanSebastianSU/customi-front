package com.costumi.app.ui.gestion.empleados

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.DialogAltaEmpleadoBinding
import com.costumi.app.databinding.DialogRolBinding
import com.costumi.app.databinding.FragmentEmpleadosBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.InvitarEmpleadoRequest
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.EmpleadoDetalleResponse
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Gestión de personal: lista, alta, cambiar rol, baja/reactivación y asignar sucursales. */
@AndroidEntryPoint
class EmpleadosFragment : Fragment(R.layout.fragment_empleados) {

    private val vm: EmpleadosViewModel by viewModels()
    private var _binding: FragmentEmpleadosBinding? = null
    private val binding get() = _binding!!
    private var sucursales: List<SucursalResponse> = emptyList()
    private var ultimaLista: List<EmpleadoDetalleResponse> = emptyList()
    private val adapter = EmpleadoAdapter { empleado, accion -> accionar(empleado, accion) }

    // Roles operativos que gestiona un DUENO/ENCARGADO (la pirámide la valida el backend).
    private val roles = listOf("Encargado" to "ENCARGADO", "Mostrador" to "MOSTRADOR", "Bodega" to "BODEGA", "Atencion" to "ATENCION")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEmpleadosBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por correo"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.fabNuevo.setOnClickListener { dialogoAlta() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay empleados. Da de alta al primero.") {
                ultimaLista = it
                adapter.submitList(it)
            }
        }
        observar(vm.sucursales) { lista ->
            sucursales = lista
            adapter.nombresDeSucursal = lista.mapNotNull { s -> s.id?.let { it to s.nombre.orEmpty() } }.toMap()
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoEmpleado.Info -> mostrarMensaje(evento.mensaje)
                is EventoEmpleado.Error -> mostrarMensaje(evento.mensaje)
                is EventoEmpleado.Actividad -> mostrarActividad(evento)
                is EventoEmpleado.Invitacion -> mostrarInvitacion(evento)
            }
        }
    }

    /** Muestra el enlace de invitación para compartir (copiar al portapapeles). */
    private fun mostrarInvitacion(inv: EventoEmpleado.Invitacion) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Invitación a ${inv.email}")
            .setMessage("Compartí este enlace para que acepte y se una:\n\n${inv.enlace}")
            .setPositiveButton("Copiar enlace") { _, _ ->
                val cb = requireContext().getSystemService(android.content.ClipboardManager::class.java)
                cb?.setPrimaryClip(android.content.ClipData.newPlainText("Invitación Costumi", inv.enlace))
                mostrarMensaje("Enlace copiado")
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarActividad(a: EventoEmpleado.Actividad) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actividad de ${a.email}")
            .setMessage("Ventas realizadas: ${a.ventas}\nTotal vendido: ${a.total.comoPrecio() ?: "$0"}")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun accionar(empleado: EmpleadoDetalleResponse, accion: AccionEmpleado) {
        val id = empleado.id ?: return
        when (accion) {
            AccionEmpleado.ROL -> dialogoRol(empleado)
            AccionEmpleado.SUCURSALES -> dialogoSucursales(empleado)
            AccionEmpleado.PERMISOS -> findNavController().navigate(
                R.id.permisosEmpleadoFragment,
                bundleOf(
                    PermisosEmpleadoFragment.ARG_ID to id.toString(),
                    PermisosEmpleadoFragment.ARG_EMAIL to empleado.email,
                ),
            )
            AccionEmpleado.ACTIVIDAD -> vm.verActividad(id, empleado.email.orEmpty())
            AccionEmpleado.ACTIVAR -> vm.activar(id)
            AccionEmpleado.DESACTIVAR -> confirmarDesactivar(id, empleado.email.orEmpty())
        }
    }

    private fun dialogoAlta() {
        val d = DialogAltaEmpleadoBinding.inflate(layoutInflater)
        d.dropRol.setSimpleItems(roles.map { it.first }.toTypedArray())
        d.dropRol.setText(roles.first().first, false)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Invitar empleado")
            .setView(d.root)
            .setPositiveButton("Invitar") { _, _ ->
                val email = d.editEmail.text?.toString()?.trim().orEmpty()
                if (email.isBlank() || !email.contains("@")) { mostrarMensaje("Correo invalido"); return@setPositiveButton }
                val rol = roles.firstOrNull { it.first == d.dropRol.text?.toString() }?.second ?: roles.first().second
                vm.invitar(email, InvitarEmpleadoRequest.Rol.valueOf(rol))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoRol(empleado: EmpleadoDetalleResponse) {
        val id = empleado.id ?: return
        val d = DialogRolBinding.inflate(layoutInflater)
        d.dropRol.setSimpleItems(roles.map { it.first }.toTypedArray())
        val actual = roles.firstOrNull { it.second == empleado.rol }?.first ?: roles.first().first
        d.dropRol.setText(actual, false)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar rol de ${empleado.email.orEmpty()}")
            .setView(d.root)
            .setPositiveButton("Guardar") { _, _ ->
                val rol = roles.firstOrNull { it.first == d.dropRol.text?.toString() }?.second ?: roles.first().second
                vm.cambiarRol(id, CambiarRolRequest.Rol.valueOf(rol))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoSucursales(empleado: EmpleadoDetalleResponse) {
        val id = empleado.id ?: return
        if (sucursales.isEmpty()) { mostrarMensaje("No hay sucursales disponibles."); return }
        val nombres = sucursales.map { it.nombre.orEmpty() }.toTypedArray()
        val asignadas = empleado.sucursales.orEmpty().toSet()
        val marcadas = sucursales.map { it.id in asignadas }.toBooleanArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sucursales de ${empleado.email.orEmpty()}")
            .setMultiChoiceItems(nombres, marcadas) { _, which, isChecked -> marcadas[which] = isChecked }
            .setPositiveButton("Asignar") { _, _ ->
                val ids = sucursales.filterIndexed { i, _ -> marcadas[i] }.mapNotNull { it.id }
                vm.asignarSucursales(id, ids)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarDesactivar(id: UUID, email: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Desactivar empleado?")
            .setMessage("$email no podra iniciar sesion hasta reactivarlo.")
            .setPositiveButton("Desactivar") { _, _ -> vm.desactivar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
