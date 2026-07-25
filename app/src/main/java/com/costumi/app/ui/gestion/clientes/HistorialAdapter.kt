package com.costumi.app.ui.gestion.clientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemHistorialBinding
import com.costumi.apiclient.models.HistorialItem

/** Historial (ventas/rentas) de un cliente: tipo, fecha, monto y estado. */
class HistorialAdapter : ListAdapter<HistorialItem, HistorialAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    class VH(private val binding: ItemHistorialBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(h: HistorialItem) {
            binding.tipo.text = when (h.tipo?.uppercase()) {
                "VENTA" -> "Compra"
                "RENTA" -> "Renta"
                else -> h.tipo.orEmpty().ifBlank { "Operacion" }
            }
            binding.fecha.text = h.fecha?.toString().orEmpty()
            binding.monto.text = h.monto.comoPrecio() ?: "-"
            binding.estado.text = h.estado.orEmpty()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistorialItem>() {
            override fun areItemsTheSame(a: HistorialItem, b: HistorialItem) = a.operacionId == b.operacionId
            override fun areContentsTheSame(a: HistorialItem, b: HistorialItem) = a == b
        }
    }
}
