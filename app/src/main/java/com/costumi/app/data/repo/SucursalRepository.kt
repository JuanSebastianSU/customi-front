package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.FotoSucursalApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.EditarSucursalRequest
import com.costumi.apiclient.models.RegistrarSucursalRequest
import com.costumi.apiclient.models.SucursalResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sucursales de la empresa (RF-15.4/12.4): listar, alta, editar y archivar/activar. */
@Singleton
class SucursalRepository @Inject constructor(
    private val sucursalApi: SucursalControllerApi,
    private val fotoApi: FotoSucursalApi,
    private val authApi: AuthControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    private suspend fun empresaId(): RespuestaRed<UUID> = when (val me = ejecutarLlamada(gson) { authApi.me() }) {
        is RespuestaRed.Fallo -> me
        is RespuestaRed.Exito -> {
            val id = me.data.empresaId
            if (id.isNullOrBlank()) RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tu usuario no tiene empresa asignada."))
            else RespuestaRed.Exito(UUID.fromString(id))
        }
    }

    suspend fun sucursales(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val emp = empresaId()) {
            is RespuestaRed.Fallo -> emp
            is RespuestaRed.Exito -> ejecutarLlamada(gson) { sucursalApi.listar9(emp.data) }
        }
    }

    suspend fun crear(nombre: String, direccion: String?, ubicacionMaps: String?): RespuestaRed<SucursalResponse> =
        conEmpresa { emp ->
            sucursalApi.registrar3(emp, RegistrarSucursalRequest(nombre, direccion?.ifBlank { null },
                ubicacionMaps?.ifBlank { null }))
        }

    suspend fun editar(id: UUID, nombre: String, direccion: String?, ubicacionMaps: String?): RespuestaRed<SucursalResponse> =
        conEmpresa { emp ->
            sucursalApi.editar3(emp, id, EditarSucursalRequest(nombre, direccion?.ifBlank { null },
                ubicacionMaps?.ifBlank { null }))
        }

    /** Sube la foto de la sucursal (multipart, part `archivo`). 503 si S3 no está, 415 si no es imagen. */
    suspend fun subirFoto(id: UUID, bytes: ByteArray, mime: String, nombreArchivo: String): RespuestaRed<SucursalResponse> =
        conEmpresa { emp ->
            val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val parte = MultipartBody.Part.createFormData("archivo", nombreArchivo, cuerpo)
            fotoApi.subirFoto(emp, id, parte)
        }

    suspend fun archivar(id: UUID): RespuestaRed<SucursalResponse> = conEmpresa { emp -> sucursalApi.archivar1(emp, id) }

    suspend fun activar(id: UUID): RespuestaRed<SucursalResponse> = conEmpresa { emp -> sucursalApi.activar1(emp, id) }

    private suspend fun <T> conEmpresa(accion: suspend (UUID) -> retrofit2.Response<T>): RespuestaRed<T> =
        withContext(dispatchers.io) {
            when (val emp = empresaId()) {
                is RespuestaRed.Fallo -> emp
                is RespuestaRed.Exito -> ejecutarLlamada(gson) { accion(emp.data) }
            }
        }
}
