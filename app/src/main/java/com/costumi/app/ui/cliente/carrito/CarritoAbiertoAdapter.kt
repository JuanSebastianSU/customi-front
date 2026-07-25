package com.costumi.app.ui.cliente.carrito

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.data.remote.CarritoAbiertoDto
import com.costumi.app.databinding.ItemCarritoAbiertoBinding

/** Un carrito abierto por tienda/sucursal/tipo; al tocarlo se abre ese carrito. */
class CarritoAbiertoAdapter(
    private val alTocar: (CarritoAbiertoDto) -> Unit,
) : ListAdapter<CarritoAbiertoDto, CarritoAbiertoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCarritoAbiertoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemCarritoAbiertoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(c: CarritoAbiertoDto) {
            binding.tienda.text = c.empresaNombre ?: "Tienda"
            val tipo = if (c.tipo == "RENTA") "Renta" else "Compra"
            val unidades = c.articulos ?: 0
            val articulos = if (unidades == 1) "1 articulo" else "$unidades articulos"
            binding.detalle.text = listOfNotNull(tipo, c.sucursalNombre, articulos).joinToString("  ·  ")
            binding.root.setOnClickListener { alTocar(c) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CarritoAbiertoDto>() {
            // Un carrito se identifica por tienda x sucursal x tipo: es lo que lo hace unico.
            override fun areItemsTheSame(a: CarritoAbiertoDto, b: CarritoAbiertoDto) =
                a.empresaId == b.empresaId && a.sucursalId == b.sucursalId && a.tipo == b.tipo

            override fun areContentsTheSame(a: CarritoAbiertoDto, b: CarritoAbiertoDto) = a == b
        }
    }
}
