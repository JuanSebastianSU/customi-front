package com.costumi.app.ui.gestion.rentas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemRentaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.common.colorDeTexto
import com.costumi.app.ui.common.periodoLegible
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.RentaResponse

/**
 * Lista paginada de rentas. Cada fila responde en orden: quién y qué, cuándo, qué urge y qué hacer.
 * Las acciones válidas salen de [CicloDeRenta], así que nunca se ofrece "Entregar" sobre algo cerrado.
 */
class RentaAdapter(
    private val alTocar: (RentaResponse) -> Unit,
    private val alAccionar: (RentaResponse, AccionRenta) -> Unit,
) : PagingDataAdapter<RentaResponse, RentaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRentaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemRentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(r: RentaResponse) {
            binding.foto.cargarFoto(r.lineas?.firstOrNull()?.fotoUrl)
            binding.cliente.text = r.clienteNombre?.takeIf { it.isNotBlank() } ?: "Cliente sin ficha"
            binding.importe.text = (r.importe ?: r.precioPorDia).comoPrecio() ?: "-"

            val articulos = r.lineas?.sumOf { it.cantidad ?: 0 } ?: 0
            binding.codigo.text = listOfNotNull(
                r.codigoRetiro?.takeIf { it.isNotBlank() },
                if (articulos > 1) "$articulos articulos" else null,
            ).joinToString("  ·  ").ifBlank { "Sin codigo de retiro" }

            val periodo = periodoLegible(r.fechaRetiro, r.fechaDevolucion)
            binding.periodo.isVisible = periodo != null
            binding.periodo.text = periodo.orEmpty()

            val tono = CicloDeRenta.tono(r)
            binding.chipEstado.pintarPastilla(CicloDeRenta.etiqueta(r), tono)

            val urgencia = CicloDeRenta.urgencia(r)
            binding.urgencia.isVisible = urgencia != null
            binding.urgencia.text = urgencia.orEmpty()
            binding.urgencia.setTextColor(tono.colorDeTexto(binding.root))

            // Acción principal: solo la que toca en este estado. Si la renta ya terminó, no hay ninguna
            // y el botón desaparece; el de la derecha se queda solo, alineado al borde.
            val principal = CicloDeRenta.accionPrincipal(r)
            binding.botonPrincipal.isVisible = principal != null
            principal?.let { (accion, texto) ->
                binding.botonPrincipal.text = texto
                binding.botonPrincipal.setOnClickListener { alAccionar(r, accion) }
            }

            // Un menú de una sola opción es un toque de más: si solo queda una acción, el botón la
            // ejecuta directamente y se llama como ella (las cerradas solo pueden ver el contrato).
            val secundarias = CicloDeRenta.accionesSecundarias(r)
            val unica = secundarias.singleOrNull()
            binding.botonAcciones.text = unica?.second ?: "Mas"
            binding.botonAcciones.setOnClickListener { v ->
                if (unica != null) alAccionar(r, unica.first) else mostrarMenu(v, r, secundarias)
            }

            binding.root.setOnClickListener { alTocar(r) }
        }

        private fun mostrarMenu(ancla: View, r: RentaResponse, acciones: List<Pair<AccionRenta, String>>) {
            PopupMenu(ancla.context, ancla).apply {
                acciones.forEachIndexed { i, (_, texto) -> menu.add(0, i, i, texto) }
                setOnMenuItemClickListener { item ->
                    acciones.getOrNull(item.itemId)?.let { (accion, _) -> alAccionar(r, accion) }
                    true
                }
                show()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RentaResponse>() {
            override fun areItemsTheSame(a: RentaResponse, b: RentaResponse) = a.id == b.id
            override fun areContentsTheSame(a: RentaResponse, b: RentaResponse) = a == b
        }
    }
}
