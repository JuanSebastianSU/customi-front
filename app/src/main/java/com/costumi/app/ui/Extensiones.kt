package com.costumi.app.ui

import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.costumi.app.core.UiState
import com.costumi.app.ui.common.StateView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Colecta un flow de forma segura por ciclo de vida (STARTED) atado a la vista del fragment. */
fun <T> Fragment.observar(flujo: Flow<T>, accion: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flujo.collect { accion(it) }
        }
    }
}

/** Snackbar corto de conveniencia. */
fun Fragment.mostrarMensaje(mensaje: String) {
    view?.let { Snackbar.make(it, mensaje, Snackbar.LENGTH_LONG).show() }
}

/**
 * Si las notificaciones del sistema están DESACTIVADAS para la app, ofrece abrir los ajustes para
 * activarlas. Android no vuelve a pedir el permiso una vez rechazado (es por dispositivo, no por cuenta),
 * así que la única salida es que el usuario lo active a mano: este aviso lo lleva directo a esa pantalla.
 */
fun Fragment.ofrecerActivarNotificaciones() {
    val vista = view ?: return
    val ctx = requireContext()
    if (androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
    Snackbar.make(vista, "Las notificaciones estan desactivadas para Costumi.", Snackbar.LENGTH_LONG)
        .setAction("Activar") {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            runCatching { startActivity(intent) }.onFailure {
                // Fallback: la pantalla de detalles de la app.
                runCatching {
                    startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(android.net.Uri.fromParts("package", ctx.packageName, null)),
                    )
                }
            }
        }
        .show()
}

/**
 * Renderiza un [UiState] sobre este StateView: muestra cargando/vacio/error y, cuando hay datos,
 * se oculta y entrega los datos a [alTenerDatos] (para poblar la lista/contenido real).
 */
inline fun <T> StateView.mostrar(
    estado: UiState<T>,
    vacio: String = "No hay nada por aqui todavia.",
    alTenerDatos: (T) -> Unit,
) {
    when (estado) {
        is UiState.Loading -> cargando()
        is UiState.Empty -> vacio(vacio)
        is UiState.Error -> error(estado.mensaje, estado.reintentar)
        is UiState.Success -> {
            ocultar()
            alTenerDatos(estado.data)
        }
    }
}

/**
 * Engancha una caja de busqueda: llama a [accion] cuando el usuario deja de escribir, no en cada tecla.
 * Sin el retardo, escribir "camisa" dispararia seis consultas y la lista parpadearia.
 */
/** Reacciona a cada tecla, sin retardo: para formularios que recalculan un total mientras se escribe. */
fun android.widget.EditText.alEscribir(accion: (String) -> Unit) {
    doAfterTextChanged { texto -> accion(texto?.toString().orEmpty()) }
}

fun android.widget.EditText.alBuscar(retardoMs: Long = 350, accion: (String) -> Unit) {
    var pendiente: Runnable? = null
    doAfterTextChanged { texto ->
        pendiente?.let { removeCallbacks(it) }
        val tarea = Runnable { accion(texto?.toString().orEmpty()) }
        pendiente = tarea
        postDelayed(tarea, retardoMs)
    }
}

