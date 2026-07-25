package com.costumi.app.ui.gestion.rentas

import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.diasEnPalabras
import com.costumi.apiclient.models.RentaResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Acciones del ciclo de una renta (además del contrato). */
enum class AccionRenta { ENTREGAR, DEVOLVER, DEVOLVER_DETALLE, COBRAR, CERRAR, CANCELAR, EXTENDER, CONTRATO }

/**
 * Lectura operativa de una renta: en qué punto del ciclo está, qué urge y qué se puede hacer AHORA.
 *
 * Antes cada fila ofrecía las 8 acciones siempre (incluida "Entregar" sobre una renta ya cerrada) y el
 * empleado tenía que deducir la fecha límite leyendo dos fechas ISO. Aquí se decide una sola vez: una
 * acción principal por estado, el resto en el menú, y los días en palabras.
 */
object CicloDeRenta {

    private val FINALES = setOf("CERRADA", "CANCELADA")

    private fun estado(r: RentaResponse) = r.estado?.uppercase().orEmpty()

    /** Sigue afuera (ACTIVA) y la fecha de devolución ya pasó. */
    fun estaVencida(r: RentaResponse, hoy: LocalDate = LocalDate.now()): Boolean {
        val devolucion = r.fechaDevolucion ?: return false
        return estado(r) == "ACTIVA" && devolucion.isBefore(hoy)
    }

    /** Nombre del estado tal como lo dice la tienda. */
    fun etiqueta(r: RentaResponse): String = when (estado(r)) {
        "RESERVADA" -> "Por entregar"
        "ACTIVA" -> if (estaVencida(r)) "Vencida" else "Activa"
        "DEVUELTA" -> "Por cerrar"
        "CERRADA" -> "Cerrada"
        "CANCELADA" -> "Cancelada"
        else -> r.estado.orEmpty()
    }

    fun tono(r: RentaResponse, hoy: LocalDate = LocalDate.now()): Tono = when (estado(r)) {
        "RESERVADA" -> Tono.INFO
        "ACTIVA" -> when {
            estaVencida(r, hoy) -> Tono.ERROR
            diasPara(r.fechaDevolucion, hoy) <= 1 -> Tono.ALERTA
            else -> Tono.EXITO
        }
        "DEVUELTA" -> Tono.ALERTA
        else -> Tono.NEUTRO
    }

    /**
     * La frase que dice qué pasa con el tiempo: "Retiro mañana", "Vence en 3 dias",
     * "Vencida hace 2 dias". Null en los estados finales, donde el tiempo ya no importa.
     */
    fun urgencia(r: RentaResponse, hoy: LocalDate = LocalDate.now()): String? = when (estado(r)) {
        "RESERVADA" -> r.fechaRetiro?.let { "Retiro ${diasEnPalabras(ChronoUnit.DAYS.between(hoy, it))}" }
        "ACTIVA" -> r.fechaDevolucion?.let {
            val dias = ChronoUnit.DAYS.between(hoy, it)
            if (dias < 0) "Vencida ${diasEnPalabras(dias)}" else "Vence ${diasEnPalabras(dias)}"
        }
        "DEVUELTA" -> "Devuelta · falta cerrarla"
        else -> null
    }

    /** La acción que toca hacer ahora, o null si la renta ya terminó su ciclo. */
    fun accionPrincipal(r: RentaResponse): Pair<AccionRenta, String>? = when (estado(r)) {
        "RESERVADA" -> AccionRenta.ENTREGAR to "Entregar"
        "ACTIVA" -> AccionRenta.DEVOLVER_DETALLE to "Registrar devolucion"
        "DEVUELTA" -> AccionRenta.CERRAR to "Cerrar renta"
        else -> null
    }

    /** El resto de acciones válidas en este estado (menú), sin las que no aplican. */
    fun accionesSecundarias(r: RentaResponse): List<Pair<AccionRenta, String>> = when (estado(r)) {
        "RESERVADA" -> listOf(
            AccionRenta.COBRAR to "Cobrar / pagos",
            AccionRenta.EXTENDER to "Cambiar fechas",
            AccionRenta.CANCELAR to "Cancelar renta",
            AccionRenta.CONTRATO to "Ver contrato",
        )
        "ACTIVA" -> listOf(
            AccionRenta.DEVOLVER to "Devolver sin revisar",
            AccionRenta.COBRAR to "Cobrar / pagos",
            AccionRenta.EXTENDER to "Extender la fecha",
            AccionRenta.CONTRATO to "Ver contrato",
        )
        "DEVUELTA" -> listOf(
            AccionRenta.COBRAR to "Cobrar / pagos",
            AccionRenta.CONTRATO to "Ver contrato",
        )
        else -> listOf(AccionRenta.CONTRATO to "Ver contrato")
    }

    fun esFinal(r: RentaResponse) = estado(r) in FINALES

    private fun diasPara(fecha: LocalDate?, hoy: LocalDate) =
        fecha?.let { ChronoUnit.DAYS.between(hoy, it) } ?: Long.MAX_VALUE
}
