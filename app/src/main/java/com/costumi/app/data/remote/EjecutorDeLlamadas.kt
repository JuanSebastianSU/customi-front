package com.costumi.app.data.remote

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.apiclient.models.ProblemDetail
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Envuelve una llamada Retrofit (`suspend () -> Response<T>`) y devuelve un [RespuestaRed] ya
 * mapeado: parsea el ProblemDetail (RFC 7807) en los errores y distingue la falta de conexion.
 * Todo repositorio pasa por aqui, asi el manejo de errores es uniforme.
 */
suspend fun <T> ejecutarLlamada(
    gson: Gson,
    llamada: suspend () -> Response<T>,
): RespuestaRed<T> = try {
    val respuesta = llamada()
    if (respuesta.isSuccessful) {
        @Suppress("UNCHECKED_CAST")
        val cuerpo = respuesta.body() ?: (Unit as T) // endpoints que devuelven Response<Unit>
        RespuestaRed.Exito(cuerpo)
    } else {
        RespuestaRed.Fallo(mapearError(gson, respuesta))
    }
} catch (_: IOException) {
    RespuestaRed.Fallo(
        ErrorApi(TipoError.SIN_CONEXION, "Sin conexion. Revisa tu internet e intenta de nuevo."),
    )
} catch (e: Exception) {
    RespuestaRed.Fallo(
        ErrorApi(TipoError.DESCONOCIDO, e.message ?: "Ocurrio un error inesperado."),
    )
}

private fun <T> mapearError(gson: Gson, respuesta: Response<T>): ErrorApi {
    val codigo = respuesta.code()
    val problema: ProblemDetail? = try {
        respuesta.errorBody()?.charStream()?.use { gson.fromJson(it, ProblemDetail::class.java) }
    } catch (_: Exception) {
        null
    }
    val mensaje = problema?.detail?.takeIf { it.isNotBlank() }
        ?: problema?.title?.takeIf { it.isNotBlank() }
        ?: mensajePorCodigo(codigo)
    return ErrorApi(tipoPorCodigo(codigo), mensaje, codigo, problema?.title)
}

private fun tipoPorCodigo(codigo: Int): TipoError = when (codigo) {
    400 -> TipoError.VALIDACION
    401 -> TipoError.NO_AUTORIZADO
    403 -> TipoError.SIN_PERMISO
    404 -> TipoError.NO_ENCONTRADO
    409 -> TipoError.CONFLICTO
    415 -> TipoError.NO_SOPORTADO
    429 -> TipoError.LIMITE_TASA
    503 -> TipoError.NO_CONFIGURADO
    in 500..599 -> TipoError.SERVIDOR
    else -> TipoError.DESCONOCIDO
}

private fun mensajePorCodigo(codigo: Int): String = when (codigo) {
    400 -> "Datos invalidos. Revisa la informacion e intenta de nuevo."
    401 -> "Tu sesion expiro. Inicia sesion de nuevo."
    403 -> "No tienes permiso para esta accion."
    404 -> "No se encontro lo solicitado."
    409 -> "La operacion no se puede completar en este momento."
    415 -> "Formato no soportado."
    429 -> "Demasiados intentos. Espera un momento e intenta de nuevo."
    503 -> "Esta funcion aun no esta disponible."
    in 500..599 -> "Error del servidor. Intenta mas tarde."
    else -> "Ocurrio un error inesperado."
}
