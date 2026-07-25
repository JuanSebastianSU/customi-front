package com.costumi.app.ui.gestion.caja

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemMovimientoBinding
import com.costumi.apiclient.models.MovimientoResponse

/** Movimientos de un turno: ingreso/egreso por método, con concepto y monto con signo. */
class MovimientoAdapter : ListAdapter<MovimientoResponse, MovimientoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMovimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    class VH(private val binding: ItemMovimientoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(m: MovimientoResponse) {
            val egreso = m.tipo == "EGRESO"
            val metodo = (m.metodo ?: "").lowercase().replaceFirstChar { it.uppercase() }
            binding.concepto.text = m.concepto?.takeIf { it.isNotBlank() } ?: (if (egreso) "Egreso" else "Ingreso")
            binding.detalle.text = "${if (egreso) "Egreso" else "Ingreso"} · $metodo"
            binding.monto.text = (if (egreso) "-" else "+") + (m.monto.comoPrecio() ?: "-")
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MovimientoResponse>() {
            override fun areItemsTheSame(a: MovimientoResponse, b: MovimientoResponse) = a === b
            override fun areContentsTheSame(a: MovimientoResponse, b: MovimientoResponse) = a == b
        }
    }
}
