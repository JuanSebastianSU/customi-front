package com.costumi.app.ui.gestion.ventas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemVentaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.common.colorDeTexto
import com.costumi.app.ui.common.comoFechaHora
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.VentaResponse

/** Lista paginada de ventas: total, estado y artículos, con acciones a mano (cobrar/devolver). */
class VentaAdapter(
    private val alTocar: (VentaResponse) -> Unit,
    private val alDevolver: (VentaResponse) -> Unit,
    private val alCobrar: (VentaResponse) -> Unit,
) : PagingDataAdapter<VentaResponse, VentaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVentaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemVentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(v: VentaResponse) {
            binding.foto.cargarFoto(v.lineas?.firstOrNull()?.fotoUrl)
            binding.cliente.text = v.clienteNombre?.takeIf { it.isNotBlank() } ?: "Venta en mostrador"
            binding.total.text = v.total.comoPrecio() ?: "-"

            val articulos = v.lineas?.sumOf { it.cantidad ?: 0 } ?: 0
            binding.codigo.text = listOfNotNull(
                v.codigoRetiro?.takeIf { it.isNotBlank() },
                if (articulos == 1) "1 articulo" else "$articulos articulos",
            ).joinToString("  ·  ").ifBlank { "Sin codigo de retiro" }

            // Cuándo se hizo la venta (recencia): la lista ya viene más reciente primero.
            val fecha = v.creadaEn?.comoFechaHora()
            binding.fecha.isVisible = fecha != null
            binding.fecha.text = fecha.orEmpty()

            // Lo devuelto se muestra solo si lo hay, en el tono del estado (ámbar/gris).
            val devuelto = v.montoReembolsado?.takeIf { it.signum() > 0 }
            binding.devuelto.isVisible = devuelto != null
            devuelto?.let {
                binding.devuelto.text = "Devuelto ${it.comoPrecio()}"
                binding.devuelto.setTextColor(EstadoDeVenta.tono(v).colorDeTexto(binding.root))
            }

            binding.chipEstado.pintarPastilla(EstadoDeVenta.etiqueta(v), EstadoDeVenta.tono(v))

            // Devolver solo si queda algo por devolver; cobrar/pagos siempre disponible.
            binding.botonDevolver.isVisible = EstadoDeVenta.unidadesDevolvibles(v) > 0
            binding.botonDevolver.setOnClickListener { alDevolver(v) }
            binding.botonCobrar.setOnClickListener { alCobrar(v) }
            binding.root.setOnClickListener { alTocar(v) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VentaResponse>() {
            override fun areItemsTheSame(a: VentaResponse, b: VentaResponse) = a.id == b.id
            override fun areContentsTheSame(a: VentaResponse, b: VentaResponse) = a == b
        }
    }
}
