package com.costumi.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.dao.PrendaVitrinaDao
import com.costumi.app.data.local.dao.SucursalDao
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.app.data.local.entity.PrendaVitrinaEntity
import com.costumi.app.data.local.entity.SucursalEntity

/**
 * Base de datos local (caché offline, RF-16.6/17.5). Se irán sumando entidades/DAOs por feature.
 * En desarrollo se permite migración destructiva; antes de release se definirán migraciones reales.
 */
@Database(
    entities = [
        EmpresaEntity::class,
        FavoritoDisfrazEntity::class,
        SucursalEntity::class,
        PrendaVitrinaEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class CostumiDatabase : RoomDatabase() {
    abstract fun empresaDao(): EmpresaDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun sucursalDao(): SucursalDao
    abstract fun prendaVitrinaDao(): PrendaVitrinaDao

    companion object {
        const val NOMBRE = "costumi.db"
    }
}
