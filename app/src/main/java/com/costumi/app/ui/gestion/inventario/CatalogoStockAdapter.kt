package com.costumi.app.ui.gestion.inventario

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemStockCatalogoBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.google.android.material.color.MaterialColors
import java.util.UUID

/**
 * Lista de stocks del inventario filtrado por categoría/etiquetas: cada fila muestra la prenda con su
 * precio, sus valores de etiqueta ("Rojo · M") y su STOCK disponible. Al tocar abre el detalle de stock.
 */
class CatalogoStockAdapter(
    private val alTocar: (PrendaDeCatalogoResponse) -> Unit,
) : ListAdapter<PrendaDeCatalogoResponse, CatalogoStockAdapter.VH>(DIFF) {

    /** valorEtiquetaId -> nombre, para pintar los valores de etiqueta. Se fija antes de `submitList`. */
    var nombresDeValores: Map<UUID, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStockCatalogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemStockCatalogoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun enlazar(p: PrendaDeCatalogoResponse) {
            binding.foto.cargarFoto(p.fotoUrl)
            binding.nombre.text = p.nombre.orEmpty()
            binding.detalle.text = detalleDe(p)

            val valores = p.etiquetas.orEmpty().mapNotNull { nombresDeValores[it.valorEtiquetaId] }
            binding.etiquetas.isVisible = valores.isNotEmpty()
            binding.etiquetas.text = valores.joinToString(" · ")

            val stock = p.unidadesDisponibles ?: 0
            binding.chipStock.text = if (stock <= 0) "Sin stock" else "$stock disp."
            val fondo = MaterialColors.getColor(
                binding.root,
                if (stock <= 0) com.google.android.material.R.attr.colorErrorContainer
                else com.google.android.material.R.attr.colorSecondaryContainer,
            )
            val texto = MaterialColors.getColor(
                binding.root,
                if (stock <= 0) com.google.android.material.R.attr.colorOnErrorContainer
                else com.google.android.material.R.attr.colorOnSecondaryContainer,
            )
            binding.chipStock.chipBackgroundColor = ColorStateList.valueOf(fondo)
            binding.chipStock.setTextColor(texto)

            binding.root.setOnClickListener { alTocar(p) }
        }

        private fun detalleDe(p: PrendaDeCatalogoResponse): String {
            val venta = p.precioVenta.comoPrecio()
            val renta = p.precioRenta.comoPrecio()
            return when (p.tipoArticulo) {
                "VENTA" -> "Venta ${venta ?: "-"}"
                "RENTA" -> "Renta ${renta ?: "-"}"
                else -> "Venta ${venta ?: "-"}  ·  Renta ${renta ?: "-"}"
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PrendaDeCatalogoResponse>() {
            override fun areItemsTheSame(a: PrendaDeCatalogoResponse, b: PrendaDeCatalogoResponse) = a.id == b.id
            override fun areContentsTheSame(a: PrendaDeCatalogoResponse, b: PrendaDeCatalogoResponse) = a == b
        }
    }
}
