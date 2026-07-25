package com.costumi.app.ui.cliente.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemLineaPedidoBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.LineaDeHistorial

/**
 * Lo que el cliente ve en un pedido. Si varias lineas salieron de armar el mismo disfraz, se muestran
 * como **un solo articulo con el nombre del disfraz**: el cliente compro "Traje Pirata", no una capa
 * suelta y un sombrero suelto.
 */
data class ArticuloDePedido(
    val clave: String,
    val nombre: String,
    val fotoUrl: String?,
    val cantidad: Int,
    /** Aclaracion secundaria, p. ej. "2 piezas" cuando el articulo es un disfraz. */
    val detalle: String?,
)

/** Artículos (con foto) de un pedido, en un carrusel horizontal dentro de "Mis Pedidos". */
class LineaPedidoAdapter : ListAdapter<ArticuloDePedido, LineaPedidoAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemLineaPedidoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLineaPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val articulo = getItem(position)
        holder.binding.foto.cargarFoto(articulo.fotoUrl)
        holder.binding.nombre.text = articulo.nombre
        holder.binding.cantidad.text = listOfNotNull("x${articulo.cantidad}", articulo.detalle)
            .joinToString("  ·  ")
    }

    companion object {
        /**
         * Colapsa las lineas que salieron del mismo disfraz. Se agrupa por `disfrazGrupo` y no por
         * `disfrazId` porque el mismo disfraz puede ir dos veces con piezas distintas: son dos articulos
         * y deben verse como dos. Las prendas sueltas pasan tal cual.
         */
        fun agrupar(lineas: List<LineaDeHistorial>): List<ArticuloDePedido> {
            val (deDisfraz, sueltas) = lineas.partition { it.disfrazGrupo != null }
            val disfraces = deDisfraz.groupBy { it.disfrazGrupo!! }.map { (grupo, piezas) ->
                val primera = piezas.first()
                val cuantasPiezas = piezas.size
                ArticuloDePedido(
                    clave = "disfraz:$grupo",
                    nombre = primera.disfrazNombre ?: "Disfraz",
                    // El disfraz aun no tiene foto propia en el historial: se usa la de su primera pieza.
                    fotoUrl = piezas.firstOrNull { !it.fotoUrl.isNullOrBlank() }?.fotoUrl,
                    cantidad = primera.disfrazCantidad ?: 1,
                    detalle = if (cuantasPiezas == 1) "1 pieza" else "$cuantasPiezas piezas",
                )
            }
            val prendas = sueltas.map {
                ArticuloDePedido(
                    clave = "prenda:${it.prendaId}:${it.nombre}",
                    nombre = it.nombre ?: "Articulo",
                    fotoUrl = it.fotoUrl,
                    cantidad = it.cantidad ?: 1,
                    detalle = null,
                )
            }
            return disfraces + prendas
        }

        private val DIFF = object : DiffUtil.ItemCallback<ArticuloDePedido>() {
            override fun areItemsTheSame(a: ArticuloDePedido, b: ArticuloDePedido) = a.clave == b.clave
            override fun areContentsTheSame(a: ArticuloDePedido, b: ArticuloDePedido) = a == b
        }
    }
}
