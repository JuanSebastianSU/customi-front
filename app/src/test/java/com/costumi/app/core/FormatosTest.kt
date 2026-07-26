package com.costumi.app.core

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Formateo de montos. El formato exacto depende del locale de la JVM, así que se verifica el contrato
 * estable: null → null, y un monto no nulo produce un texto no vacío que incluye las cifras.
 */
class FormatosTest {

    @Test
    fun precio_null_es_null() {
        val nulo: BigDecimal? = null
        assertNull(nulo.comoPrecio())
    }

    @Test
    fun precio_no_nulo_incluye_las_cifras() {
        val texto = BigDecimal("45000").comoPrecio()
        assertTrue(texto != null && texto.isNotBlank())
        // El separador de miles varía por locale; basta con que aparezcan los dígitos significativos.
        assertTrue(texto!!.contains("45"))
    }

    @Test
    fun precio_cero_se_formatea() {
        val texto = BigDecimal.ZERO.comoPrecio()
        assertTrue(texto != null && texto.contains("0"))
    }
}
