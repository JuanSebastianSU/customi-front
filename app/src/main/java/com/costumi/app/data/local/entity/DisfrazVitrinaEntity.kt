package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché local de los disfraces en vitrina de una tienda (RF-18, `PLAN_ROOM_OFFLINE.md` A2). Gemelo de
 * [PrendaVitrinaEntity]: la UI observa Room y el repo trae de la red y guarda aquí.
 *
 * Solo se guarda lo que la vitrina PINTA (nombre, foto, precios que fijó el dueño, tipo y cuántas piezas),
 * nunca lo que decide (disponibilidad/stock, N3). **Los slots no se cachean** (§A2 del plan): son estructura
 * anidada que solo se ve al abrir el detalle, y ese detalle se vuelve a pedir a la red; aquí basta con
 * [piezas] (el conteo) para el "N piezas" de la tarjeta. Índice por [empresaId] y reemplazo por-empresa.
 */
@Entity(tableName = "disfraz_vitrina", indices = [Index("empresaId")])
data class DisfrazVitrinaEntity(
    @PrimaryKey val id: String,
    val empresaId: String,
    val nombre: String?,
    val categoria: String?,
    /** "RENTA" / "VENTA" / "AMBOS" (valor del enum `DisfrazResponse.Tipo`); null si no vino. */
    val tipo: String?,
    val precioRentaGeneral: String?,
    val precioRentaSugerido: String?,
    val precioVentaGeneral: String?,
    val precioVentaSugerido: String?,
    val fotoUrl: String?,
    /** Cantidad de piezas (= `slots.size`); solo el conteo, no el contenido de los slots. */
    val piezas: Int?,
)
