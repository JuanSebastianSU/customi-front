package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.FotoEmpresaApi
import com.costumi.app.data.remote.MiEmpresaApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.models.EditarMiTiendaRequest
import com.costumi.apiclient.models.EmpresaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La propia tienda del usuario (RF-15.1). Se cachea en memoria: el nombre encabeza Gestion y no cambia
 * dentro de una sesion, asi que no tiene sentido volver a pedirlo cada vez que se abre el panel.
 */
@Singleton
class MiEmpresaRepository @Inject constructor(
    private val api: MiEmpresaApi,
    private val empresaApi: EmpresaControllerApi,
    private val fotoApi: FotoEmpresaApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    private var cache: EmpresaResponse? = null

    /** Edita los datos de la propia tienda (nombre/descripción/ciudad/ubicación/contacto). */
    suspend fun editar(
        nombre: String?, descripcion: String?, ciudad: String?, ubicacion: String?, contacto: String?,
    ): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) {
            empresaApi.editarMia(EditarMiTiendaRequest(
                nombre = nombre?.trim()?.ifBlank { null },
                descripcion = descripcion?.trim()?.ifBlank { null },
                ciudad = ciudad?.trim()?.ifBlank { null },
                ubicacion = ubicacion?.trim()?.ifBlank { null },
                contacto = contacto?.trim()?.ifBlank { null },
            ))
        }.also { r -> if (r is RespuestaRed.Exito) cache = r.data }
    }

    suspend fun mia(): RespuestaRed<EmpresaResponse> {
        cache?.let { return RespuestaRed.Exito(it) }
        return withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.mia() }.also { r ->
                if (r is RespuestaRed.Exito) cache = r.data
            }
        }
    }

    /** Fuerza recargar la tienda desde la red (tras cambiar logo/portada/nombre). */
    suspend fun refrescar(): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { api.mia() }.also { r -> if (r is RespuestaRed.Exito) cache = r.data }
    }

    /** Sube el logo de la tienda (multipart, part `archivo`). Actualiza el cache con la respuesta. */
    suspend fun subirLogo(bytes: ByteArray, mime: String, nombreArchivo: String): RespuestaRed<EmpresaResponse> =
        subir(nombreArchivo, bytes, mime) { parte -> fotoApi.subirLogo(parte) }

    /** Sube la portada de la tienda (multipart, part `archivo`). Actualiza el cache con la respuesta. */
    suspend fun subirPortada(bytes: ByteArray, mime: String, nombreArchivo: String): RespuestaRed<EmpresaResponse> =
        subir(nombreArchivo, bytes, mime) { parte -> fotoApi.subirPortada(parte) }

    private suspend fun subir(
        nombreArchivo: String,
        bytes: ByteArray,
        mime: String,
        llamada: suspend (MultipartBody.Part) -> retrofit2.Response<EmpresaResponse>,
    ): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val parte = MultipartBody.Part.createFormData("archivo", nombreArchivo, cuerpo)
        ejecutarLlamada(gson) { llamada(parte) }.also { r -> if (r is RespuestaRed.Exito) cache = r.data }
    }

    /** Al cerrar sesion la tienda deja de ser la misma: si no, el proximo dueno veria el nombre anterior. */
    fun limpiar() {
        cache = null
    }
}
