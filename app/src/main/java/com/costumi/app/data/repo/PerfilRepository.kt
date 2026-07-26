package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.PerfilDao
import com.costumi.app.data.local.entity.PerfilEntity
import com.costumi.app.data.remote.FotoPerfilApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.PerfilControllerApi
import com.costumi.apiclient.models.ActualizarPerfilRequest
import com.costumi.apiclient.models.CambiarContrasenaRequest
import com.costumi.apiclient.models.PerfilResponse
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
 * La propia cuenta (RF-14): ver y editar los datos, y cambiar la contrasena. Siempre opera sobre el
 * usuario del token, asi que no recibe ningun id.
 */
@Singleton
class PerfilRepository @Inject constructor(
    private val api: PerfilControllerApi,
    private val fotoApi: FotoPerfilApi,
    private val dao: PerfilDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** La UI observa esto (Room como fuente de verdad): al refrescar/editar y guardar, se actualiza sola. */
    fun observarPerfil(): Flow<PerfilResponse?> = dao.observar().map { it?.aResponse() }

    private suspend fun guardar(perfil: PerfilResponse) = dao.guardar(PerfilEntity(json = gson.toJson(perfil)))
    private fun PerfilEntity.aResponse(): PerfilResponse? =
        runCatching { gson.fromJson(json, PerfilResponse::class.java) }.getOrNull()

    /** Trae el perfil de la red y **escribe en Room** (los datos llegan por el Flow). Solo devuelve el error. */
    suspend fun refrescarPerfil(): RespuestaRed<Unit> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { api.ver() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> { guardar(r.data); RespuestaRed.Exito(Unit) }
        }
    }

    /** Borra el perfil cacheado (se llama al cerrar sesión, norma N1). */
    suspend fun limpiar() = withContext(dispatchers.io) { dao.limpiar() }

    suspend fun perfil(): RespuestaRed<PerfilResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.ver() }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
        }

    /** Sube la foto de perfil (multipart, part `archivo`). 503 si S3 no está, 415 si no es imagen. */
    suspend fun subirFoto(bytes: ByteArray, mime: String, nombreArchivo: String): RespuestaRed<PerfilResponse> =
        withContext(dispatchers.io) {
            val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val parte = MultipartBody.Part.createFormData("archivo", nombreArchivo, cuerpo)
            ejecutarLlamada(gson) { fotoApi.subirFoto(parte) }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
        }

    /** Vacio borra el dato: nombre y telefono son opcionales. */
    suspend fun actualizar(nombre: String?, telefono: String?): RespuestaRed<PerfilResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                api.actualizar(
                    ActualizarPerfilRequest(
                        nombre = nombre?.trim().orEmpty(),
                        telefono = telefono?.trim().orEmpty(),
                    ),
                )
            }.also { r -> if (r is RespuestaRed.Exito) guardar(r.data) }
        }

    /** El backend exige la contrasena actual aunque la sesion este abierta. */
    suspend fun cambiarContrasena(actual: String, nueva: String): RespuestaRed<Unit> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                api.cambiarContrasena(CambiarContrasenaRequest(contrasenaActual = actual, contrasenaNueva = nueva))
            }
        }
}
