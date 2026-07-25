package com.costumi.app.ui.gestion.notificaciones

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemNotificacionBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.comoFechaHora
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.NotificacionResponse
import java.util.UUID

/**
 * Bandeja de notificaciones emitidas (paginada). Cada fila se lee en cristiano: el canal con nombre
 * amigable, el estado como pastilla de color, y el destinatario ("Tu negocio" para los avisos internos)
 * con la fecha relativa.
 */
class NotificacionAdapter(
    private val nombreCliente: (UUID?) -> String,
) : PagingDataAdapter<NotificacionResponse, NotificacionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.enlazar(it) }
    }

    inner class VH(private val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(n: NotificacionResponse) {
            binding.titulo.text = canalAmigable(n.canal)
            binding.chipEstado.pintarPastilla(etiquetaEstado(n.estado), tonoEstado(n.estado))
            binding.mensaje.text = n.mensaje.orEmpty()
            // Un aviso interno (clienteId == null) es para el propio negocio, no para un cliente.
            val destinatario = if (n.clienteId == null) "Tu negocio" else nombreCliente(n.clienteId)
            val fecha = n.fecha?.comoFechaHora()
            binding.detalle.text = listOfNotNull(destinatario, fecha).joinToString("  ·  ")
        }
    }

    private fun canalAmigable(canal: String?): String = when (canal?.uppercase()) {
        "IN_APP" -> "Aviso interno"
        "WHATSAPP" -> "WhatsApp"
        "EMAIL" -> "Email"
        "FCM", "PUSH" -> "Push"
        "SMS" -> "SMS"
        else -> canal?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Notificacion"
    }

    private fun etiquetaEstado(estado: String?): String = when (estado?.uppercase()) {
        "ENVIADA", "ENVIADO" -> "Enviada"
        "ENTREGADA", "ENTREGADO" -> "Entregada"
        "PENDIENTE", "ENCOLADA", "EN_COLA" -> "Pendiente"
        "FALLIDA", "FALLIDO", "ERROR", "RECHAZADA" -> "Fallida"
        else -> estado?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "-"
    }

    private fun tonoEstado(estado: String?): Tono = when (estado?.uppercase()) {
        "ENVIADA", "ENVIADO", "ENTREGADA", "ENTREGADO" -> Tono.EXITO
        "PENDIENTE", "ENCOLADA", "EN_COLA" -> Tono.INFO
        "FALLIDA", "FALLIDO", "ERROR", "RECHAZADA" -> Tono.ERROR
        else -> Tono.NEUTRO
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificacionResponse>() {
            override fun areItemsTheSame(a: NotificacionResponse, b: NotificacionResponse) = a.id == b.id
            override fun areContentsTheSame(a: NotificacionResponse, b: NotificacionResponse) = a == b
        }
    }
}
