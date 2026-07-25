package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disfraces guardados por el cliente ("Mis guardados"). Persistencia **local** (Room): funciona ya, sin
 * backend. Sincronizarlos con la cuenta entre dispositivos es el paso de backend (ver PROGRESS.md).
 */
@Singleton
class FavoritosRepository @Inject constructor(
    private val dao: FavoritoDao,
    private val dispatchers: DispatcherProvider,
) {
    fun observar(): Flow<List<FavoritoDisfrazEntity>> = dao.observarTodos()

    fun esFavorito(disfrazId: String): Flow<Boolean> = dao.esFavorito(disfrazId)

    /** Guarda o quita según [esFavorito] actual (el que el corazón está mostrando). */
    suspend fun alternar(favorito: FavoritoDisfrazEntity, esFavorito: Boolean) =
        withContext(dispatchers.io) {
            if (esFavorito) dao.eliminar(favorito.disfrazId) else dao.guardar(favorito)
        }
}
