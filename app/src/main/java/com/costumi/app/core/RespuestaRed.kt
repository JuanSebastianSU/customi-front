package com.costumi.app.core

/** Categoria de error segun el codigo HTTP, para que la UI reaccione distinto (ver §1 del diseno). */
enum class TipoError {
    VALIDACION,     // 400
    NO_AUTORIZADO,  // 401
    SIN_PERMISO,    // 403 (rol / piramide / sucursal)
    NO_ENCONTRADO,  // 404 (o de otra empresa)
    CONFLICTO,      // 409 (lista negra, cliente archivado, sin stock, item no devuelto, ...)
    NO_SOPORTADO,   // 415 (imagen no soportada)
    LIMITE_TASA,    // 429 (demasiados intentos)
    NO_CONFIGURADO, // 503 (S3 / pasarela sin credenciales)
    SIN_CONEXION,   // sin red
    SERVIDOR,       // 5xx
    DESCONOCIDO,
}

/**
 * Error de una llamada ya traducido a algo mostrable: [mensaje] sale del `detail` del ProblemDetail
 * (RFC 7807), que el backend ya devuelve en espanol.
 */
data class ErrorApi(
    val tipo: TipoError,
    val mensaje: String,
    val httpCode: Int? = null,
    val title: String? = null,
)

/** Resultado tipado de una llamada de red: exito con datos, o fallo ya mapeado. */
sealed interface RespuestaRed<out T> {
    data class Exito<T>(val data: T) : RespuestaRed<T>
    data class Fallo(val error: ErrorApi) : RespuestaRed<Nothing>
}

/**
 * Transforma el dato de una respuesta exitosa; un fallo pasa tal cual. Sirve, por ejemplo, para quedarse
 * con el `contenido` de una respuesta paginada sin repetir el `when` en cada repositorio.
 */
inline fun <T, R> RespuestaRed<T>.mapear(transformar: (T) -> R): RespuestaRed<R> = when (this) {
    is RespuestaRed.Exito -> RespuestaRed.Exito(transformar(data))
    is RespuestaRed.Fallo -> this
}
