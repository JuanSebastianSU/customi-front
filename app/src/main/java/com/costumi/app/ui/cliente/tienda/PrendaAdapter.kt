package com.costumi.app.ui.cliente.tienda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemPrendaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.PrendaVitrinaResponse

/** Catalogo de una tienda (prendas en vitrina). */
class PrendaAdapter(
    private val alTocar: (PrendaVitrinaResponse) -> Unit,
) : ListAdapter<PrendaVitrinaResponse, PrendaAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemPrendaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPrendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val prenda = getItem(position)
        holder.binding.foto.cargarFoto(prenda.fotoUrl)
        holder.binding.nombre.text = prenda.nombre ?: "Prenda"
        holder.binding.categoria.text = listOfNotNull(prenda.categoria, prenda.tipoArticulo)
            .joinToString(" - ").ifBlank { "Sin categoria" }

        // Etiquetas (color/talla/estilo…): los valores, para que el cliente vea qué es de un vistazo.
        val etiquetas = prenda.etiquetas.orEmpty().mapNotNull { it.valor?.takeIf { v -> v.isNotBlank() } }
        holder.binding.etiquetas.isVisible = etiquetas.isNotEmpty()
        holder.binding.etiquetas.text = etiquetas.joinToString("  ·  ")

        // Renta como precio primario y venta debajo (no en la misma linea): en una tarjeta de grilla,
        // juntarlas truncaba ambas.
        val renta = prenda.precioRenta.comoPrecio()?.let { "Renta $it" }
        val venta = prenda.precioVenta.comoPrecio()?.let { "Venta $it" }
        holder.binding.precioRenta.text = renta ?: venta ?: "Precio no disponible"
        holder.binding.precioVenta.text = if (renta != null) venta.orEmpty() else ""
        holder.binding.precioVenta.isVisible = renta != null && venta != null

        holder.binding.root.setOnClickListener { alTocar(prenda) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<PrendaVitrinaResponse>() {
            override fun areItemsTheSame(a: PrendaVitrinaResponse, b: PrendaVitrinaResponse) = a.id == b.id
            override fun areContentsTheSame(a: PrendaVitrinaResponse, b: PrendaVitrinaResponse) = a == b
        }
    }
}
