package com.costumi.app.ui.gestion.clientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemClienteBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.ClienteResponse

/** Lista paginada de clientes con chips de estado y menú de acciones. */
class ClienteAdapter(
    private val alEditar: (ClienteResponse) -> Unit,
    private val alVerHistorial: (ClienteResponse) -> Unit,
    private val alVerEstadoCuenta: (ClienteResponse) -> Unit,
    private val alAlternarListaNegra: (ClienteResponse) -> Unit,
    private val alAlternarArchivado: (ClienteResponse) -> Unit,
) : PagingDataAdapter<ClienteResponse, ClienteAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemClienteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun enlazar(c: ClienteResponse) {
            val nombre = c.nombre.orEmpty().ifBlank { "Cliente sin nombre" }
            binding.nombre.text = nombre
            binding.inicial.text = nombre.trim().take(1).uppercase()
            // El telefono primero: es el dato con el que se le llama cuando se pasa de la fecha.
            binding.detalle.text = listOfNotNull(
                c.telefono?.takeIf { it.isNotBlank() },
                c.documento?.takeIf { it.isNotBlank() },
                c.email?.takeIf { it.isNotBlank() },
            ).joinToString("  ·  ").ifBlank { "Sin datos de contacto" }

            // Cartera: cuánto debe todavía y su multa acumulada (solo si hay algo, RF-7/11.5).
            val debe = c.saldoPendiente?.takeIf { it.signum() > 0 }?.let { "Debe ${it.comoPrecio()}" }
            val multa = c.multaTotal?.takeIf { it.signum() > 0 }?.let { "multa ${it.comoPrecio()}" }
            val enCurso = if (c.tieneRentaEnCurso == true) "Renta en curso" else null
            val cartera = listOfNotNull(debe, multa, enCurso).joinToString("  ·  ")
            binding.botonDeuda.isVisible = cartera.isNotBlank()
            binding.botonDeuda.text = cartera
            binding.botonDeuda.setOnClickListener { alVerEstadoCuenta(c) }

            // Un solo estado a la vez, y el que más pesa: la lista negra manda sobre el archivado.
            val estado = when {
                c.enListaNegra == true -> "Lista negra" to Tono.ERROR
                c.archivada == true -> "Archivado" to Tono.NEUTRO
                else -> null
            }
            binding.chipEstado.isVisible = estado != null
            estado?.let { (texto, tono) -> binding.chipEstado.pintarPastilla(texto, tono) }
            binding.root.alpha = if (c.archivada == true) 0.6f else 1f

            binding.root.setOnClickListener { alEditar(c) }
            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, c) }
        }

        private fun mostrarMenu(ancla: View, c: ClienteResponse) {
            val archivado = c.archivada == true
            val enNegra = c.enListaNegra == true
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_EDITAR, 0, "Editar")
                menu.add(0, ID_ESTADO_CUENTA, 1, "Estado de cuenta")
                menu.add(0, ID_HISTORIAL, 2, "Ver historial")
                menu.add(0, ID_LISTA_NEGRA, 3, if (enNegra) "Quitar de lista negra" else "Poner en lista negra")
                menu.add(0, ID_ARCHIVAR, 4, if (archivado) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_EDITAR -> alEditar(c)
                        ID_ESTADO_CUENTA -> alVerEstadoCuenta(c)
                        ID_HISTORIAL -> alVerHistorial(c)
                        ID_LISTA_NEGRA -> alAlternarListaNegra(c)
                        ID_ARCHIVAR -> alAlternarArchivado(c)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object {
        private const val ID_EDITAR = 1
        private const val ID_HISTORIAL = 2
        private const val ID_LISTA_NEGRA = 3
        private const val ID_ARCHIVAR = 4
        private const val ID_ESTADO_CUENTA = 5

        private val DIFF = object : DiffUtil.ItemCallback<ClienteResponse>() {
            override fun areItemsTheSame(a: ClienteResponse, b: ClienteResponse) = a.id == b.id
            override fun areContentsTheSame(a: ClienteResponse, b: ClienteResponse) = a == b
        }
    }
}
