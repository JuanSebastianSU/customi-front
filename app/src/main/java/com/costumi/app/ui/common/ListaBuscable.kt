package com.costumi.app.ui.common

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.databinding.DialogListaBuscableBinding
import com.costumi.app.databinding.ItemSeleccionBuscableBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Una opción de una lista buscable. [id] identifica la opción; [subtitulo] es info secundaria (stock, etc.);
 * [habilitada] en false la muestra atenuada y no deja elegirla (ej. una prenda sin stock).
 */
data class OpcionBuscable(
    val id: String,
    val titulo: String,
    val subtitulo: String? = null,
    val habilitada: Boolean = true,
)

/**
 * Listas de selección **buscables**: sirven cuando hay demasiados elementos para una fila de chips o una
 * lista plana (categorías, prendas del inventario…). El diálogo trae un buscador arriba y filtra en vivo.
 */
object ListaBuscable {

    /** Elegir UNA opción (ej. categoría). Llama a [alElegir] con el id, o null si eligió la opción "todas". */
    fun unaOpcion(
        context: Context,
        titulo: String,
        opciones: List<OpcionBuscable>,
        seleccionadoId: String?,
        alElegir: (String?) -> Unit,
    ) {
        val binding = DialogListaBuscableBinding.inflate(LayoutInflater.from(context))
        lateinit var dialogo: androidx.appcompat.app.AlertDialog
        val adapter = SeleccionAdapter(opciones, multiple = false, seleccionados = setOfNotNull(seleccionadoId)) {
            alElegir(it.firstOrNull())
            dialogo.dismiss()
        }
        montar(binding, adapter)
        dialogo = MaterialAlertDialogBuilder(context)
            .setTitle(titulo)
            .setView(binding.root)
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Elegir VARIAS opciones (ej. los elementos de una parte del disfraz). [alListo] recibe los ids marcados.
     * [accionNeutra] agrega un botón extra (ej. "Filtros").
     */
    fun variasOpciones(
        context: Context,
        titulo: String,
        opciones: List<OpcionBuscable>,
        seleccionados: Set<String>,
        textoNeutro: String? = null,
        alNeutro: ((Set<String>) -> Unit)? = null,
        alListo: (Set<String>) -> Unit,
    ) {
        val binding = DialogListaBuscableBinding.inflate(LayoutInflater.from(context))
        val adapter = SeleccionAdapter(opciones, multiple = true, seleccionados = seleccionados) {}
        montar(binding, adapter)
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(titulo)
            .setView(binding.root)
            .setPositiveButton("Listo") { _, _ -> alListo(adapter.marcados()) }
            .setNegativeButton("Cancelar", null)
        if (textoNeutro != null && alNeutro != null) {
            builder.setNeutralButton(textoNeutro) { _, _ -> alNeutro(adapter.marcados()) }
        }
        builder.show()
    }

    private fun montar(binding: DialogListaBuscableBinding, adapter: SeleccionAdapter) {
        binding.lista.adapter = adapter
        binding.vacio.isVisible = adapter.itemCount == 0
        binding.editBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filtrar(s?.toString().orEmpty())
                binding.vacio.isVisible = adapter.itemCount == 0
            }

            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        })
    }

    private class SeleccionAdapter(
        private val todas: List<OpcionBuscable>,
        private val multiple: Boolean,
        seleccionados: Set<String>,
        private val alElegirUna: (Set<String>) -> Unit,
    ) : RecyclerView.Adapter<SeleccionAdapter.VH>() {

        private val marcados = seleccionados.toMutableSet()
        private var visibles = todas

        fun marcados(): Set<String> = marcados.toSet()

        fun filtrar(texto: String) {
            val q = texto.trim().lowercase()
            visibles = if (q.isEmpty()) {
                todas
            } else {
                todas.filter { it.titulo.lowercase().contains(q) || it.subtitulo?.lowercase()?.contains(q) == true }
            }
            notifyDataSetChanged()
        }

        override fun getItemCount() = visibles.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemSeleccionBuscableBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val op = visibles[position]
            holder.binding.titulo.text = op.titulo
            holder.binding.subtitulo.isVisible = !op.subtitulo.isNullOrBlank()
            holder.binding.subtitulo.text = op.subtitulo.orEmpty()
            holder.binding.check.isChecked = op.id in marcados
            holder.binding.root.alpha = if (op.habilitada) 1f else 0.45f
            holder.binding.root.isEnabled = op.habilitada
            holder.binding.root.setOnClickListener(
                if (!op.habilitada) {
                    null
                } else {
                    android.view.View.OnClickListener {
                        if (multiple) {
                            if (!marcados.add(op.id)) marcados.remove(op.id)
                            notifyItemChanged(position)
                        } else {
                            marcados.clear()
                            marcados.add(op.id)
                            alElegirUna(marcados)
                        }
                    }
                },
            )
        }

        class VH(val binding: ItemSeleccionBuscableBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
