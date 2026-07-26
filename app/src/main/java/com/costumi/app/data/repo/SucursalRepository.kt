package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.local.dao.SucursalDao
import com.costumi.app.data.local.entity.SucursalEntity
import com.costumi.app.data.remote.FotoSucursalApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.EditarSucursalRequest
import com.costumi.apiclient.models.RegistrarSucursalRequest
import com.costumi.apiclient.models.SucursalResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sucursales de la empresa (RF-15.4/12.4): listar, alta, editar y archivar/activar.
 *
 * Cache-first (Room, `PLAN_ROOM_OFFLINE.md` A3): la UI observa [observarSucursales] (Room) y el repo
 * refresca de la red con [refrescarSucursales], que escribe en Room. Solo se cachea lo que DESCRIBE; el
 * `sucursalDao` se limpia al cerrar sesión (norma N1, ver AuthRepository).
 */
@Singleton
class SucursalRepository @Inject constructor(
    private val sucursalApi: SucursalControllerApi,
    private val fotoApi: FotoSucursalApi,
    private val authApi: AuthControllerApi,
    private val sucursalDao: SucursalDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** La UI observa esto (Room como fuente de verdad); mapea la entidad al modelo que pinta la pantalla. */
    fun observarSucursales(): Flow<List<SucursalResponse>> =
        sucursalDao.observarTodas().map { lista -> lista.map { it.aResponse() } }

    /** Trae de la red y **escribe en Room** (los datos llegan por el Flow). Devuelve solo el error si lo hay. */
    suspend fun refrescarSucursales(): RespuestaRed<Unit> = withContext(dispatchers.io) {
        when (val r = sucursales()) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                sucursalDao.reemplazar(r.data.mapNotNull { it.aEntity() })
                RespuestaRed.Exito(Unit)
            }
        }
    }

    /** Borra la caché de sucursales (se llama al cerrar sesión). */
    suspend fun limpiarCache() = withContext(dispatchers.io) { sucursalDao.limpiar() }
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

    // --- Mapeadores DTO <-> Entity (UUID como texto, sin TypeConverters) ---

    /** null si la sucursal no trae id (no se puede cachear sin clave primaria). */
    private fun SucursalResponse.aEntity(): SucursalEntity? {
        val idTexto = id?.toString() ?: return null
        return SucursalEntity(
            id = idTexto,
            empresaId = empresaId?.toString(),
            nombre = nombre,
            direccion = direccion,
            ubicacionMaps = ubicacionMaps,
            descripcion = descripcion,
            fotoUrl = fotoUrl,
            latitud = latitud,
            longitud = longitud,
            archivada = archivada,
        )
    }

    private fun SucursalEntity.aResponse(): SucursalResponse = SucursalResponse(
        id = UUID.fromString(id),
        empresaId = empresaId?.let { UUID.fromString(it) },
        nombre = nombre,
        direccion = direccion,
        ubicacionMaps = ubicacionMaps,
        archivada = archivada,
        descripcion = descripcion,
        latitud = latitud,
        longitud = longitud,
        fotoUrl = fotoUrl,
    )
}
