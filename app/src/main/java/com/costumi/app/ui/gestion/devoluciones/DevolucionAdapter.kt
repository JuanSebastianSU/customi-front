package com.costumi.app.ui.gestion.devoluciones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemDevolucionBinding

/** Historial de devoluciones: de quién es (cliente/código), cargos, remanente devuelto y multa. */
class DevolucionAdapter(
    private val alTocar: (DevolucionUi) -> Unit,
) : ListAdapter<DevolucionUi, DevolucionAdapter.VH>(DIFF) {

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
        fun enlazar(u: DevolucionUi) {
            val d = u.dev
            val piezas = d.piezas?.size ?: 0
            val pendientes = d.piezas?.count { it.resuelta == false } ?: 0
            // El título dice de QUIÉN es: cliente + código de la renta; si no se resolvió, cae al conteo de piezas.
            binding.titulo.text = listOfNotNull(u.clienteNombre?.takeIf { it.isNotBlank() }, u.codigoRetiro)
                .joinToString(" · ").ifBlank { "$piezas pieza(s) revisadas" }
            binding.detalle.text = buildString {
                append("$piezas pieza(s) · Deposito ${d.deposito.comoPrecio() ?: "$0"}")
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
        private val DIFF = object : DiffUtil.ItemCallback<DevolucionUi>() {
            override fun areItemsTheSame(a: DevolucionUi, b: DevolucionUi) = a.dev.id == b.dev.id
            override fun areContentsTheSame(a: DevolucionUi, b: DevolucionUi) = a == b
        }
    }
}
