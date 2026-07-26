package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.costumi.app.data.local.entity.MiEmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiEmpresaDao {

    /** Observa la única fila (para quien quiera cache-first por Flow). */
    @Query("SELECT * FROM mi_empresa WHERE id = 0")
    fun observar(): Flow<MiEmpresaEntity?>

    /** Lee la única fila de una vez (para `mia()`, que es one-shot). */
    @Query("SELECT * FROM mi_empresa WHERE id = 0")
    suspend fun leer(): MiEmpresaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fila: MiEmpresaEntity)

    @Query("DELETE FROM mi_empresa")
    suspend fun limpiar()
}
