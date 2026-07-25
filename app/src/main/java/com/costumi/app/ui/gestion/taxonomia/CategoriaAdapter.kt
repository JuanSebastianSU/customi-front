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
import com.costumi.apiclient.models.CategoriaResponse

/** Lista de categorías con menú (renombrar / archivar-activar). */
class CategoriaAdapter(
    private val alRenombrar: (CategoriaResponse) -> Unit,
    private val alAlternarArchivado: (CategoriaResponse) -> Unit,
) : ListAdapter<CategoriaResponse, CategoriaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCategoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(cat: CategoriaResponse) {
            binding.nombre.text = cat.nombre.orEmpty()
            val archivada = cat.archivada == true
            binding.chipArchivada.isVisible = archivada
            binding.root.alpha = if (archivada) 0.55f else 1f
            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, cat, archivada) }
        }

        private fun mostrarMenu(ancla: View, cat: CategoriaResponse, archivada: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_RENOMBRAR, 0, "Renombrar")
                menu.add(0, ID_ARCHIVAR, 1, if (archivada) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_RENOMBRAR -> alRenombrar(cat)
                        ID_ARCHIVAR -> alAlternarArchivado(cat)
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

        private val DIFF = object : DiffUtil.ItemCallback<CategoriaResponse>() {
            override fun areItemsTheSame(a: CategoriaResponse, b: CategoriaResponse) = a.id == b.id
            override fun areContentsTheSame(a: CategoriaResponse, b: CategoriaResponse) = a == b
        }
    }
}
