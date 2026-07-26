package com.costumi.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.costumi.app.data.local.dao.MiEmpresaDao
import com.costumi.app.data.local.dao.PerfilDao
import com.costumi.app.data.local.entity.MiEmpresaEntity
import com.costumi.app.data.local.entity.PerfilEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de las cachés de **una sola fila** (mi tienda y perfil, `PLAN_ROOM_OFFLINE.md` A6). Lo que puede
 * romperse: que `id = 0` REEMPLACE (no acumule filas al guardar dos veces) y que `limpiar` vacíe (logout, N1).
 */
@RunWith(AndroidJUnit4::class)
class CacheUnicaFilaDaoTest {

    private lateinit var db: CostumiDatabase
    private lateinit var miEmpresa: MiEmpresaDao
    private lateinit var perfil: PerfilDao

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CostumiDatabase::class.java,
        ).allowMainThreadQueries().build()
        miEmpresa = db.miEmpresaDao()
        perfil = db.perfilDao()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun mi_empresa_guardar_dos_veces_deja_una_sola_fila_con_lo_ultimo() = runTest {
        miEmpresa.guardar(MiEmpresaEntity(json = """{"nombre":"Vieja"}"""))
        miEmpresa.guardar(MiEmpresaEntity(json = """{"nombre":"Nueva"}"""))
        assertEquals("""{"nombre":"Nueva"}""", miEmpresa.leer()?.json)
    }

    @Test
    fun mi_empresa_limpiar_vacia() = runTest {
        miEmpresa.guardar(MiEmpresaEntity(json = """{"nombre":"X"}"""))
        miEmpresa.limpiar()
        assertNull(miEmpresa.leer())
    }

    @Test
    fun perfil_guardar_dos_veces_deja_una_sola_fila_y_limpiar_vacia() = runTest {
        perfil.guardar(PerfilEntity(json = """{"email":"a@x.com"}"""))
        perfil.guardar(PerfilEntity(json = """{"email":"b@x.com"}"""))
        assertEquals("""{"email":"b@x.com"}""", perfil.observar().first()?.json)

        perfil.limpiar()
        assertNull(perfil.observar().first())
    }
}
