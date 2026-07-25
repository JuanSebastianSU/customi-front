package com.costumi.app.ui.cliente.carrito

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemLineaCarritoBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.LineaDeCarritoResponse
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Líneas del carrito, con precio unitario y subtotal calculados por el backend. */
class CarritoLineaAdapter(
    private val esRenta: Boolean,
    private val alQuitar: (LineaDeCarritoResponse) -> Unit,
    private val alEditarCantidad: (LineaDeCarritoResponse) -> Unit = {},
) : ListAdapter<LineaDeCarritoResponse, CarritoLineaAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemLineaCarritoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLineaCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val linea = getItem(position)
        val cantidad = linea.cantidad ?: 1
        holder.binding.foto.cargarFoto(linea.fotoUrl)
        val etiquetaDisfraz = if (linea.disfrazId != null) {
            val piezas = linea.selecciones?.size ?: 0
            if (piezas > 0) "  ·  disfraz ($piezas ${if (piezas == 1) "pieza" else "piezas"})" else "  ·  disfraz"
        } else {
            ""
        }
        holder.binding.descripcion.text =
            "${linea.nombre ?: "Articulo"}${if (cantidad > 1) "  ·  x$cantidad" else ""}$etiquetaDisfraz"
        val unitario = linea.precioUnitario.comoPrecio()
        holder.binding.precioUnitario.text = when {
            unitario == null -> ""
            esRenta -> "$unitario / dia"
            else -> "$unitario c/u"
        }
        holder.binding.subtotal.text = linea.subtotal.comoPrecio() ?: ""

        // El periodo es un dato normal (siempre visible en renta). El aviso de no-disponible es un error
        // aparte, en rojo, que bloquea el checkout: antes compartian un TextView y se confundian.
        val periodo = if (esRenta) fechasDe(linea) else null
        holder.binding.periodo.isVisible = !periodo.isNullOrBlank()
        holder.binding.periodo.text = periodo.orEmpty()

        val motivo = linea.motivoNoDisponible?.takeIf { it.isNotBlank() }
        holder.binding.aviso.isVisible = motivo != null
        holder.binding.aviso.text = motivo.orEmpty()

        holder.binding.botonQuitar.setOnClickListener { alQuitar(linea) }
        // Tocar la línea permite cambiar la cantidad (A10).
        holder.binding.root.setOnClickListener { alEditarCantidad(linea) }
    }

    /** "12 ago → 15 ago · 3 dias" con las fechas de la propia linea; null si la linea no las tiene. */
    private fun fechasDe(linea: LineaDeCarritoResponse): String? {
        val retiro = linea.fechaRetiro ?: return null
        val devolucion = linea.fechaDevolucion ?: return null
        val dias = ChronoUnit.DAYS.between(retiro, devolucion).coerceAtLeast(1)
        return "${retiro.format(FORMATO)}  →  ${devolucion.format(FORMATO)}  ·  $dias ${if (dias == 1L) "dia" else "dias"}"
    }

    private companion object {
        val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

        val DIFF = object : DiffUtil.ItemCallback<LineaDeCarritoResponse>() {
            // La linea ya tiene id propio: es lo que la identifica (dos lineas pueden ser del mismo disfraz).
            override fun areItemsTheSame(a: LineaDeCarritoResponse, b: LineaDeCarritoResponse) = a.id == b.id
            override fun areContentsTheSame(a: LineaDeCarritoResponse, b: LineaDeCarritoResponse) = a == b
        }
    }
}
