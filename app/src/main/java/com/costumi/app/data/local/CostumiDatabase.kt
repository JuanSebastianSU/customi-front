package com.costumi.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity

/**
 * Base de datos local (caché offline, RF-16.6/17.5). Se irán sumando entidades/DAOs por feature.
 * En desarrollo se permite migración destructiva; antes de release se definirán migraciones reales.
 */
@Database(
    entities = [
        EmpresaEntity::class,
        FavoritoDisfrazEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class CostumiDatabase : RoomDatabase() {
    abstract fun empresaDao(): EmpresaDao
    abstract fun favoritoDao(): FavoritoDao

    companion object {
        const val NOMBRE = "costumi.db"
    }
}
