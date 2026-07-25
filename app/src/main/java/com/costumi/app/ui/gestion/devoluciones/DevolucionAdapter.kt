package com.costumi.app.ui.gestion.devoluciones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemDevolucionBinding
import com.costumi.apiclient.models.DevolucionResponse

/** Historial de devoluciones: cargos, remanente devuelto y multa (si la hubo). */
class DevolucionAdapter(
    private val alTocar: (DevolucionResponse) -> Unit,
) : ListAdapter<DevolucionResponse, DevolucionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDevolucionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.enlazar(item)
        holder.itemView.setOnClickListener { alTocar(item) }
    }

    class VH(private val binding: ItemDevolucionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(d: DevolucionResponse) {
            val piezas = d.piezas?.size ?: 0
            val pendientes = d.piezas?.count { it.resuelta == false } ?: 0
            binding.titulo.text = "$piezas pieza(s) revisadas"
            binding.detalle.text = buildString {
                append("Deposito ${d.deposito.comoPrecio() ?: "$0"}")
                d.cargoPorDanos?.takeIf { it.signum() > 0 }?.let { append(" · danos ${it.comoPrecio()}") }
                d.cargoPorRetraso?.takeIf { it.signum() > 0 }?.let { append(" · retraso ${it.comoPrecio()}") }
                if (pendientes > 0) append(" · $pendientes pendiente(s)")
            }
            binding.remanente.text = "Remanente ${d.remanente.comoPrecio() ?: "$0"}"
            val multa = d.multa?.takeIf { it.signum() > 0 }
            binding.multa.isVisible = multa != null
            binding.multa.text = multa?.let { "Multa ${it.comoPrecio()}" }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DevolucionResponse>() {
            override fun areItemsTheSame(a: DevolucionResponse, b: DevolucionResponse) = a.id == b.id
            override fun areContentsTheSame(a: DevolucionResponse, b: DevolucionResponse) = a == b
        }
    }
}
