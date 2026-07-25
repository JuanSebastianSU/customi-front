package com.costumi.app.ui.cliente.tienda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemDisfrazClienteBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.DisfrazResponse

/** Vitrina de disfraces de una tienda (apartado Disfraces). El precio de renta lo fija el dueño. */
class DisfrazVitrinaAdapter(
    private val alTocar: (DisfrazResponse) -> Unit,
) : ListAdapter<DisfrazResponse, DisfrazVitrinaAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemDisfrazClienteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDisfrazClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val disfraz = getItem(position)
        holder.binding.foto.cargarFoto(disfraz.fotoUrl)
        holder.binding.nombre.text = disfraz.nombre ?: "Disfraz"

        val piezas = disfraz.slots?.size ?: 0
        holder.binding.piezas.text = if (piezas == 1) "1 pieza" else "$piezas piezas"

        // Solo se muestra el precio de la operación que el dueño habilitó para este disfraz.
        val permiteRenta = disfraz.tipo != DisfrazResponse.Tipo.VENTA
        val permiteVenta = disfraz.tipo != DisfrazResponse.Tipo.RENTA

        // El precio es el que fijó el dueño (general); si no lo puso, la suma sugerida.
        val renta = (disfraz.precioRentaGeneral ?: disfraz.precioRentaSugerido).comoPrecio()
        holder.binding.precioRenta.text =
            if (!permiteRenta) "" else renta?.let { "Renta $it" } ?: "Renta no disponible"
        holder.binding.precioRenta.visibility =
            if (permiteRenta) android.view.View.VISIBLE else android.view.View.GONE

        val venta = (disfraz.precioVentaGeneral ?: disfraz.precioVentaSugerido).comoPrecio()
        holder.binding.precioVenta.text = if (!permiteVenta) "" else venta?.let { "Venta $it" }.orEmpty()
        holder.binding.precioVenta.visibility =
            if (permiteVenta && venta != null) android.view.View.VISIBLE else android.view.View.GONE

        holder.binding.root.setOnClickListener { alTocar(disfraz) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DisfrazResponse>() {
            override fun areItemsTheSame(a: DisfrazResponse, b: DisfrazResponse) = a.id == b.id
            override fun areContentsTheSame(a: DisfrazResponse, b: DisfrazResponse) = a == b
        }
    }
}
