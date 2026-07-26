package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.DeudaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeudaDao {

    /** La UI observa este Flow; se conserva el orden del servidor con [DeudaEntity.orden]. */
    @Query("SELECT * FROM deuda ORDER BY orden")
    fun observarTodas(): Flow<List<DeudaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(deudas: List<DeudaEntity>)

    @Query("DELETE FROM deuda")
    suspend fun limpiar()

    /** Reemplaza toda la caché de deudas del cliente en una transacción (es la lista completa del usuario). */
    @Transaction
    suspend fun reemplazar(deudas: List<DeudaEntity>) {
        limpiar()
        guardar(deudas)
    }
}
