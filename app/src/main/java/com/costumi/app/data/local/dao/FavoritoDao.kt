package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {

    /** "Mis guardados": la UI observa este Flow, lo más reciente primero. */
    @Query("SELECT * FROM favorito_disfraz ORDER BY guardadoEn DESC")
    fun observarTodos(): Flow<List<FavoritoDisfrazEntity>>

    /** true/false reactivo de si un disfraz está guardado (para pintar el corazón). */
    @Query("SELECT EXISTS(SELECT 1 FROM favorito_disfraz WHERE disfrazId = :disfrazId)")
    fun esFavorito(disfrazId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(favorito: FavoritoDisfrazEntity)

    @Query("DELETE FROM favorito_disfraz WHERE disfrazId = :disfrazId")
    suspend fun eliminar(disfrazId: String)

    /** Se limpia al cerrar sesión: los favoritos son de la cuenta que se va. */
    @Query("DELETE FROM favorito_disfraz")
    suspend fun limpiar()
}
