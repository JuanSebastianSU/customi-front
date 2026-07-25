package com.costumi.app.ui.gestion.inventario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemLoadStateBinding

/** Pie de la lista paginada: muestra el spinner al cargar más y un "Reintentar" si falla. */
class PrendasLoadStateAdapter(
    private val reintentar: () -> Unit,
) : LoadStateAdapter<PrendasLoadStateAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): VH {
        val binding = ItemLoadStateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return VH(binding, reintentar)
    }

    override fun onBindViewHolder(holder: VH, loadState: LoadState) = holder.enlazar(loadState)

    class VH(
        private val binding: ItemLoadStateBinding,
        reintentar: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.botonReintentar.setOnClickListener { reintentar() }
        }

        fun enlazar(loadState: LoadState) {
            binding.progreso.isVisible = loadState is LoadState.Loading
            val esError = loadState is LoadState.Error
            binding.botonReintentar.isVisible = esError
            binding.textoError.isVisible = esError
            if (loadState is LoadState.Error) {
                binding.textoError.text = loadState.error.localizedMessage ?: "No se pudo cargar mas."
            }
        }
    }
}
