package com.costumi.app.ui

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.costumi.app.R

/**
 * Carga una foto remota en el ImageView con Coil (crossfade + placeholder). Si [url] es nula/vacía
 * deja el placeholder. Centraliza el uso de Coil para que los adapters no lo repitan.
 */
fun ImageView.cargarFoto(url: String?) {
    load(url?.takeIf { it.isNotBlank() }) {
        crossfade(true)
        placeholder(R.drawable.ic_imagen_placeholder)
        error(R.drawable.ic_imagen_placeholder)
    }
}
