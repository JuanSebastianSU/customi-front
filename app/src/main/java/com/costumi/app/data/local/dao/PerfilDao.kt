package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.costumi.app.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {

    /** La UI observa esto: al refrescar y guardar, la pantalla se actualiza sola (cache-first). */
    @Query("SELECT * FROM perfil WHERE id = 0")
    fun observar(): Flow<PerfilEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fila: PerfilEntity)

    @Query("DELETE FROM perfil")
    suspend fun limpiar()
}
