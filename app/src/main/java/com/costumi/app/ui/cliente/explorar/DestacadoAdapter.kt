package com.costumi.app.ui.cliente.explorar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemDestacadoBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.apiclient.models.DisfrazDestacadoResponse

/** Carrusel horizontal de disfraces destacados del marketplace. Al tocar, abre su detalle. */
class DestacadoAdapter(
    private val alTocar: (DisfrazDestacadoResponse) -> Unit,
) : ListAdapter<DisfrazDestacadoResponse, DestacadoAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemDestacadoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDestacadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = getItem(position)
        holder.binding.foto.cargarFoto(d.fotoUrl)
        holder.binding.nombre.text = d.nombre ?: "Disfraz"
        holder.binding.tienda.text = d.empresaNombre.orEmpty()
        holder.binding.root.setOnClickListener { alTocar(d) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DisfrazDestacadoResponse>() {
            override fun areItemsTheSame(a: DisfrazDestacadoResponse, b: DisfrazDestacadoResponse) =
                a.disfrazId == b.disfrazId && a.empresaId == b.empresaId
            override fun areContentsTheSame(a: DisfrazDestacadoResponse, b: DisfrazDestacadoResponse) = a == b
        }
    }
}
