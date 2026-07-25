package com.costumi.app.ui.gestion.pagos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.app.databinding.ItemOperacionPagoBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.pintarPastilla

/** Operaciones cobrables (venta/renta) para elegir a cuál registrarle un pago. */
class OperacionPagoAdapter(
    private val alElegir: (OperacionPago) -> Unit,
) : PagingDataAdapter<OperacionPago, OperacionPagoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOperacionPagoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemOperacionPagoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(op: OperacionPago) {
            // El código de retiro manda: es lo que el cliente muestra y hay que cotejar al cobrar.
            binding.codigo.text = op.codigoRetiro?.takeIf { it.isNotBlank() } ?: "Sin codigo de retiro"
            val tipo = if (op.tipo == TipoConcepto.VENTA) "Venta" else "Renta"
            binding.detalle.text = "$tipo  ·  ${op.total.comoPrecio() ?: "-"}"
            binding.chipEstado.pintarPastilla(op.estado?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-", Tono.INFO)
            binding.root.setOnClickListener { alElegir(op) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OperacionPago>() {
            override fun areItemsTheSame(a: OperacionPago, b: OperacionPago) = a.id == b.id
            override fun areContentsTheSame(a: OperacionPago, b: OperacionPago) = a == b
        }
    }
}
