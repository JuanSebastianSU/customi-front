package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.DisfrazVitrinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisfrazVitrinaDao {

    /** La UI observa este Flow (por tienda): cambios en la tabla re-emiten sin volver a pedir a la red. */
    @Query("SELECT * FROM disfraz_vitrina WHERE empresaId = :empresaId ORDER BY nombre")
    fun observarDeEmpresa(empresaId: String): Flow<List<DisfrazVitrinaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(disfraces: List<DisfrazVitrinaEntity>)

    /** Borra solo los disfraces de una tienda (para reemplazarlos sin tocar el de las demás). */
    @Query("DELETE FROM disfraz_vitrina WHERE empresaId = :empresaId")
    suspend fun limpiarDeEmpresa(empresaId: String)

    /** Vacía toda la tabla (se usa al cerrar sesión, norma N1). */
    @Query("DELETE FROM disfraz_vitrina")
    suspend fun limpiar()

    /** Reemplaza los disfraces de UNA tienda en una transacción (no borra el caché de las demás, §9.1). */
    @Transaction
    suspend fun reemplazarDeEmpresa(empresaId: String, disfraces: List<DisfrazVitrinaEntity>) {
        limpiarDeEmpresa(empresaId)
        guardar(disfraces)
    }
}
