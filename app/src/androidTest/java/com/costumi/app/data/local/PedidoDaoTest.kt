package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.PedidoDao
import com.costumi.app.data.local.entity.PedidoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests del DAO del historial de pedidos con base **en memoria** (§7 de `PLAN_ROOM_OFFLINE.md`, A4): que se
 * lea **en el orden del servidor** (`orden`), que `reemplazar` deje solo lo nuevo y que `limpiar` vacíe (N1).
 */
@RunWith(AndroidJUnit4::class)
class PedidoDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var dao: PedidoDao

    private fun pedido(operacionId: String, orden: Int) =
        PedidoEntity(operacionId = operacionId, orden = orden, json = """{"operacionId":"$operacionId"}""")

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pedidoDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun observar_devuelve_en_el_orden_guardado() = runTest {
        dao.guardar(listOf(pedido("c", 2), pedido("a", 0), pedido("b", 1)))
        val leidos = dao.observarTodos().first()
        assertEquals(listOf("a", "b", "c"), leidos.map { it.operacionId })
    }

    @Test
    fun reemplazar_deja_solo_lo_nuevo_sin_duplicar() = runTest {
        dao.guardar(listOf(pedido("a", 0), pedido("b", 1)))
        dao.reemplazar(listOf(pedido("c", 0)))
        val leidos = dao.observarTodos().first()
        assertEquals(1, leidos.size)
        assertEquals("c", leidos.first().operacionId)
    }

    @Test
    fun limpiar_vacia_la_tabla() = runTest {
        dao.guardar(listOf(pedido("a", 0)))
        dao.limpiar()
        assertTrue(dao.observarTodos().first().isEmpty())
    }
}
