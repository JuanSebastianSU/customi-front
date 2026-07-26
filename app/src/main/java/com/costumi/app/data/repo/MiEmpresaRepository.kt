package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.MiEmpresaDao
import com.costumi.app.data.local.entity.MiEmpresaEntity
import com.costumi.app.data.remote.FotoEmpresaApi
import com.costumi.app.data.remote.MiEmpresaApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.models.EditarMiTiendaRequest
import com.costumi.apiclient.models.EmpresaResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La propia tienda del usuario (RF-15.1). Se cachea en **Room** (`PLAN_ROOM_OFFLINE.md` A6): el nombre
 * encabeza Gestion y no cambia dentro de una sesion, y ahora aparece al instante incluso tras reiniciar la
 * app. Antes la caché era en memoria; se pasó a Room para tener **una sola fuente de verdad** (no dos).
 * La API pública (mia/refrescar/editar/subir/limpiar) no cambió: solo cambió dónde se guarda.
 */
@Singleton
class MiEmpresaRepository @Inject constructor(
    private val api: MiEmpresaApi,
    private val empresaApi: EmpresaControllerApi,
    private val fotoApi: FotoEmpresaApi,
    private val dao: MiEmpresaDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Observa la tienda cacheada (cache-first por Flow); null si aún no se guardó nada. */
    fun observar(): Flow<EmpresaResponse?> = dao.observar().map { it?.aResponse() }

    private suspend fun guardar(empresa: EmpresaResponse) = dao.guardar(MiEmpresaEntity(json = gson.toJson(empresa)))
    private fun MiEmpresaEntity.aResponse(): EmpresaResponse? =
        runCatching { gson.fromJson(json, EmpresaResponse::class.java) }.getOrNull()

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
        }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
    }

    /** Cache-first: si hay tienda guardada en Room la devuelve; si no, la pide a la red y la guarda. */
    suspend fun mia(): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        dao.leer()?.aResponse()?.let { return@withContext RespuestaRed.Exito(it) }
        ejecutarLlamada(gson) { api.mia() }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
    }

    /** Fuerza recargar la tienda desde la red (tras cambiar logo/portada/nombre). */
    suspend fun refrescar(): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { api.mia() }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
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
        ejecutarLlamada(gson) { llamada(parte) }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
    }

    /** Al cerrar sesion la tienda deja de ser la misma: si no, el proximo dueno veria el nombre anterior (N1). */
    suspend fun limpiar() = withContext(dispatchers.io) { dao.limpiar() }
}
