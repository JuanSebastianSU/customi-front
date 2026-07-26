package com.costumi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.costumi.app.data.local.entity.PrendaVitrinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrendaVitrinaDao {

    /** La UI observa este Flow (por tienda): cambios en la tabla re-emiten sin volver a pedir a la red. */
    @Query("SELECT * FROM prenda_vitrina WHERE empresaId = :empresaId ORDER BY nombre")
    fun observarDeEmpresa(empresaId: String): Flow<List<PrendaVitrinaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(prendas: List<PrendaVitrinaEntity>)

    /** Borra solo el catálogo de una tienda (para reemplazarlo sin tocar el de las demás). */
    @Query("DELETE FROM prenda_vitrina WHERE empresaId = :empresaId")
    suspend fun limpiarDeEmpresa(empresaId: String)

    /** Vacía toda la tabla (se usa al cerrar sesión, norma N1). */
    @Query("DELETE FROM prenda_vitrina")
    suspend fun limpiar()

    /**
     * Reemplaza el catálogo de UNA tienda en una transacción. Clave: borra solo `empresaId`, así abrir la
     * tienda B no borra el caché de la tienda A (el error documentado de "reemplazar toda la tabla", §9.1).
     */
    @Transaction
    suspend fun reemplazarDeEmpresa(empresaId: String, prendas: List<PrendaVitrinaEntity>) {
        limpiarDeEmpresa(empresaId)
        guardar(prendas)
    }
}
