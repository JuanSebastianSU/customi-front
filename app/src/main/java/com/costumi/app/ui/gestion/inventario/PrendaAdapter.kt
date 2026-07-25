package com.costumi.app.ui.gestion.inventario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemPrendaGestionBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.PrendaResponse

/** Lista paginada de prendas del inventario, con menú de acciones (editar / archivar-activar). */
class PrendaAdapter(
    private val alEditar: (PrendaResponse) -> Unit,
    private val alVerStock: (PrendaResponse) -> Unit,
    private val alAlternarArchivado: (PrendaResponse) -> Unit,
) : PagingDataAdapter<PrendaResponse, PrendaAdapter.VH>(DIFF) {

    /** Ids de prendas con alguna variante en stock bajo: se marcan con un chip. */
    var prendasConStockBajo: Set<java.util.UUID> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPrendaGestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemPrendaGestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(prenda: PrendaResponse) {
            binding.foto.cargarFoto(prenda.fotoUrl)
            binding.nombre.text = prenda.nombre.orEmpty()
            binding.detalle.text = detalleDe(prenda)
            binding.chipArchivada.isVisible = prenda.archivada == true
            binding.chipStockBajo.isVisible = prenda.id != null && prenda.id in prendasConStockBajo
            binding.root.alpha = if (prenda.archivada == true) 0.55f else 1f

            binding.root.setOnClickListener { alEditar(prenda) }
            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, prenda) }
        }

        private fun mostrarMenu(ancla: View, prenda: PrendaResponse) {
            val archivada = prenda.archivada == true
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_EDITAR, 0, "Editar")
                menu.add(0, ID_STOCK, 1, "Stock")
                menu.add(0, ID_ARCHIVAR, 2, if (archivada) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_EDITAR -> alEditar(prenda)
                        ID_STOCK -> alVerStock(prenda)
                        ID_ARCHIVAR -> alAlternarArchivado(prenda)
                    }
                    true
                }
                show()
            }
        }

        private fun detalleDe(p: PrendaResponse): String {
            val venta = p.precioVenta.comoPrecio()
            val renta = p.precioRenta.comoPrecio()
            return when (p.tipoArticulo) {
                PrendaResponse.TipoArticulo.VENTA -> "Venta ${venta ?: "-"}"
                PrendaResponse.TipoArticulo.RENTA -> "Renta ${renta ?: "-"}"
                else -> "Venta ${venta ?: "-"}  ·  Renta ${renta ?: "-"}"
            }
        }
    }

    companion object {
        private const val ID_EDITAR = 1
        private const val ID_STOCK = 2
        private const val ID_ARCHIVAR = 3

        private val DIFF = object : DiffUtil.ItemCallback<PrendaResponse>() {
            override fun areItemsTheSame(a: PrendaResponse, b: PrendaResponse) = a.id == b.id
            override fun areContentsTheSame(a: PrendaResponse, b: PrendaResponse) = a == b
        }
    }
}
