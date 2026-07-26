package com.costumi.app.di

import android.content.Context
import androidx.room.Room
import com.costumi.app.data.local.CostumiDatabase
import com.costumi.app.data.local.dao.DeudaDao
import com.costumi.app.data.local.dao.DisfrazVitrinaDao
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.dao.FavoritoDao
import com.costumi.app.data.local.dao.MiEmpresaDao
import com.costumi.app.data.local.dao.PedidoDao
import com.costumi.app.data.local.dao.PerfilDao
import com.costumi.app.data.local.dao.PrendaVitrinaDao
import com.costumi.app.data.local.dao.SucursalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee la base de datos local (caché) y sus DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CostumiDatabase =
        Room.databaseBuilder(context, CostumiDatabase::class.java, CostumiDatabase.NOMBRE)
            // En desarrollo aceptamos recrear la BD si cambia el esquema; antes de release se
            // definiran migraciones reales.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideEmpresaDao(db: CostumiDatabase): EmpresaDao = db.empresaDao()

    @Provides
    fun provideFavoritoDao(db: CostumiDatabase): FavoritoDao = db.favoritoDao()

    @Provides
    fun provideSucursalDao(db: CostumiDatabase): SucursalDao = db.sucursalDao()

    @Provides
    fun providePrendaVitrinaDao(db: CostumiDatabase): PrendaVitrinaDao = db.prendaVitrinaDao()

    @Provides
    fun provideDisfrazVitrinaDao(db: CostumiDatabase): DisfrazVitrinaDao = db.disfrazVitrinaDao()

    @Provides
    fun provideMiEmpresaDao(db: CostumiDatabase): MiEmpresaDao = db.miEmpresaDao()

    @Provides
    fun providePerfilDao(db: CostumiDatabase): PerfilDao = db.perfilDao()

    @Provides
    fun provideDeudaDao(db: CostumiDatabase): DeudaDao = db.deudaDao()

    @Provides
    fun providePedidoDao(db: CostumiDatabase): PedidoDao = db.pedidoDao()
}
