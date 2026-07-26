package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.DisfrazVitrinaDao
import com.costumi.app.data.local.entity.DisfrazVitrinaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests del DAO de disfraces de vitrina con base **en memoria** (§7 de `PLAN_ROOM_OFFLINE.md`, A2). Verifica
 * lo que puede romperse en la caché por-tienda: leer filtrado por empresa, que `reemplazarDeEmpresa` de una
 * tienda **no borre el caché de otra** (§9.1), y que `limpiar` vacíe todo (logout, N1).
 */
@RunWith(AndroidJUnit4::class)
class DisfrazVitrinaDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var dao: DisfrazVitrinaDao

    private fun disfraz(id: String, empresaId: String, nombre: String) = DisfrazVitrinaEntity(
        id = id, empresaId = empresaId, nombre = nombre, categoria = null, tipo = "AMBOS",
        precioRentaGeneral = "20.00", precioRentaSugerido = null, precioVentaGeneral = null,
        precioVentaSugerido = null, fotoUrl = null, piezas = 3,
    )

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.disfrazVitrinaDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun guardar_y_observar_devuelve_solo_los_de_esa_empresa() = runTest {
        dao.guardar(listOf(disfraz("1", "A", "Pirata"), disfraz("2", "A", "Bruja"), disfraz("3", "B", "Robot")))
        val deA = dao.observarDeEmpresa("A").first()
        assertEquals(2, deA.size)
        assertTrue(deA.all { it.empresaId == "A" })
    }

    @Test
    fun reemplazar_de_una_empresa_no_borra_los_de_otra() = runTest {
        dao.guardar(listOf(disfraz("1", "A", "Pirata"), disfraz("2", "B", "Robot")))
        dao.reemplazarDeEmpresa("A", listOf(disfraz("3", "A", "Vampiro")))

        val deA = dao.observarDeEmpresa("A").first()
        val deB = dao.observarDeEmpresa("B").first()
        assertEquals(1, deA.size)
        assertEquals("3", deA.first().id)
        assertEquals(1, deB.size)
        assertEquals("Robot", deB.first().nombre)
    }

    @Test
    fun limpiar_vacia_toda_la_tabla() = runTest {
        dao.guardar(listOf(disfraz("1", "A", "Pirata"), disfraz("2", "B", "Robot")))
        dao.limpiar()
        assertTrue(dao.observarDeEmpresa("A").first().isEmpty())
        assertTrue(dao.observarDeEmpresa("B").first().isEmpty())
    }
}
