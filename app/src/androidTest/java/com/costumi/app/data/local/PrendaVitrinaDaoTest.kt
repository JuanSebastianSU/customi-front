package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.PrendaVitrinaDao
import com.costumi.app.data.local.entity.PrendaVitrinaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests del DAO del catálogo con una base **en memoria** (§7 de `PLAN_ROOM_OFFLINE.md`). Verifica lo que
 * puede romperse en la caché por-tienda: guardar/leer filtrado por empresa, que `reemplazarDeEmpresa` de una
 * tienda **no borre el caché de otra** (el error documentado en §9.1), y que `limpiar` vacíe todo (logout, N1).
 */
@RunWith(AndroidJUnit4::class)
class PrendaVitrinaDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var dao: PrendaVitrinaDao

    private fun prenda(id: String, empresaId: String, nombre: String) = PrendaVitrinaEntity(
        id = id, empresaId = empresaId, nombre = nombre, tipoArticulo = "PRENDA",
        precioRenta = "10.50", precioVenta = null, categoria = null, fotoUrl = null, etiquetasJson = null,
    )

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.prendaVitrinaDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun guardar_y_observar_devuelve_solo_las_de_esa_empresa() = runTest {
        dao.guardar(listOf(prenda("1", "A", "Capa"), prenda("2", "A", "Mascara"), prenda("3", "B", "Botas")))
        val deA = dao.observarDeEmpresa("A").first()
        assertEquals(2, deA.size)
        assertTrue(deA.all { it.empresaId == "A" })
    }

    @Test
    fun reemplazar_de_una_empresa_no_borra_el_catalogo_de_otra() = runTest {
        dao.guardar(listOf(prenda("1", "A", "Capa"), prenda("2", "B", "Botas")))
        // Se refresca solo la tienda A: la B debe quedar intacta.
        dao.reemplazarDeEmpresa("A", listOf(prenda("3", "A", "Sombrero")))

        val deA = dao.observarDeEmpresa("A").first()
        val deB = dao.observarDeEmpresa("B").first()
        assertEquals(1, deA.size)
        assertEquals("3", deA.first().id)
        assertEquals(1, deB.size)
        assertEquals("Botas", deB.first().nombre)
    }

    @Test
    fun limpiar_vacia_toda_la_tabla() = runTest {
        dao.guardar(listOf(prenda("1", "A", "Capa"), prenda("2", "B", "Botas")))
        dao.limpiar()
        assertTrue(dao.observarDeEmpresa("A").first().isEmpty())
        assertTrue(dao.observarDeEmpresa("B").first().isEmpty())
    }
}
