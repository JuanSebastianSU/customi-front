package com.costumi.app.ui.cliente.detalle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemOpcionSlotBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.OpcionDto
import com.google.android.material.color.MaterialColors
import java.util.UUID

/**
 * "Ruleta" de un slot: opciones concretas (prendas) que el cliente puede elegir, con foto/precio/stock.
 * Selección única con resaltado local; reporta la prenda elegida por [alSeleccionar].
 */
class SlotOpcionAdapter(
    private val alSeleccionar: (OpcionDto) -> Unit,
) : ListAdapter<OpcionDto, SlotOpcionAdapter.VH>(DIFF) {

    var seleccionadaId: UUID? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(val binding: ItemOpcionSlotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOpcionSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val opcion = getItem(position)
        holder.binding.foto.cargarFoto(opcion.fotoUrl)
        holder.binding.nombre.text = opcion.nombre ?: "Prenda"
        holder.binding.precio.text = opcion.precioRenta.comoPrecio() ?: ""

        // Sin stock la opción se muestra pero NO se puede elegir (el cliente ve que existe, atenuada).
        val stock = opcion.unidadesDisponibles ?: 0
        val hayStock = stock > 0
        holder.binding.stock.text = when {
            !hayStock -> "Sin stock"
            stock == 1 -> "1 disponible"
            else -> "$stock disponibles"
        }
        holder.binding.stock.setTextColor(
            MaterialColors.getColor(
                holder.binding.root,
                if (hayStock) {
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                } else {
                    androidx.appcompat.R.attr.colorError
                },
            ),
        )
        holder.binding.root.alpha = if (hayStock) 1f else 0.45f

        val seleccionada = hayStock && opcion.prendaId != null && opcion.prendaId == seleccionadaId
        holder.binding.card.isChecked = seleccionada
        val densidad = holder.binding.root.resources.displayMetrics.density
        holder.binding.card.strokeWidth = ((if (seleccionada) 2 else 1) * densidad).toInt()

        holder.binding.card.isEnabled = hayStock
        holder.binding.card.isClickable = hayStock
        if (hayStock) {
            holder.binding.card.setOnClickListener {
                seleccionadaId = opcion.prendaId
                alSeleccionar(opcion)
            }
        } else {
            holder.binding.card.setOnClickListener(null)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<OpcionDto>() {
            override fun areItemsTheSame(a: OpcionDto, b: OpcionDto) = a.prendaId == b.prendaId
            override fun areContentsTheSame(a: OpcionDto, b: OpcionDto) = a == b
        }
    }
}
