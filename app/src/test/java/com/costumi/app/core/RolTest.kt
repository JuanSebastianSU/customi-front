package com.costumi.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parseo del rol que viene del token y su mapeo a modo de app. Decide a qué shell entra el usuario
 * (cliente/gestión/superadmin), así que un rol mal leído mandaría a alguien a la pantalla equivocada.
 */
class RolTest {

    @Test
    fun desde_reconoce_los_roles_exactos() {
        assertEquals(Rol.CLIENTE, Rol.desde("CLIENTE"))
        assertEquals(Rol.DUENO, Rol.desde("DUENO"))
        assertEquals(Rol.SUPERADMIN, Rol.desde("SUPERADMIN"))
    }

    @Test
    fun desde_es_case_insensitive_y_recorta_espacios() {
        assertEquals(Rol.ENCARGADO, Rol.desde("encargado"))
        assertEquals(Rol.MOSTRADOR, Rol.desde("  Mostrador  "))
    }

    @Test
    fun desde_devuelve_null_para_desconocido_o_null() {
        assertNull(Rol.desde("INEXISTENTE"))
        assertNull(Rol.desde(null))
        assertNull(Rol.desde(""))
    }

    @Test
    fun el_modo_agrupa_los_roles_de_gestion() {
        assertEquals(ModoApp.CLIENTE, Rol.CLIENTE.modo)
        assertEquals(ModoApp.SUPERADMIN, Rol.SUPERADMIN.modo)
        // Dueño y todos los empleados comparten el modo Gestión.
        listOf(Rol.DUENO, Rol.ENCARGADO, Rol.MOSTRADOR, Rol.BODEGA, Rol.ATENCION).forEach {
            assertEquals("$it deberia ser GESTION", ModoApp.GESTION, it.modo)
        }
    }
}
