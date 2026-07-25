package com.costumi.app.ui.gestion.empleados

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemPermisoBinding

/** Fila de la matriz: una sección con sus switches Ver / Operar. */
class PermisoAdapter(
    private val alCambiar: (seccion: PermisoSeccion, esVer: Boolean, concedido: Boolean) -> Unit,
) : ListAdapter<PermisoSeccion, PermisoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPermisoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemPermisoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(p: PermisoSeccion) {
            binding.seccion.text = p.nombre
            // Quitar listeners antes de setChecked para no disparar la llamada al reciclar.
            binding.switchVer.setOnCheckedChangeListener(null)
            binding.switchAccion.setOnCheckedChangeListener(null)
            binding.switchVer.isChecked = p.ver
            binding.switchAccion.isChecked = p.accion
            binding.switchVer.setOnCheckedChangeListener { _, isChecked -> alCambiar(p, true, isChecked) }
            binding.switchAccion.setOnCheckedChangeListener { _, isChecked -> alCambiar(p, false, isChecked) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PermisoSeccion>() {
            override fun areItemsTheSame(a: PermisoSeccion, b: PermisoSeccion) = a.seccion == b.seccion
            override fun areContentsTheSame(a: PermisoSeccion, b: PermisoSeccion) = a == b
        }
    }
}
