package com.costumi.app.ui.gestion.taxonomia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemTipoEtiquetaBinding
import com.costumi.apiclient.models.TipoEtiquetaResponse

/** Lista de tipos de etiqueta: flags (variante / cliente elige), menú y navegación a sus valores. */
class TipoEtiquetaAdapter(
    private val alAbrirValores: (TipoEtiquetaResponse) -> Unit,
    private val alRenombrar: (TipoEtiquetaResponse) -> Unit,
    private val alAlternarArchivado: (TipoEtiquetaResponse) -> Unit,
) : ListAdapter<TipoEtiquetaResponse, TipoEtiquetaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTipoEtiquetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemTipoEtiquetaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(tipo: TipoEtiquetaResponse) {
            binding.nombre.text = tipo.nombre.orEmpty()
            val archivada = tipo.archivada == true
            binding.chipArchivada.isVisible = archivada
            binding.root.alpha = if (archivada) 0.55f else 1f
            binding.flags.text = flagsDe(tipo)

            binding.root.setOnClickListener { alAbrirValores(tipo) }
            binding.botonAcciones.setOnClickListener { v -> mostrarMenu(v, tipo, archivada) }
        }

        /** Los flags técnicos, en cristiano: qué implica cada uno para el dueño. */
        private fun flagsDe(t: TipoEtiquetaResponse): String {
            val partes = buildList {
                if (t.defineVariante == true) add("Separa el stock")
                if (t.seleccionablePorCliente == true) add("El cliente filtra por esta")
            }
            return if (partes.isEmpty()) "Solo para clasificar" else partes.joinToString("  ·  ")
        }

        private fun mostrarMenu(ancla: View, tipo: TipoEtiquetaResponse, archivada: Boolean) {
            PopupMenu(ancla.context, ancla).apply {
                menu.add(0, ID_VALORES, 0, "Ver valores")
                menu.add(0, ID_RENOMBRAR, 1, "Renombrar")
                menu.add(0, ID_ARCHIVAR, 2, if (archivada) "Activar" else "Archivar")
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        ID_VALORES -> alAbrirValores(tipo)
                        ID_RENOMBRAR -> alRenombrar(tipo)
                        ID_ARCHIVAR -> alAlternarArchivado(tipo)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object {
        private const val ID_VALORES = 1
        private const val ID_RENOMBRAR = 2
        private const val ID_ARCHIVAR = 3

        private val DIFF = object : DiffUtil.ItemCallback<TipoEtiquetaResponse>() {
            override fun areItemsTheSame(a: TipoEtiquetaResponse, b: TipoEtiquetaResponse) = a.id == b.id
            override fun areContentsTheSame(a: TipoEtiquetaResponse, b: TipoEtiquetaResponse) = a == b
        }
    }
}
