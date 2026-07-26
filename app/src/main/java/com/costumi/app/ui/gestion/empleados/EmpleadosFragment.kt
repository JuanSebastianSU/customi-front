package com.costumi.app.ui.gestion.empleados

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.common.comoDiaMes
import com.costumi.app.databinding.DialogAltaEmpleadoBinding
import com.costumi.app.databinding.DialogRolBinding
import com.costumi.app.databinding.FragmentEmpleadosBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.InvitacionPendienteResponse
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
        binding.toolbar.menu.add("Invitaciones pendientes").setOnMenuItemClickListener {
            vm.verPendientes(); true
        }
        binding.lista.adapter = adapter
        binding.fabNuevo.setOnClickListener { dialogoAlta() }

        binding.botonFiltroRol.setOnClickListener { menuFiltroRol(it) }
        binding.botonFiltroSucursal.setOnClickListener { menuFiltroSucursal(it) }

        observar(vm.estado) { estado ->
            // Si un filtro (rol/sucursal) deja la lista vacia, limpiar las filas previas: el StateView es
            // transparente y si no se veian debajo del mensaje de vacio.
            if (estado !is UiState.Success) adapter.submitList(emptyList())
            binding.stateView.mostrar(estado, vacio = "No hay empleados con ese filtro.") {
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
                is EventoEmpleado.Pendientes -> mostrarPendientes(evento.invitaciones)
            }
        }
    }

    /** Filtro por rol: "Todos" + los roles operativos. Actualiza el texto del boton y filtra en la app. */
    private fun menuFiltroRol(ancla: View) {
        android.widget.PopupMenu(requireContext(), ancla).apply {
            menu.add(0, 0, 0, "Todos")
            roles.forEachIndexed { i, (label, _) -> menu.add(0, i + 1, i + 1, label) }
            setOnMenuItemClickListener { item ->
                val sel = if (item.itemId == 0) null else roles[item.itemId - 1]
                vm.filtrarRol(sel?.second)
                binding.botonFiltroRol.text = "Rol: ${sel?.first ?: "Todos"}"
                true
            }
            show()
        }
    }

    /** Filtro por sucursal: "Todas" + cada sucursal de la empresa. */
    private fun menuFiltroSucursal(ancla: View) {
        if (sucursales.isEmpty()) { mostrarMensaje("Aun no se cargaron las sucursales."); return }
        android.widget.PopupMenu(requireContext(), ancla).apply {
            menu.add(0, 0, 0, "Todas")
            sucursales.forEachIndexed { i, s -> menu.add(0, i + 1, i + 1, s.nombre.orEmpty()) }
            setOnMenuItemClickListener { item ->
                val sel = if (item.itemId == 0) null else sucursales[item.itemId - 1]
                vm.filtrarSucursal(sel?.id)
                binding.botonFiltroSucursal.text = "Sucursal: ${sel?.nombre ?: "Todas"}"
                true
            }
            show()
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

    /** Lista las invitaciones pendientes; tocar una ofrece cancelarla. */
    private fun mostrarPendientes(invs: List<InvitacionPendienteResponse>) {
        if (invs.isEmpty()) {
            mostrarMensaje("No hay invitaciones pendientes.")
            return
        }
        val etiquetas = invs.map {
            val vence = it.expiraEn?.toLocalDate()?.let { d -> " · vence ${d.comoDiaMes()}" }.orEmpty()
            "${it.email.orEmpty()} — ${it.rol.orEmpty()}$vence"
        }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Invitaciones pendientes")
            .setItems(etiquetas) { _, i ->
                val inv = invs[i]
                val id = inv.id ?: return@setItems
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(inv.email.orEmpty())
                    .setMessage("¿Reenviar el enlace de invitación (por si el correo no llegó) o cancelarla?")
                    // El positivo es la acción constructiva (reenviar); cancelar va como neutral/destructivo.
                    .setPositiveButton("Reenviar") { _, _ -> vm.reenviarInvitacion(id, inv.email.orEmpty()) }
                    .setNeutralButton("Cancelar invitación") { _, _ -> vm.cancelarInvitacion(id) }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
            .setNegativeButton("Cerrar", null)
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
            AccionEmpleado.SUSPENDER -> vm.suspender(id)
            AccionEmpleado.REACTIVAR_MEMBRESIA -> vm.reactivarMembresia(id)
            AccionEmpleado.QUITAR -> confirmarQuitar(id, empleado.email.orEmpty())
        }
    }

    private fun confirmarQuitar(id: UUID, email: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Dar de baja a $email?")
            .setMessage("Dejará de trabajar en la tienda (sigue siendo cliente). Para volver, habrá que re-invitarlo.")
            .setPositiveButton("Dar de baja") { _, _ -> vm.quitar(id) }
            .setNegativeButton("Cancelar", null)
            .show()
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
