package com.costumi.app.ui.cliente.pedidos

import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.HistorialItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Lógica de cómo se lee un pedido (etiqueta/tono/bucket/pago/filtro). Es la clasificación que decide qué
 * pastilla y qué filtro le toca a cada operación, así que un cambio silencioso aquí desordena "Mis pedidos".
 */
class EstadoDePedidoTest {

    private fun item(
        estado: String? = null,
        estadoPago: String? = null,
        saldo: String? = null,
    ) = HistorialItem(
        estado = estado,
        estadoPago = estadoPago,
        saldoPendiente = saldo?.let { BigDecimal(it) },
    )

    @Test
    fun etiqueta_traduce_los_estados_conocidos() {
        assertEquals("Por retirar", EstadoDePedido.etiqueta("RESERVADA"))
        assertEquals("Activo", EstadoDePedido.etiqueta("ACTIVA"))
        assertEquals("Confirmado", EstadoDePedido.etiqueta("CONFIRMADA"))
        assertEquals("Devuelto", EstadoDePedido.etiqueta("DEVUELTA"))
        assertEquals("Devuelto en parte", EstadoDePedido.etiqueta("PARCIALMENTE_DEVUELTA"))
        assertEquals("Cerrado", EstadoDePedido.etiqueta("CERRADA"))
        assertEquals("Cancelado", EstadoDePedido.etiqueta("CANCELADA"))
        assertEquals("Reembolsado", EstadoDePedido.etiqueta("REEMBOLSADA"))
    }

    @Test
    fun etiqueta_es_case_insensitive_y_capitaliza_lo_desconocido() {
        assertEquals("Activo", EstadoDePedido.etiqueta("activa"))
        assertEquals("Otracosa", EstadoDePedido.etiqueta("OTRACOSA"))
        assertEquals("-", EstadoDePedido.etiqueta(null))
    }

    @Test
    fun tono_por_estado() {
        assertEquals(Tono.INFO, EstadoDePedido.tono("RESERVADA"))
        assertEquals(Tono.EXITO, EstadoDePedido.tono("ACTIVA"))
        assertEquals(Tono.EXITO, EstadoDePedido.tono("CONFIRMADA"))
        assertEquals(Tono.ALERTA, EstadoDePedido.tono("PARCIALMENTE_DEVUELTA"))
        assertEquals(Tono.ERROR, EstadoDePedido.tono("CANCELADA"))
        assertEquals(Tono.NEUTRO, EstadoDePedido.tono("CERRADA"))
        assertEquals(Tono.NEUTRO, EstadoDePedido.tono(null))
    }

    @Test
    fun bucket_agrupa_para_los_filtros() {
        assertEquals(EstadoDePedido.Filtro.POR_RETIRAR, EstadoDePedido.bucket("RESERVADA"))
        assertEquals(EstadoDePedido.Filtro.ACTIVOS, EstadoDePedido.bucket("ACTIVA"))
        assertEquals(EstadoDePedido.Filtro.ACTIVOS, EstadoDePedido.bucket("CONFIRMADA"))
        assertEquals(EstadoDePedido.Filtro.CERRADOS, EstadoDePedido.bucket("DEVUELTA"))
        assertEquals(EstadoDePedido.Filtro.CERRADOS, EstadoDePedido.bucket("CANCELADA"))
    }

    @Test
    fun tieneSaldo_solo_cuando_es_positivo() {
        assertTrue(EstadoDePedido.tieneSaldo(item(saldo = "10.00")))
        assertFalse(EstadoDePedido.tieneSaldo(item(saldo = "0")))
        assertFalse(EstadoDePedido.tieneSaldo(item(saldo = null)))
    }

    @Test
    fun pagoEtiqueta_falta_pagado_o_nada() {
        assertEquals("Pagado", EstadoDePedido.pagoEtiqueta(item(estadoPago = "PAGADO")))
        assertNull(EstadoDePedido.pagoEtiqueta(item(estadoPago = "PENDIENTE")))
        // Con saldo positivo, gana "Falta ..." aunque el estadoPago diga otra cosa.
        val conSaldo = EstadoDePedido.pagoEtiqueta(item(estadoPago = "PAGADO", saldo = "5.00"))
        assertTrue(conSaldo != null && conSaldo.startsWith("Falta"))
    }

    @Test
    fun aplicar_todos_no_filtra() {
        val lista = listOf(item(estado = "ACTIVA"), item(estado = "CANCELADA"))
        assertEquals(2, EstadoDePedido.aplicar(lista, EstadoDePedido.Filtro.TODOS).size)
    }

    @Test
    fun aplicar_por_pagar_deja_solo_los_que_tienen_saldo() {
        val lista = listOf(
            item(estado = "ACTIVA", saldo = "10.00"),
            item(estado = "CONFIRMADA", saldo = "0"),
            item(estado = "CERRADA", saldo = null),
        )
        val soloConSaldo = EstadoDePedido.aplicar(lista, EstadoDePedido.Filtro.POR_PAGAR)
        assertEquals(1, soloConSaldo.size)
        assertEquals("ACTIVA", soloConSaldo.first().estado)
    }

    @Test
    fun aplicar_activos_usa_el_bucket_del_estado() {
        val lista = listOf(
            item(estado = "ACTIVA"),
            item(estado = "CONFIRMADA"),
            item(estado = "RESERVADA"),
            item(estado = "CERRADA"),
        )
        val activos = EstadoDePedido.aplicar(lista, EstadoDePedido.Filtro.ACTIVOS)
        assertEquals(2, activos.size) // ACTIVA + CONFIRMADA
    }
}
