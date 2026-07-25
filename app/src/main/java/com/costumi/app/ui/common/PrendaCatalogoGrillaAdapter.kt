package com.costumi.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemOpcionGrillaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import java.util.UUID

/**
 * Catálogo de prendas en grilla (foto, precio, stock) para elegir una o varias. Reusa la tarjeta de la
 * ruleta del cliente. Lo usan el selector de prendas de armar disfraz y el punto de venta. A diferencia de
 * la ruleta del cliente, una prenda sin stock SÍ se puede elegir (el dueño la define/vende igual).
 */
class PrendaCatalogoGrillaAdapter(
    private val alElegir: (PrendaDeCatalogoResponse) -> Unit,
) : ListAdapter<PrendaDeCatalogoResponse, PrendaCatalogoGrillaAdapter.VH>(DIFF) {

    /** Prendas marcadas ahora (una en modo fija, varias en modo múltiple); se marcan con el acento. */
    var seleccionados: Set<UUID> = emptySet()
        set(valor) {
            field = valor
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOpcionGrillaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemOpcionGrillaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(p: PrendaDeCatalogoResponse) {
            binding.foto.cargarFoto(p.fotoUrl)
            binding.nombre.text = p.nombre.orEmpty()
            binding.precio.text = (p.precioRenta ?: p.precioVenta).comoPrecio().orEmpty()
            binding.etiquetas.isVisible = false

            val unidades = p.unidadesDisponibles ?: 0
            binding.stock.text = when {
                unidades <= 0 -> "Sin stock"
                unidades == 1 -> "1 disponible"
                else -> "$unidades disponibles"
            }
            binding.tarjetaOpcion.isChecked = p.id in seleccionados
            binding.tarjetaOpcion.setOnClickListener { alElegir(p) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<PrendaDeCatalogoResponse>() {
            override fun areItemsTheSame(a: PrendaDeCatalogoResponse, b: PrendaDeCatalogoResponse) = a.id == b.id
            override fun areContentsTheSame(a: PrendaDeCatalogoResponse, b: PrendaDeCatalogoResponse) = a == b
        }
    }
}
