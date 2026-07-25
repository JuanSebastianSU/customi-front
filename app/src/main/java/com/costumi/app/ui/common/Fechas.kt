package com.costumi.app.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formato corto de fecha en español (12 ago). Único en la app: las listas no deben escribirlo cada una. */
val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es"))

fun LocalDate.comoDiaMes(): String = format(FORMATO_DIA_MES)

/** Periodo legible: "12 ago → 15 ago · 3 dias". Devuelve null si falta alguna fecha. */
fun periodoLegible(desde: LocalDate?, hasta: LocalDate?): String? {
    if (desde == null || hasta == null) return null
    val dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta).toInt().coerceAtLeast(0)
    val plazo = if (dias == 1) "1 dia" else "$dias dias"
    return "${desde.comoDiaMes()} → ${hasta.comoDiaMes()}  ·  $plazo"
}

/** Fecha y hora relativa a hoy: "hoy 14:30", "ayer 09:12", "12 jul 14:30". Para trails/historial. */
fun java.time.OffsetDateTime.comoFechaHora(): String {
    val enZona = atZoneSameInstant(java.time.ZoneId.systemDefault())
    val dia = enZona.toLocalDate()
    val hora = enZona.format(DateTimeFormatter.ofPattern("HH:mm"))
    val hoy = LocalDate.now()
    return when (dia) {
        hoy -> "hoy $hora"
        hoy.minusDays(1) -> "ayer $hora"
        else -> "${dia.comoDiaMes()} $hora"
    }
}

/** El selector de fechas de Material trabaja en milisegundos UTC; estas dos hacen la conversion. */
fun aLocalDate(millis: Long?): LocalDate? =
    millis?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() }

fun LocalDate.enMillisUtc(): Long = atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

/**
 * Cuenta de dias en palabras respecto a hoy: "hoy", "mañana", "en 3 dias", "hace 2 dias".
 * [dias] es la diferencia con hoy (positivo = futuro).
 */
fun diasEnPalabras(dias: Long): String = when {
    dias == 0L -> "hoy"
    dias == 1L -> "mañana"
    dias == -1L -> "ayer"
    dias > 1 -> "en $dias dias"
    else -> "hace ${-dias} dias"
}
