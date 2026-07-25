package com.costumi.app.ui.superadmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemEmpresaGestionableBinding
import com.costumi.app.databinding.ItemEmpresaPendienteBinding
import com.costumi.app.databinding.ItemSeccionHeaderBinding
import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import java.time.format.DateTimeFormatter

/**
 * Panel del SuperAdmin: encabezados de sección, solicitudes pendientes (aprobar/rechazar) y empresas
 * gestionables (suspender/reactivar), en una sola lista.
 */
class PanelSuperAdminAdapter(
    private val onAprobar: (EmpresaPendienteResponse) -> Unit,
    private val onRechazar: (EmpresaPendienteResponse) -> Unit,
    private val onSuspender: (EmpresaResumenResponse) -> Unit,
    private val onReactivar: (EmpresaResumenResponse) -> Unit,
) : ListAdapter<FilaSuperAdmin, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FilaSuperAdmin.Encabezado -> TIPO_HEADER
        is FilaSuperAdmin.Solicitud -> TIPO_SOLICITUD
        is FilaSuperAdmin.Empresa -> TIPO_EMPRESA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TIPO_HEADER -> HeaderVH(ItemSeccionHeaderBinding.inflate(inflater, parent, false))
            TIPO_SOLICITUD -> SolicitudVH(ItemEmpresaPendienteBinding.inflate(inflater, parent, false))
            else -> EmpresaVH(ItemEmpresaGestionableBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val fila = getItem(position)) {
            is FilaSuperAdmin.Encabezado -> (holder as HeaderVH).bind(fila.titulo)
            is FilaSuperAdmin.Solicitud -> (holder as SolicitudVH).bind(fila.empresa)
            is FilaSuperAdmin.Empresa -> (holder as EmpresaVH).bind(fila.empresa)
        }
    }

    class HeaderVH(private val b: ItemSeccionHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(titulo: String) { b.titulo.text = titulo }
    }

    inner class SolicitudVH(private val b: ItemEmpresaPendienteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EmpresaPendienteResponse) {
            b.nombre.text = e.nombre.orEmpty()
            b.detalle.text = listOf(e.ubicacion.orEmpty(), e.contacto.orEmpty())
                .filter { it.isNotBlank() }.joinToString(" · ")
            b.fecha.text = "Solicitada el ${e.fechaRegistro?.format(FORMATO).orEmpty()}"
            b.chipVencida.visibility = if (e.vencida == true) View.VISIBLE else View.GONE
            b.botonAprobar.setOnClickListener { onAprobar(e) }
            b.botonRechazar.setOnClickListener { onRechazar(e) }
        }
    }

    inner class EmpresaVH(private val b: ItemEmpresaGestionableBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EmpresaResumenResponse) {
            b.nombre.text = e.nombre.orEmpty()
            b.detalle.text = listOf(e.ubicacion.orEmpty(), e.contacto.orEmpty())
                .filter { it.isNotBlank() }.joinToString(" · ")
            val suspendida = e.estado == "SUSPENDIDA"
            b.chipEstado.text = if (suspendida) "Suspendida" else "Activa"
            // Un solo botón según el estado: suspender una activa o reactivar una suspendida.
            b.boton.text = if (suspendida) "Reactivar" else "Suspender"
            b.boton.setOnClickListener { if (suspendida) onReactivar(e) else onSuspender(e) }
        }
    }

    companion object {
        private const val TIPO_HEADER = 0
        private const val TIPO_SOLICITUD = 1
        private const val TIPO_EMPRESA = 2
        private val FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        private val DIFF = object : DiffUtil.ItemCallback<FilaSuperAdmin>() {
            override fun areItemsTheSame(a: FilaSuperAdmin, b: FilaSuperAdmin) = when {
                a is FilaSuperAdmin.Encabezado && b is FilaSuperAdmin.Encabezado -> a.titulo == b.titulo
                a is FilaSuperAdmin.Solicitud && b is FilaSuperAdmin.Solicitud -> a.empresa.id == b.empresa.id
                a is FilaSuperAdmin.Empresa && b is FilaSuperAdmin.Empresa -> a.empresa.id == b.empresa.id
                else -> false
            }
            override fun areContentsTheSame(a: FilaSuperAdmin, b: FilaSuperAdmin) = a == b
        }
    }
}
