package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.SucursalDao
import com.costumi.app.data.local.entity.SucursalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests del DAO de sucursales con una base **en memoria** (§7 de `PLAN_ROOM_OFFLINE.md`). Verifica lo que
 * puede romperse en la caché: guardar/leer, que `reemplazar` no acumule duplicados, y que `limpiar` vacíe.
 */
@RunWith(AndroidJUnit4::class)
class SucursalDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var dao: SucursalDao

    private fun sucursal(id: String, nombre: String) = SucursalEntity(
        id = id, empresaId = "e1", nombre = nombre, direccion = null, ubicacionMaps = null,
        descripcion = null, fotoUrl = null, latitud = null, longitud = null, archivada = false,
    )

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.sucursalDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun guardar_y_leer_devuelve_lo_guardado() = runTest {
        dao.guardar(listOf(sucursal("1", "Centro"), sucursal("2", "Norte")))
        val leidas = dao.observarTodas().first()
        assertEquals(2, leidas.size)
        assertEquals("Centro", leidas.first { it.id == "1" }.nombre)
    }

    @Test
    fun reemplazar_deja_solo_lo_nuevo_sin_duplicar() = runTest {
        dao.guardar(listOf(sucursal("1", "Centro"), sucursal("2", "Norte")))
        dao.reemplazar(listOf(sucursal("3", "Sur")))
        val leidas = dao.observarTodas().first()
        assertEquals(1, leidas.size)
        assertEquals("3", leidas.first().id)
    }

    @Test
    fun limpiar_vacia_la_tabla() = runTest {
        dao.guardar(listOf(sucursal("1", "Centro")))
        dao.limpiar()
        assertTrue(dao.observarTodas().first().isEmpty())
    }
}
