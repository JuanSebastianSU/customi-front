package com.costumi.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Formateadores de fecha compartidos. El formato exacto (nombre del mes) depende del locale, así que se
 * verifica el contrato estable: pluralización de días, orden del periodo, palabras relativas y el
 * ida-y-vuelta millis↔LocalDate del selector de Material.
 */
class FechasTest {

    @Test
    fun periodo_null_si_falta_una_fecha() {
        assertNull(periodoLegible(null, LocalDate.of(2026, 1, 5)))
        assertNull(periodoLegible(LocalDate.of(2026, 1, 5), null))
    }

    @Test
    fun periodo_pluraliza_y_mantiene_el_orden() {
        val tres = periodoLegible(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 8))
        assertTrue(tres != null && tres.contains("→") && tres.contains("3 dias"))

        val uno = periodoLegible(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6))
        assertTrue(uno != null && uno.contains("1 dia") && !uno.contains("1 dias"))
    }

    @Test
    fun periodo_no_da_dias_negativos() {
        // Fechas invertidas: los días se acotan a 0, no cuenta hacia atrás.
        val invertido = periodoLegible(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 5))
        assertTrue(invertido != null && invertido.contains("0 dias"))
    }

    @Test
    fun dias_en_palabras() {
        assertEquals("hoy", diasEnPalabras(0))
        assertEquals("mañana", diasEnPalabras(1))
        assertEquals("ayer", diasEnPalabras(-1))
        assertEquals("en 3 dias", diasEnPalabras(3))
        assertEquals("hace 2 dias", diasEnPalabras(-2))
    }

    @Test
    fun millis_y_localdate_hacen_ida_y_vuelta() {
        val fecha = LocalDate.of(2026, 7, 26)
        assertEquals(fecha, aLocalDate(fecha.enMillisUtc()))
        assertNull(aLocalDate(null))
    }
}
