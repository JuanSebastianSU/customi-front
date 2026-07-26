package com.costumi.app.ui.gestion.empleados

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemEmpleadoBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.EmpleadoDetalleResponse

/** Acción sobre un empleado. */
enum class AccionEmpleado { ROL, SUCURSALES, PERMISOS, ACTIVIDAD, ACTIVAR, DESACTIVAR, SUSPENDER, REACTIVAR_MEMBRESIA, QUITAR }

/** Lista de personal: email, rol, estado, sucursales asignadas y menú de gestión. */
class EmpleadoAdapter(
    private val alAccionar: (EmpleadoDetalleResponse, AccionEmpleado) -> Unit,
) : ListAdapter<EmpleadoDetalleResponse, EmpleadoAdapter.VH>(DIFF) {

    /**
     * Nombre de cada sucursal por id. El listado de personal solo trae los ids, asi que sin esto la fila
     * puede decir "2 sucursal(es)" pero no CUALES, que es justo lo que el dueno necesita saber.
     */
    var nombresDeSucursal: Map<java.util.UUID, String> = emptyMap()
        set(valor) {
            field = valor
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEmpleadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemEmpleadoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(e: EmpleadoDetalleResponse) {
            val email = e.email.orEmpty()
            binding.email.text = email
            binding.inicial.text = email.trim().take(1).uppercase().ifBlank { "?" }

            // Foto del empleado si la tiene; si no, queda la inicial detras.
            val foto = e.fotoUrl?.takeIf { it.isNotBlank() }
            binding.foto.isVisible = foto != null
            if (foto != null) binding.foto.cargarFoto(foto)

            // Rol legible ("MOSTRADOR" → "Mostrador"), como pastilla informativa.
            val rol = e.rol.orEmpty().lowercase().replaceFirstChar { it.uppercase() }
            binding.chipRol.pintarPastilla(rol.ifBlank { "Sin rol" }, Tono.INFO)

            val activo = e.activo != false
            binding.chipEstado.pintarPastilla(
                if (activo) "Activo" else "Desactivado",
                if (activo) Tono.EXITO else Tono.NEUTRO,
            )

            val asignadas = e.sucursales.orEmpty()
            binding.sucursales.text = when {
                asignadas.isEmpty() -> "Sin sucursal asignada"
                // Con muchas no se listan todas: la fila quedaria ilegible.
                asignadas.size > 3 -> "${asignadas.size} sucursales"
                else -> asignadas.joinToString(", ") { nombresDeSucursal[it] ?: "Sucursal" }
            }

            binding.root.alpha = if (activo) 1f else 0.55f
            binding.botonGestionar.setOnClickListener { v -> mostrarMenu(v, e, activo) }
        }

        private fun mostrarMenu(ancla: View, e: EmpleadoDetalleResponse, activo: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, 1, 0, "Cambiar rol")
                menu.add(0, 2, 1, "Asignar sucursales")
                menu.add(0, 5, 2, "Permisos")
                menu.add(0, 6, 3, "Actividad")
                // Membresía de trabajo (Fase B): suspender/reactivar (reversible) o quitar (despido).
                menu.add(0, 7, 4, "Suspender del trabajo")
                menu.add(0, 8, 5, "Reactivar en el trabajo")
                menu.add(0, 9, 6, "Quitar (despedir)")
                if (activo) menu.add(0, 3, 7, "Desactivar cuenta") else menu.add(0, 4, 7, "Reactivar cuenta")
                setOnMenuItemClickListener { item ->
                    val accion = when (item.itemId) {
                        1 -> AccionEmpleado.ROL
                        2 -> AccionEmpleado.SUCURSALES
                        5 -> AccionEmpleado.PERMISOS
                        6 -> AccionEmpleado.ACTIVIDAD
                        7 -> AccionEmpleado.SUSPENDER
                        8 -> AccionEmpleado.REACTIVAR_MEMBRESIA
                        9 -> AccionEmpleado.QUITAR
                        3 -> AccionEmpleado.DESACTIVAR
                        else -> AccionEmpleado.ACTIVAR
                    }
                    alAccionar(e, accion)
                    true
                }
                show()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EmpleadoDetalleResponse>() {
            override fun areItemsTheSame(a: EmpleadoDetalleResponse, b: EmpleadoDetalleResponse) = a.id == b.id
            override fun areContentsTheSame(a: EmpleadoDetalleResponse, b: EmpleadoDetalleResponse) = a == b
        }
    }
}
