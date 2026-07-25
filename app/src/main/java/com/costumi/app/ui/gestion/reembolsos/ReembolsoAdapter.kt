package com.costumi.app.ui.gestion.reembolsos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemReembolsoBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.comoDiaMes
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse

/** Acción sobre una solicitud de reembolso. */
enum class AccionReembolso { APROBAR, RECHAZAR, REGISTRAR_DEVOLUCION }

/** Bandeja de solicitudes: concepto, monto, estado, motivos y acciones de decisión. */
class ReembolsoAdapter(
    private val alTocar: (SolicitudDeReembolsoResponse) -> Unit,
    private val alDecidir: (SolicitudDeReembolsoResponse, AccionReembolso) -> Unit,
) : ListAdapter<SolicitudDeReembolsoResponse, ReembolsoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReembolsoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemReembolsoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(s: SolicitudDeReembolsoResponse) {
            val tipo = if (s.tipoConcepto?.value == "RENTA") "Renta" else "Venta"
            binding.titulo.text = "$tipo · ${s.monto.comoPrecio() ?: "-"}"

            val estado = s.estado?.value
            binding.chipEstado.pintarPastilla(etiqueta(estado), tono(estado))

            // El motivo del cliente es lo que hay que juzgar: va en primer plano, entrecomillado.
            val motivo = s.motivoSolicitud?.takeIf { it.isNotBlank() }
            binding.motivo.isVisible = motivo != null
            binding.motivo.text = motivo?.let { "“$it”" }.orEmpty()

            // La fecha manda: cuándo lo pidió y —si ya se decidió— cuándo se resolvió (el backend
            // expone creadaEn y decididaEn; se muestran ambas, que antes se perdían).
            binding.detalle.text = listOfNotNull(
                s.creadaEn?.toLocalDate()?.comoDiaMes()?.let { "Solicitado $it" },
                s.decididaEn?.toLocalDate()?.comoDiaMes()?.let { "resuelto $it" },
                s.motivoDecision?.takeIf { it.isNotBlank() }?.let { "· $it" },
            ).joinToString("  ")

            // PENDIENTE: se decide → Aprobar (principal) + Mas (Rechazar / Registrar devolución, que es
            // la precondición para aprobar). RECHAZADA: se puede reconsiderar → Aprobar. APROBADA: nada.
            val pendiente = estado == "PENDIENTE"
            val rechazada = estado == "RECHAZADA"
            binding.acciones.isVisible = pendiente || rechazada
            binding.botonMas.isVisible = pendiente
            binding.botonPrincipal.text = "Aprobar"
            binding.botonPrincipal.setOnClickListener { alDecidir(s, AccionReembolso.APROBAR) }
            binding.botonMas.setOnClickListener { v -> mostrarMenu(v, s) }

            binding.root.setOnClickListener { alTocar(s) }
        }

        private fun mostrarMenu(ancla: View, s: SolicitudDeReembolsoResponse) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_DEVOLVER, 0, "Registrar devolucion")
                menu.add(0, ID_RECHAZAR, 1, "Rechazar")
                setOnMenuItemClickListener { item ->
                    alDecidir(
                        s,
                        if (item.itemId == ID_DEVOLVER) AccionReembolso.REGISTRAR_DEVOLUCION
                        else AccionReembolso.RECHAZAR,
                    )
                    true
                }
                show()
            }
        }
    }

    private fun etiqueta(estado: String?) = when (estado) {
        "PENDIENTE" -> "Pendiente"
        "APROBADA" -> "Aprobada"
        "RECHAZADA" -> "Rechazada"
        else -> estado.orEmpty()
    }

    private fun tono(estado: String?) = when (estado) {
        "PENDIENTE" -> Tono.ALERTA
        "APROBADA" -> Tono.EXITO
        else -> Tono.NEUTRO
    }

    companion object {
        private const val ID_DEVOLVER = 1
        private const val ID_RECHAZAR = 2

        private val DIFF = object : DiffUtil.ItemCallback<SolicitudDeReembolsoResponse>() {
            override fun areItemsTheSame(a: SolicitudDeReembolsoResponse, b: SolicitudDeReembolsoResponse) = a.id == b.id
            override fun areContentsTheSame(a: SolicitudDeReembolsoResponse, b: SolicitudDeReembolsoResponse) = a == b
        }
    }
}
