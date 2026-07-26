package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.DeudaDao
import com.costumi.app.data.local.entity.DeudaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests del DAO de deudas con base **en memoria** (§7 de `PLAN_ROOM_OFFLINE.md`, A5). Verifica lo que puede
 * romperse: que se lea **en el orden del servidor** (`orden`), que `reemplazar` deje solo lo nuevo sin
 * duplicar, y que `limpiar` vacíe (logout, N1).
 */
@RunWith(AndroidJUnit4::class)
class DeudaDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var dao: DeudaDao

    private fun deuda(rentaId: String, orden: Int) =
        DeudaEntity(rentaId = rentaId, orden = orden, json = """{"rentaId":"$rentaId"}""")

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.deudaDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun observar_devuelve_en_el_orden_guardado() = runTest {
        // Se insertan desordenadas; deben salir por `orden`.
        dao.guardar(listOf(deuda("c", 2), deuda("a", 0), deuda("b", 1)))
        val leidas = dao.observarTodas().first()
        assertEquals(listOf("a", "b", "c"), leidas.map { it.rentaId })
    }

    @Test
    fun reemplazar_deja_solo_lo_nuevo_sin_duplicar() = runTest {
        dao.guardar(listOf(deuda("a", 0), deuda("b", 1)))
        dao.reemplazar(listOf(deuda("c", 0)))
        val leidas = dao.observarTodas().first()
        assertEquals(1, leidas.size)
        assertEquals("c", leidas.first().rentaId)
    }

    @Test
    fun limpiar_vacia_la_tabla() = runTest {
        dao.guardar(listOf(deuda("a", 0)))
        dao.limpiar()
        assertTrue(dao.observarTodas().first().isEmpty())
    }
}
