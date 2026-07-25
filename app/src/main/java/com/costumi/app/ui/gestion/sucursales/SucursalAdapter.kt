package com.costumi.app.ui.gestion.sucursales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemSucursalBinding
import com.costumi.apiclient.models.SucursalResponse

/** Acción sobre una sucursal. */
enum class AccionSucursal { EDITAR, ARCHIVAR, ACTIVAR }

/** Lista de sucursales: nombre, dirección, estado y menú (editar/archivar/activar). */
class SucursalAdapter(
    private val alAccionar: (SucursalResponse, AccionSucursal) -> Unit,
) : ListAdapter<SucursalResponse, SucursalAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSucursalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemSucursalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(s: SucursalResponse) {
            binding.nombre.text = s.nombre.orEmpty()
            val dir = s.direccion?.takeIf { it.isNotBlank() }
            binding.direccion.isVisible = dir != null
            binding.direccion.text = dir
            val archivada = s.archivada == true
            binding.chipArchivada.isVisible = archivada
            binding.root.alpha = if (archivada) 0.55f else 1f
            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, s, archivada) }
        }

        private fun mostrarMenu(ancla: View, s: SucursalResponse, archivada: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, 1, 0, "Editar")
                if (archivada) menu.add(0, 3, 1, "Activar") else menu.add(0, 2, 1, "Archivar")
                setOnMenuItemClickListener { item ->
                    val accion = when (item.itemId) {
                        1 -> AccionSucursal.EDITAR
                        2 -> AccionSucursal.ARCHIVAR
                        else -> AccionSucursal.ACTIVAR
                    }
                    alAccionar(s, accion)
                    true
                }
                show()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SucursalResponse>() {
            override fun areItemsTheSame(a: SucursalResponse, b: SucursalResponse) = a.id == b.id
            override fun areContentsTheSame(a: SucursalResponse, b: SucursalResponse) = a == b
        }
    }
}
