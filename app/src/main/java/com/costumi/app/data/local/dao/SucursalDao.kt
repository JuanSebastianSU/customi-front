package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.SucursalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SucursalDao {

    /** La UI observa este Flow: cambios en la tabla re-emiten sin volver a pedir a la red. */
    @Query("SELECT * FROM sucursal ORDER BY nombre")
    fun observarTodas(): Flow<List<SucursalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(sucursales: List<SucursalEntity>)

    @Query("DELETE FROM sucursal")
    suspend fun limpiar()

    /** Reemplaza toda la caché de una vez (borra las que ya no están y guarda las nuevas), en una transacción. */
    @Transaction
    suspend fun reemplazar(sucursales: List<SucursalEntity>) {
        limpiar()
        guardar(sucursales)
    }
}
