package com.costumi.app.ui.cliente.pedidos

import com.costumi.apiclient.models.LineaDeHistorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Agrupado de líneas de un pedido en artículos (`LineaPedidoAdapter.agrupar`). Es la regla que hace que las
 * piezas de un mismo disfraz se vean como **un** artículo ("Traje Pirata · 3 piezas") y no como piezas
 * sueltas. Se agrupa por `disfrazGrupo` (no por `disfrazId`) porque el mismo disfraz puede ir dos veces.
 */
class LineaPedidoAdapterTest {

    private val g1: UUID = UUID.randomUUID()
    private val g2: UUID = UUID.randomUUID()

    private fun piezaDisfraz(grupo: UUID, nombre: String = "Pirata", foto: String? = null, cantidad: Int? = 1) =
        LineaDeHistorial(
            disfrazGrupo = grupo, disfrazNombre = nombre, disfrazCantidad = cantidad,
            fotoUrl = foto, nombre = "pieza", cantidad = 1,
        )

    private fun prendaSuelta(nombre: String, cantidad: Int = 1) =
        LineaDeHistorial(prendaId = UUID.randomUUID(), nombre = nombre, cantidad = cantidad)

    @Test
    fun prendas_sueltas_pasan_tal_cual() {
        val articulos = LineaPedidoAdapter.agrupar(listOf(prendaSuelta("Capa", 2)))
        assertEquals(1, articulos.size)
        assertEquals("Capa", articulos.first().nombre)
        assertEquals(2, articulos.first().cantidad)
        assertEquals(null, articulos.first().detalle)
    }

    @Test
    fun piezas_del_mismo_disfraz_se_colapsan_en_un_articulo() {
        val lineas = listOf(
            piezaDisfraz(g1, "Traje Pirata", cantidad = 1),
            piezaDisfraz(g1, "Traje Pirata"),
            piezaDisfraz(g1, "Traje Pirata"),
        )
        val articulos = LineaPedidoAdapter.agrupar(lineas)
        assertEquals(1, articulos.size)
        assertEquals("Traje Pirata", articulos.first().nombre)
        assertEquals("3 piezas", articulos.first().detalle)
    }

    @Test
    fun una_sola_pieza_dice_1_pieza() {
        val articulos = LineaPedidoAdapter.agrupar(listOf(piezaDisfraz(g1)))
        assertEquals("1 pieza", articulos.first().detalle)
    }

    @Test
    fun el_mismo_disfraz_en_dos_grupos_son_dos_articulos() {
        val lineas = listOf(
            piezaDisfraz(g1, "Pirata"),
            piezaDisfraz(g2, "Pirata"),
        )
        val articulos = LineaPedidoAdapter.agrupar(lineas)
        assertEquals(2, articulos.size)
    }

    @Test
    fun la_foto_del_disfraz_es_la_primera_pieza_con_foto() {
        val lineas = listOf(
            piezaDisfraz(g1, foto = null),
            piezaDisfraz(g1, foto = "http://x/foto.png"),
        )
        val articulo = LineaPedidoAdapter.agrupar(lineas).first()
        assertEquals("http://x/foto.png", articulo.fotoUrl)
    }

    @Test
    fun mezcla_disfraz_y_prendas_devuelve_ambos() {
        val lineas = listOf(
            piezaDisfraz(g1, "Pirata"),
            prendaSuelta("Sombrero"),
        )
        val articulos = LineaPedidoAdapter.agrupar(lineas)
        assertEquals(2, articulos.size)
        assertTrue(articulos.any { it.nombre == "Pirata" })
        assertTrue(articulos.any { it.nombre == "Sombrero" })
    }
}
