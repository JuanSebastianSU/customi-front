package com.costumi.app.ui.cliente.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemPedidoBinding
import com.costumi.app.ui.common.comoDiaMes
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.HistorialItem

/** "Mis Pedidos": una fila por operación (renta/venta) del cliente, con sus artículos, código y reembolso. */
class PedidoAdapter(
    private val alSolicitarReembolso: (HistorialItem) -> Unit,
) : ListAdapter<HistorialItem, PedidoAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root) {
        val articulosAdapter = LineaPedidoAdapter()

        init {
            binding.articulos.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.articulos.adapter = articulosAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tienda.text = item.empresaNombre ?: "Tienda"
        holder.binding.chipEstado.pintarPastilla(
            EstadoDePedido.etiqueta(item.estado),
            EstadoDePedido.tono(item.estado),
        )
        holder.binding.tipoEstado.text = listOfNotNull(
            tipoLegible(item.tipo),
            item.fecha?.comoDiaMes(),
            EstadoDePedido.pagoEtiqueta(item),
        ).joinToString("  ·  ")
        holder.binding.monto.text = item.monto.comoPrecio() ?: ""

        // Las piezas de un mismo disfraz se muestran como un solo articulo con el nombre del disfraz.
        val articulos = LineaPedidoAdapter.agrupar(item.lineas.orEmpty())
        holder.binding.articulos.isVisible = articulos.isNotEmpty()
        holder.articulosAdapter.submitList(articulos)

        val codigo = item.codigoRetiro
        holder.binding.codigo.isVisible = !codigo.isNullOrBlank()
        holder.binding.codigo.text = "Código de retiro: $codigo"

        // El cliente puede pedir el reembolso de su operación; la tienda lo aprueba/rechaza.
        holder.binding.botonReembolso.isVisible = puedePedirReembolso(item)
        holder.binding.botonReembolso.setOnClickListener { alSolicitarReembolso(item) }
    }

    /** Se ofrece el reembolso salvo en estados donde ya no aplica; el backend valida el resto. */
    private fun puedePedirReembolso(item: HistorialItem): Boolean {
        val estado = item.estado?.uppercase()
        return estado != "CANCELADA" && estado != "REEMBOLSADA"
    }

    private fun tipoLegible(tipo: String?): String = when (tipo?.uppercase()) {
        "RENTA" -> "Renta"
        "VENTA" -> "Compra"
        else -> tipo ?: "Pedido"
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<HistorialItem>() {
            override fun areItemsTheSame(a: HistorialItem, b: HistorialItem) = a.operacionId == b.operacionId
            override fun areContentsTheSame(a: HistorialItem, b: HistorialItem) = a == b
        }
    }
}
