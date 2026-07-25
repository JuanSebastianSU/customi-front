package com.costumi.app.ui.gestion.plantillas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.ItemPlantillaBinding
import com.costumi.apiclient.models.PlantillaResponse

/** Lista de mensajes automaticos: cada fila con su etiqueta, un preview del texto y el switch on/off. */
class PlantillaAdapter(
    private val onEditar: (PlantillaResponse) -> Unit,
    private val onToggle: (PlantillaResponse, Boolean) -> Unit,
) : ListAdapter<PlantillaResponse, PlantillaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPlantillaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemPlantillaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(p: PlantillaResponse) {
            val meta = CategoriaMensaje.de(p.tipo)
            binding.titulo.text = meta.etiqueta
            binding.descripcion.text = meta.descripcion
            binding.preview.text = p.texto.orEmpty()
            // Evita que el listener dispare al reciclar/enlazar.
            binding.switchActiva.setOnCheckedChangeListener(null)
            binding.switchActiva.isChecked = p.activa == true
            binding.switchActiva.setOnCheckedChangeListener { _, checked ->
                if (checked != (p.activa == true)) onToggle(p, checked)
            }
            binding.botonEditar.setOnClickListener { onEditar(p) }
            binding.root.setOnClickListener { onEditar(p) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PlantillaResponse>() {
            override fun areItemsTheSame(a: PlantillaResponse, b: PlantillaResponse) = a.tipo == b.tipo
            override fun areContentsTheSame(a: PlantillaResponse, b: PlantillaResponse) = a == b
        }
    }
}
