package com.costumi.app.ui.gestion.empleados

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemPermisoCapacidadBinding
import com.costumi.app.databinding.ItemPermisoEncabezadoBinding

/**
 * Matriz de permisos (Fase B, paso 5): encabezados de sección + una fila-toggle por capacidad, cada una con
 * su descripción de qué habilita.
 */
class PermisoAdapter(
    private val alCambiar: (clave: String, concedido: Boolean) -> Unit,
) : ListAdapter<PermisoFila, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is PermisoFila.Encabezado -> TIPO_ENCABEZADO
        is PermisoFila.Capacidad -> TIPO_CAPACIDAD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_ENCABEZADO) {
            EncabezadoVH(ItemPermisoEncabezadoBinding.inflate(inflater, parent, false))
        } else {
            CapacidadVH(ItemPermisoCapacidadBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PermisoFila.Encabezado -> (holder as EncabezadoVH).enlazar(item)
            is PermisoFila.Capacidad -> (holder as CapacidadVH).enlazar(item)
        }
    }

    class EncabezadoVH(private val binding: ItemPermisoEncabezadoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(e: PermisoFila.Encabezado) {
            binding.seccion.text = e.nombre
        }
    }

    inner class CapacidadVH(private val binding: ItemPermisoCapacidadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(c: PermisoFila.Capacidad) {
            binding.descripcion.text = c.descripcion
            // Quitar el listener antes de setChecked para no disparar la llamada al reciclar.
            binding.toggle.setOnCheckedChangeListener(null)
            binding.toggle.isChecked = c.concedido
            binding.toggle.setOnCheckedChangeListener { _, isChecked -> alCambiar(c.clave, isChecked) }
        }
    }

    companion object {
        private const val TIPO_ENCABEZADO = 0
        private const val TIPO_CAPACIDAD = 1

        private val DIFF = object : DiffUtil.ItemCallback<PermisoFila>() {
            override fun areItemsTheSame(a: PermisoFila, b: PermisoFila) = a.id == b.id
            override fun areContentsTheSame(a: PermisoFila, b: PermisoFila) = a == b
        }
    }
}
