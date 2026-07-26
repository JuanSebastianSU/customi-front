package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.models.GuardarFavoritoRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disfraces guardados por el cliente ("Mis guardados"). **Sincronizados con la cuenta** (server) y
 * cacheados en Room para que la UI reaccione al instante y funcione offline:
 * - [observar]/[esFavorito] leen la cache (Room) — fuente reactiva de la UI.
 * - [alternar] escribe optimista en la cache y refleja en el server; si el server falla, revierte.
 * - [sincronizar] baja la lista del server y reemplaza la cache (al abrir "Mis guardados").
 */
@Singleton
class FavoritosRepository @Inject constructor(
    private val dao: FavoritoDao,
    private val api: ClienteControllerApi,
    private val dispatchers: DispatcherProvider,
) {
    fun observar(): Flow<List<FavoritoDisfrazEntity>> = dao.observarTodos()

    fun esFavorito(disfrazId: String): Flow<Boolean> = dao.esFavorito(disfrazId)

    /** Guarda o quita según [esFavorito] actual. Optimista en local + sync con el server (revierte si falla). */
    suspend fun alternar(favorito: FavoritoDisfrazEntity, esFavorito: Boolean) = withContext(dispatchers.io) {
        // 1) Refleja ya en la cache: el corazón reacciona sin esperar la red.
        if (esFavorito) dao.eliminar(favorito.disfrazId) else dao.guardar(favorito)
        // 2) Sincroniza con el server.
        val ok = runCatching {
            if (esFavorito) {
                api.quitarFavorito(UUID.fromString(favorito.disfrazId)).isSuccessful
            } else {
                api.guardarFavorito(
                    GuardarFavoritoRequest(
                        disfrazId = UUID.fromString(favorito.disfrazId),
                        empresaId = UUID.fromString(favorito.empresaId),
                        nombre = favorito.nombre,
                        fotoUrl = favorito.fotoUrl,
                        precioRenta = favorito.precioRenta?.toBigDecimalOrNull(),
                        precioVenta = favorito.precioVenta?.toBigDecimalOrNull(),
                    ),
                ).isSuccessful
            }
        }.getOrDefault(false)
        // 3) Si el server falló, revierte la cache para no mentirle al usuario.
        if (!ok) {
            if (esFavorito) dao.guardar(favorito) else dao.eliminar(favorito.disfrazId)
        }
    }

    /** Baja los favoritos del server y reemplaza la cache local (best-effort; si falla, deja la cache). */
    suspend fun sincronizar() = withContext(dispatchers.io) {
        val resp = runCatching { api.misFavoritos() }.getOrNull() ?: return@withContext
        if (!resp.isSuccessful) return@withContext
        val remotos = resp.body().orEmpty().mapNotNull { it.aEntity() }
        dao.limpiar()
        remotos.forEach { dao.guardar(it) }
    }
}
