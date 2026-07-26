package com.costumi.app.data.repo

import com.costumi.apiclient.models.EmpresaVitrinaResponse
import com.costumi.apiclient.models.FavoritoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Mapeadores DTO(red) → entidad(Room) de la caché. Reglas que importan: se descarta lo que no trae id (sin
 * clave no se puede cachear) y los campos en blanco caen a un valor por defecto legible, no a "" ni null suelto.
 */
class MapeadoresTest {

    @Test
    fun empresa_sin_id_se_descarta() {
        assertNull(EmpresaVitrinaResponse(id = null, nombre = "X").aEntity())
    }

    @Test
    fun empresa_mapea_campos_y_recorta_blancos() {
        val id = UUID.randomUUID()
        val e = EmpresaVitrinaResponse(
            id = id, nombre = "Fiesta", ciudad = "  ", logoUrl = "http://x/l.png", portadaUrl = "",
        ).aEntity()!!
        assertEquals(id.toString(), e.id)
        assertEquals("Fiesta", e.nombre)
        assertNull(e.ciudad)      // "  " en blanco -> null
        assertEquals("http://x/l.png", e.logoUrl)
        assertNull(e.portadaUrl)  // "" -> null
    }

    @Test
    fun empresa_sin_nombre_usa_default_legible() {
        val e = EmpresaVitrinaResponse(id = UUID.randomUUID(), nombre = " ").aEntity()!!
        assertEquals("Tienda", e.nombre)
    }

    @Test
    fun favorito_sin_alguno_de_los_ids_se_descarta() {
        assertNull(FavoritoResponse(disfrazId = null, empresaId = UUID.randomUUID()).aEntity())
        assertNull(FavoritoResponse(disfrazId = UUID.randomUUID(), empresaId = null).aEntity())
    }

    @Test
    fun favorito_mapea_ids_precios_y_default_de_nombre() {
        val did = UUID.randomUUID()
        val eid = UUID.randomUUID()
        val f = FavoritoResponse(
            disfrazId = did, empresaId = eid, nombre = "  ",
            precioRenta = BigDecimal("12.50"), precioVenta = null,
            guardadoEn = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        ).aEntity()!!
        assertEquals(did.toString(), f.disfrazId)
        assertEquals(eid.toString(), f.empresaId)
        assertEquals("Disfraz", f.nombre)          // blanco -> default
        assertEquals("12.50", f.precioRenta)        // BigDecimal como texto plano
        assertNull(f.precioVenta)
        assertEquals(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli(), f.guardadoEn)
    }
}
