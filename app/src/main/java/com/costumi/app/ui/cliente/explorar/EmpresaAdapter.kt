package com.costumi.app.ui.cliente.explorar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.databinding.ItemEmpresaBinding
import com.costumi.app.ui.cargarFoto
import com.google.android.material.color.MaterialColors

/** Lista de tiendas del marketplace. Al tocar una, [alTocar] navega a su catalogo. */
class EmpresaAdapter(
    private val alTocar: (EmpresaEntity) -> Unit,
) : ListAdapter<EmpresaEntity, EmpresaAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemEmpresaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEmpresaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val empresa = getItem(position)
        holder.binding.nombre.text = empresa.nombre
        holder.binding.avatar.text = empresa.nombre.firstOrNull()?.uppercase() ?: "?"

        // Portada: si el backend trae portadaUrl, la imagen; si no, un color decorativo por tienda
        // (estable por id, adapta a claro/oscuro) para que la tarjeta tenga identidad sin inventar datos.
        val attr = FONDOS[Math.floorMod(empresa.id.hashCode(), FONDOS.size)]
        holder.binding.portada.setBackgroundColor(MaterialColors.getColor(holder.binding.portada, attr))

        // Logo real de la tienda cuando el backend lo dé; si no, la inicial (degrada bien).
        val tieneLogo = !empresa.logoUrl.isNullOrBlank()
        holder.binding.logo.isVisible = tieneLogo
        holder.binding.avatar.isVisible = !tieneLogo
        if (tieneLogo) holder.binding.logo.cargarFoto(empresa.logoUrl)

        // Ciudad/barrio: solo si el backend la trae.
        holder.binding.ciudad.isVisible = !empresa.ciudad.isNullOrBlank()
        holder.binding.ciudad.text = empresa.ciudad.orEmpty()

        holder.binding.root.setOnClickListener { alTocar(empresa) }
    }

    private companion object {
        /** Colores de portada (atributos del tema → adaptan a claro/oscuro). Se elige por id de la tienda. */
        val FONDOS = intArrayOf(
            com.google.android.material.R.attr.colorSecondaryContainer,
            com.google.android.material.R.attr.colorTertiaryContainer,
            com.google.android.material.R.attr.colorPrimaryContainer,
        )

        val DIFF = object : DiffUtil.ItemCallback<EmpresaEntity>() {
            override fun areItemsTheSame(a: EmpresaEntity, b: EmpresaEntity) = a.id == b.id
            override fun areContentsTheSame(a: EmpresaEntity, b: EmpresaEntity) = a == b
        }
    }
}
