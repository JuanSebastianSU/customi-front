package com.costumi.app.ui.gestion.inventario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemGrupoStockBinding
import com.costumi.apiclient.models.GrupoDeStockResponse
import java.util.UUID

/** Lista de grupos de stock de una prenda: conteo por estado y menú de operaciones. */
class GrupoStockAdapter(
    private val nombreSucursal: (UUID?) -> String,
    private val describirVariante: (GrupoDeStockResponse) -> String,
    private val alAbrirAcciones: (GrupoDeStockResponse, View) -> Unit,
) : ListAdapter<GrupoDeStockResponse, GrupoStockAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGrupoStockBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemGrupoStockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(g: GrupoDeStockResponse) {
            // La variante manda (es lo que distingue un grupo de otro); la sucursal, debajo.
            binding.sucursal.text = describirVariante(g)
            val rentadas = g.rentadas ?: 0
            val rentTxt = if (rentadas > 0) "  ·  Rentadas $rentadas" else ""
            binding.variante.text = "${nombreSucursal(g.sucursalId)}  ·  Total ${g.total ?: 0}$rentTxt"
            binding.disponibles.text = (g.disponibles ?: 0).toString()
            binding.danadas.text = (g.danadas ?: 0).toString()
            binding.limpieza.text = (g.enLimpieza ?: 0).toString()
            binding.perdidas.text = (g.perdidas ?: 0).toString()
            binding.botonAcciones.setOnClickListener { v -> alAbrirAcciones(g, v) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GrupoDeStockResponse>() {
            override fun areItemsTheSame(a: GrupoDeStockResponse, b: GrupoDeStockResponse) = a.id == b.id
            override fun areContentsTheSame(a: GrupoDeStockResponse, b: GrupoDeStockResponse) = a == b
        }
    }
}
