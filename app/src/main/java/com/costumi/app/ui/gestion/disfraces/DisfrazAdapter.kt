package com.costumi.app.ui.gestion.disfraces

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemDisfrazBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.DisfrazResponse

/** Lista de disfraces: piezas + precio, con menú (disponibilidad / archivar-activar). */
class DisfrazAdapter(
    private val alVerDisponibilidad: (DisfrazResponse) -> Unit,
    private val alEditar: (DisfrazResponse) -> Unit,
    private val alAsignar: (DisfrazResponse) -> Unit,
    private val alAlternarArchivado: (DisfrazResponse) -> Unit,
) : ListAdapter<DisfrazResponse, DisfrazAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDisfrazBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemDisfrazBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(d: DisfrazResponse) {
            binding.foto.cargarFoto(d.fotoUrl)
            binding.nombre.text = d.nombre.orEmpty()
            val archivado = d.activo != true
            binding.chipArchivado.isVisible = archivado
            binding.root.alpha = if (archivado) 0.55f else 1f

            val piezas = d.slots?.size ?: 0
            val piezasTxt = if (piezas == 1) "1 pieza" else "$piezas piezas"
            // Se muestra el precio de la operación que el dueño habilitó (tipo del disfraz).
            val precio = when (d.tipo) {
                DisfrazResponse.Tipo.VENTA -> precioDe(d.precioVentaGeneral, d.precioVentaSugerido)
                else -> precioDe(d.precioRentaGeneral, d.precioRentaSugerido)?.let { "$it / dia" }
            }
            val partes = listOfNotNull(piezasTxt, precio, etiquetaTipo(d.tipo))
            binding.detalle.text = partes.joinToString(" · ")

            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, d, archivado) }
        }

        /** El precio que fijó el dueño; si no lo puso, la suma sugerida de las piezas. */
        private fun precioDe(general: java.math.BigDecimal?, sugerido: java.math.BigDecimal?): String? =
            general.comoPrecio() ?: sugerido.comoPrecio()?.let { "$it (sugerido)" }

        /** Solo se aclara cuando el disfraz esta limitado a una operacion. */
        private fun etiquetaTipo(tipo: DisfrazResponse.Tipo?): String? = when (tipo) {
            DisfrazResponse.Tipo.RENTA -> "solo renta"
            DisfrazResponse.Tipo.VENTA -> "solo venta"
            else -> null
        }

        private fun mostrarMenu(ancla: View, d: DisfrazResponse, archivado: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_ASIGNAR, 0, "Vender / Rentar a cliente")
                menu.add(0, ID_DISPONIBILIDAD, 1, "Ver disponibilidad")
                menu.add(0, ID_EDITAR, 2, "Editar")
                menu.add(0, ID_ARCHIVAR, 3, if (archivado) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_ASIGNAR -> alAsignar(d)
                        ID_DISPONIBILIDAD -> alVerDisponibilidad(d)
                        ID_EDITAR -> alEditar(d)
                        ID_ARCHIVAR -> alAlternarArchivado(d)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object {
        private const val ID_DISPONIBILIDAD = 1
        private const val ID_EDITAR = 2
        private const val ID_ARCHIVAR = 3
        private const val ID_ASIGNAR = 4

        private val DIFF = object : DiffUtil.ItemCallback<DisfrazResponse>() {
            override fun areItemsTheSame(a: DisfrazResponse, b: DisfrazResponse) = a.id == b.id
            override fun areContentsTheSame(a: DisfrazResponse, b: DisfrazResponse) = a == b
        }
    }
}
