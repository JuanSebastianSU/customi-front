package com.costumi.app.ui.cliente.favoritos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.app.databinding.ItemFavoritoBinding
import com.costumi.app.ui.cargarFoto

/** "Mis guardados": un disfraz guardado por fila; tocar lo abre, el corazón lo quita. */
class FavoritoAdapter(
    private val alTocar: (FavoritoDisfrazEntity) -> Unit,
    private val alQuitar: (FavoritoDisfrazEntity) -> Unit,
) : ListAdapter<FavoritoDisfrazEntity, FavoritoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFavoritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemFavoritoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(f: FavoritoDisfrazEntity) {
            binding.foto.cargarFoto(f.fotoUrl)
            binding.nombre.text = f.nombre
            val renta = f.precioRenta?.let { "Renta $it" }
            val venta = f.precioVenta?.let { "Venta $it" }
            val precios = listOfNotNull(renta, venta).joinToString("  ·  ")
            binding.precios.isVisible = precios.isNotBlank()
            binding.precios.text = precios
            binding.root.setOnClickListener { alTocar(f) }
            binding.botonQuitar.setOnClickListener { alQuitar(f) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavoritoDisfrazEntity>() {
            override fun areItemsTheSame(a: FavoritoDisfrazEntity, b: FavoritoDisfrazEntity) = a.disfrazId == b.disfrazId
            override fun areContentsTheSame(a: FavoritoDisfrazEntity, b: FavoritoDisfrazEntity) = a == b
        }
    }
}
