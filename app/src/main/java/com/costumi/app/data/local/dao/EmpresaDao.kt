package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {

    /** La UI observa este Flow: cambios en la tabla re-emiten sin volver a pedir a la red. */
    @Query("SELECT * FROM empresa ORDER BY nombre")
    fun observarTodas(): Flow<List<EmpresaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(empresas: List<EmpresaEntity>)

    @Query("DELETE FROM empresa")
    suspend fun limpiar()

    /** Reemplaza toda la caché de una vez (borra las que ya no estan y guarda las nuevas). */
    @Transaction
    suspend fun reemplazar(empresas: List<EmpresaEntity>) {
        limpiar()
        guardar(empresas)
    }
}
