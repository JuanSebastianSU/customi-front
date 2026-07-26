package com.costumi.app.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.costumi.app.core.UiState
import com.costumi.app.push.Notificaciones
import com.costumi.app.ui.common.StateView
import com.google.android.material.materialswitch.MaterialSwitch
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
 * Lee los bytes de un `content://` (imagen elegida en un selector) de forma **segura**: devuelve
 * `(bytes, mime)` o `null` si no se pudo, **sin crashear nunca**. Corre en IO.
 *
 * Por qué el try/catch importa: el permiso de lectura que otorga el selector es temporal y **se pierde si
 * el sistema mata el proceso de la app mientras el selector está abierto** (frecuente en teléfonos con poca
 * RAM: el selector es otra app y empuja a Costumi fuera de memoria). Sin capturar, `openInputStream`
 * lanzaba `SecurityException` al volver y la app se cerraba —parecía que "se salía del formulario"—.
 */
suspend fun Context.leerBytesDeImagen(uri: Uri): Pair<ByteArray, String>? =
    withContext(Dispatchers.IO) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            bytes to (contentResolver.getType(uri) ?: "image/*")
        } catch (e: Exception) {
            null
        }
    }

/** Extensión de archivo a partir del mime de imagen (jpg por defecto). */
fun extensionDeImagen(mime: String?): String = when (mime) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}

/**
 * Request del **selector de imágenes moderno** (Android Photo Picker, `PickVisualMedia`, solo imágenes).
 * Se prefiere sobre `GetContent`: no pide permisos de almacenamiento, es el selector recomendado y molesta
 * menos a la app (menos probable que el sistema la mate mientras el selector está abierto).
 */
val soloImagenes: PickVisualMediaRequest
    get() = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

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
 * Estado real de las push en ESTE dispositivo: el usuario las quiere (preferencia local) Y el sistema
 * las permite (permiso concedido + no silenciadas). El toggle de la pantalla se pinta con esto.
 */
fun Fragment.notificacionesActivas(): Boolean {
    val ctx = requireContext()
    return Notificaciones.activadasPorUsuario(ctx) &&
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}

/** ¿Está concedido POST_NOTIFICATIONS? En Android < 13 no hace falta pedirlo, así que siempre sí. */
fun Fragment.tienePermisoNotificaciones(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

/** Pone el switch en el estado real (sin disparar su listener; se usa setOnClickListener aparte). */
fun MaterialSwitch.reflejarNotificaciones(activas: Boolean) {
    isChecked = activas
}

/**
 * El launcher del permiso (Android 13+). Regístralo como campo del Fragment:
 * `private val pedirPermisoNotif = registrarPermisoNotificaciones { concedido -> ... }`.
 */
fun Fragment.registrarPermisoNotificaciones(
    alResponder: (concedido: Boolean) -> Unit,
): ActivityResultLauncher<String> =
    registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { concedido -> alResponder(concedido) }

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

