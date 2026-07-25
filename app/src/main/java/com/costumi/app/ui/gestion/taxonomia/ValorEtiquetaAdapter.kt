package com.costumi.app.ui.gestion.taxonomia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemCategoriaBinding
import com.costumi.apiclient.models.ValorEtiquetaResponse

/** Lista de valores de un tipo de etiqueta (reusa el item de categoría: nombre + chip + menú). */
class ValorEtiquetaAdapter(
    private val alRenombrar: (ValorEtiquetaResponse) -> Unit,
    private val alAlternarArchivado: (ValorEtiquetaResponse) -> Unit,
) : ListAdapter<ValorEtiquetaResponse, ValorEtiquetaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCategoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(v: ValorEtiquetaResponse) {
            binding.nombre.text = v.valor.orEmpty()
            val archivada = v.archivada == true
            binding.chipArchivada.isVisible = archivada
            binding.root.alpha = if (archivada) 0.55f else 1f
            binding.botonAcciones.setOnClickListener { ancla -> mostrarMenu(ancla, v, archivada) }
        }

        private fun mostrarMenu(ancla: View, valor: ValorEtiquetaResponse, archivada: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_RENOMBRAR, 0, "Renombrar")
                menu.add(0, ID_ARCHIVAR, 1, if (archivada) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_RENOMBRAR -> alRenombrar(valor)
                        ID_ARCHIVAR -> alAlternarArchivado(valor)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object {
        private const val ID_RENOMBRAR = 1
        private const val ID_ARCHIVAR = 2

        private val DIFF = object : DiffUtil.ItemCallback<ValorEtiquetaResponse>() {
            override fun areItemsTheSame(a: ValorEtiquetaResponse, b: ValorEtiquetaResponse) = a.id == b.id
            override fun areContentsTheSame(a: ValorEtiquetaResponse, b: ValorEtiquetaResponse) = a == b
        }
    }
}
