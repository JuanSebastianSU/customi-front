package com.costumi.app.ui.cliente.detalle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemOpcionGrillaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.OpcionDto
import java.util.UUID

/**
 * Opciones de una pieza en grilla de dos columnas, para **comparar** antes de elegir.
 *
 * Las opciones sin stock se muestran atenuadas en vez de ocultarse: esconderlas hace creer al cliente
 * que la prenda no existe y que busque en otra tienda, cuando en realidad podria elegir otras fechas.
 */
class OpcionGrillaAdapter(
    private val alElegir: (OpcionDto) -> Unit,
) : ListAdapter<OpcionDto, OpcionGrillaAdapter.VH>(DIFF) {

    /** Prenda elegida ahora mismo; se marca con el borde de acento. */
    var seleccionadaId: UUID? = null
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
        fun enlazar(o: OpcionDto) {
            binding.foto.cargarFoto(o.fotoUrl)
            binding.nombre.text = o.nombre.orEmpty()
            binding.precio.text = o.precioRenta.comoPrecio().orEmpty()

            // Talla y color, el dato que distingue dos prendas iguales. Se muestran los valores ("M · Blanco");
            // si la prenda no tiene etiquetas, la linea desaparece en vez de dejar un hueco.
            val etiquetas = o.etiquetas.orEmpty()
                .mapNotNull { it.valorNombre?.takeIf { valor -> valor.isNotBlank() } }
                .joinToString("  ·  ")
            binding.etiquetas.text = etiquetas
            binding.etiquetas.isVisible = etiquetas.isNotEmpty()

            val unidades = o.unidadesDisponibles ?: 0
            val hayStock = unidades > 0
            binding.stock.text = when {
                !hayStock -> "Sin stock"
                unidades == 1 -> "1 disponible"
                else -> "$unidades disponibles"
            }

            // Sin stock: se ve, pero no se puede elegir.
            binding.tarjetaOpcion.alpha = if (hayStock) 1f else 0.45f
            binding.tarjetaOpcion.isEnabled = hayStock
            binding.tarjetaOpcion.isChecked = hayStock && o.prendaId == seleccionadaId
            binding.tarjetaOpcion.setOnClickListener(
                if (hayStock) android.view.View.OnClickListener { alElegir(o) } else null,
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<OpcionDto>() {
            override fun areItemsTheSame(a: OpcionDto, b: OpcionDto) = a.prendaId == b.prendaId
            override fun areContentsTheSame(a: OpcionDto, b: OpcionDto) = a == b
        }
    }
}
