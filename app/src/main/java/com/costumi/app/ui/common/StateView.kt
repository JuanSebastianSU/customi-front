package com.costumi.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.costumi.app.R
import com.costumi.app.databinding.ViewEstadoBinding

/**
 * Vista de estado reutilizable que se superpone al contenido (ver ORDEN_CONSTRUCCION §B): muestra
 * cargando, vacio, error (con Reintentar) o sin conexion. Toda pantalla la usa para no parecer
 * congelada. En Success se oculta con [ocultar] para dejar ver el contenido real.
 */
class StateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    private val binding = ViewEstadoBinding.inflate(LayoutInflater.from(context), this)

    fun cargando() {
        isVisible = true
        binding.progresoEstado.isVisible = true
        binding.contenedorMensaje.isVisible = false
    }

    fun vacio(mensaje: String, @DrawableRes icono: Int = R.drawable.ic_estado_vacio) {
        mostrarMensaje(mensaje, icono, textoBoton = null, accion = null)
    }

    fun error(mensaje: String, reintentar: (() -> Unit)? = null) {
        mostrarMensaje(mensaje, R.drawable.ic_estado_error, reintentar?.let { "Reintentar" }, reintentar)
    }

    fun sinConexion(
        mensaje: String = "Sin conexion. Revisa tu internet e intenta de nuevo.",
        reintentar: (() -> Unit)? = null,
    ) {
        mostrarMensaje(mensaje, R.drawable.ic_estado_sin_conexion, reintentar?.let { "Reintentar" }, reintentar)
    }

    fun ocultar() {
        isVisible = false
    }

    private fun mostrarMensaje(
        mensaje: String,
        @DrawableRes icono: Int,
        textoBoton: String?,
        accion: (() -> Unit)?,
    ) {
        isVisible = true
        binding.progresoEstado.isVisible = false
        binding.contenedorMensaje.isVisible = true
        binding.iconoEstado.setImageResource(icono)
        binding.textoEstado.text = mensaje
        if (textoBoton != null && accion != null) {
            binding.botonReintentar.isVisible = true
            binding.botonReintentar.text = textoBoton
            binding.botonReintentar.setOnClickListener { accion() }
        } else {
            binding.botonReintentar.isVisible = false
            binding.botonReintentar.setOnClickListener(null)
        }
    }
}
