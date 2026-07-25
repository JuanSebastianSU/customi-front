package com.costumi.app.ui.gestion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemDesgloseLineaBinding
import com.costumi.app.databinding.SheetDesgloseBinding
import com.costumi.app.ui.cargarFoto
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Una línea del desglose de una renta/venta/devolución: foto + nombre + detalle + monto. */
data class LineaDesglose(
    val fotoUrl: String?,
    val nombre: String,
    val detalle: String?,
    val monto: String?,
)

/** Adapter del desglose (líneas con foto). */
class DesgloseLineaAdapter : ListAdapter<LineaDesglose, DesgloseLineaAdapter.VH>(DIFF) {

    class VH(val binding: ItemDesgloseLineaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemDesgloseLineaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val linea = getItem(position)
        // Sin foto no se deja el recuadro gris: en un estado de cuenta (una línea por renta) no hay
        // imagen que mostrar y ocho placeholders en fila solo ensucian la lectura.
        holder.binding.foto.isVisible = !linea.fotoUrl.isNullOrBlank()
        holder.binding.foto.cargarFoto(linea.fotoUrl)
        holder.binding.nombre.text = linea.nombre
        holder.binding.detalle.isVisible = !linea.detalle.isNullOrBlank()
        holder.binding.detalle.text = linea.detalle
        holder.binding.monto.isVisible = !linea.monto.isNullOrBlank()
        holder.binding.monto.text = linea.monto
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LineaDesglose>() {
            override fun areItemsTheSame(a: LineaDesglose, b: LineaDesglose) = a === b
            override fun areContentsTheSame(a: LineaDesglose, b: LineaDesglose) = a == b
        }
    }
}

/**
 * Muestra el desglose de una operación (renta/venta/devolución) en un bottom sheet: título, código de
 * retiro, subtítulo, las líneas con foto y un pie con los totales. Todo con datos ya en memoria (sin red).
 */
fun Fragment.mostrarDesglose(
    titulo: String,
    codigoRetiro: String? = null,
    subtitulo: String? = null,
    lineas: List<LineaDesglose>,
    pie: List<Pair<String, String>> = emptyList(),
) {
    val sheet = BottomSheetDialog(requireContext())
    val binding = SheetDesgloseBinding.inflate(layoutInflater)

    binding.titulo.text = titulo
    binding.subtitulo.isVisible = !subtitulo.isNullOrBlank()
    binding.subtitulo.text = subtitulo
    binding.codigo.isVisible = !codigoRetiro.isNullOrBlank()
    binding.codigo.text = "Código de retiro: $codigoRetiro"

    val adapter = DesgloseLineaAdapter()
    binding.lineas.layoutManager = LinearLayoutManager(requireContext())
    binding.lineas.adapter = adapter
    binding.lineas.isVisible = lineas.isNotEmpty()
    adapter.submitList(lineas)

    binding.pie.isVisible = pie.isNotEmpty()
    binding.pie.text = pie.joinToString("\n") { (etiqueta, valor) -> "$etiqueta: $valor" }

    sheet.setContentView(binding.root)
    sheet.show()
}
