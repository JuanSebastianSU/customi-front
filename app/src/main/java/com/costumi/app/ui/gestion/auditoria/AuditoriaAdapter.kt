package com.costumi.app.ui.gestion.auditoria

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import com.costumi.app.databinding.ItemAuditoriaBinding
import com.costumi.app.ui.common.comoFechaHora
import com.costumi.apiclient.models.AuditoriaResponse

/** Trail de auditoria: accion, detalle y fecha de cada evento de la empresa. */
class AuditoriaAdapter : ListAdapter<AuditoriaResponse, AuditoriaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAuditoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemAuditoriaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(r: AuditoriaResponse) {
            binding.accion.text = legible(r.accion)
            binding.detalle.text = r.detalle.orEmpty()
            binding.detalle.isVisible = !r.detalle.isNullOrBlank()
            // Fecha relativa ("hoy 14:30", "ayer 09:12", "12 jul 14:30"): en un trail lo que importa es
            // cuándo pasó respecto de hoy, no la fecha absoluta ISO.
            binding.fecha.text = r.fecha?.comoFechaHora().orEmpty()
        }
    }

    companion object {
        /** "DEVOLUCION_REGISTRADA" -> "Devolucion registrada". */
        private fun legible(accion: String?): String {
            if (accion.isNullOrBlank()) return "Evento"
            return accion.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }

        private val DIFF = object : DiffUtil.ItemCallback<AuditoriaResponse>() {
            override fun areItemsTheSame(a: AuditoriaResponse, b: AuditoriaResponse) = a.id == b.id
            override fun areContentsTheSame(a: AuditoriaResponse, b: AuditoriaResponse) = a == b
        }
    }
}
