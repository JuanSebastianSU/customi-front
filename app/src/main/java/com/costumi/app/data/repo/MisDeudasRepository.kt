package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.DeudaDao
import com.costumi.app.data.local.entity.DeudaEntity
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.data.remote.MisDeudasApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multas y saldos del propio cliente, en todas las tiendas (RF-7/11.5).
 *
 * Cache-first (Room, `PLAN_ROOM_OFFLINE.md` A5): la UI observa [observarDeudas] (Room) y el repo sincroniza
 * con [refrescarDeudas]. Es informativo: el importe a cobrar se reconfirma contra el servidor antes de pagar
 * (N3). Se limpia al cerrar sesión (N1, en `AuthRepository`).
 */
@Singleton
class MisDeudasRepository @Inject constructor(
    private val api: MisDeudasApi,
    private val dao: DeudaDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** La UI observa esto (Room como fuente de verdad); reconstruye el DTO completo desde el JSON guardado. */
    fun observarDeudas(): Flow<List<MiDeudaDto>> =
        dao.observarTodas().map { lista -> lista.mapNotNull { it.aDto() } }

    /**
     * Trae las deudas desde la red y **escribe en Room** (los datos llegan por el Flow). Devuelve **cuántas**
     * trajo: el VM lo usa para distinguir "0 = no debes nada" de "cargando" (Room no re-emite si la tabla ya
     * estaba vacía, así que sin esto la pantalla se quedaría cargando para siempre).
     */
    suspend fun refrescarDeudas(): RespuestaRed<Int> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { api.mias() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                val entidades = r.data.mapIndexedNotNull { i, d -> d.aEntity(i) }
                dao.reemplazar(entidades)
                RespuestaRed.Exito(entidades.size)
            }
        }
    }

    /** Borra la caché de deudas (se llama al cerrar sesión, norma N1). */
    suspend fun limpiar() = withContext(dispatchers.io) { dao.limpiar() }

    suspend fun mias(): RespuestaRed<List<MiDeudaDto>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.mias() } }

    // --- Mapeadores DTO <-> Entity (JSON por fila; el adapter usa el DTO completo) ---

    /** null si la deuda no trae rentaId (sin clave primaria no se puede cachear ni diferenciar). */
    private fun MiDeudaDto.aEntity(orden: Int): DeudaEntity? {
        val id = rentaId?.toString() ?: return null
        return DeudaEntity(rentaId = id, orden = orden, json = gson.toJson(this))
    }

    private fun DeudaEntity.aDto(): MiDeudaDto? =
        runCatching { gson.fromJson(json, MiDeudaDto::class.java) }.getOrNull()
}
