package com.costumi.app.ui.gestion.pagos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemPagoBinding
import com.costumi.apiclient.models.PagoResponse
import java.time.format.DateTimeFormatter

/** Movimientos de pago de una operación: tipo + método, fecha/referencia y monto. */
class PagoAdapter : ListAdapter<PagoResponse, PagoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPagoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    class VH(private val binding: ItemPagoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(p: PagoResponse) {
            binding.concepto.text = "${etiquetaTipo(p.tipoPago)} · ${(p.metodo ?: "").lowercase().replaceFirstChar { it.uppercase() }}"
            val fecha = p.fecha?.format(FORMATO).orEmpty()
            val ref = p.referencia?.takeIf { it.isNotBlank() }?.let { " · ref $it" } ?: ""
            binding.detalle.text = "$fecha$ref"
            val resta = p.tipoPago == "REEMBOLSO" || p.tipoPago == "DEVOLUCION_DEPOSITO"
            binding.monto.text = (if (resta) "-" else "") + (p.monto.comoPrecio() ?: "-")
        }

        private fun etiquetaTipo(tipo: String?): String = when (tipo) {
            "COBRO" -> "Cobro"
            "REEMBOLSO" -> "Reembolso"
            "DEPOSITO" -> "Deposito"
            "DEVOLUCION_DEPOSITO" -> "Devolucion deposito"
            else -> "Movimiento"
        }
    }

    companion object {
        private val FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        private val DIFF = object : DiffUtil.ItemCallback<PagoResponse>() {
            override fun areItemsTheSame(a: PagoResponse, b: PagoResponse) = a.id == b.id
            override fun areContentsTheSame(a: PagoResponse, b: PagoResponse) = a == b
        }
    }
}
