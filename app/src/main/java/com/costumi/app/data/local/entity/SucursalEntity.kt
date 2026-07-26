package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de una sucursal de la tienda del dueño (RF-15.4, `PLAN_ROOM_OFFLINE.md` A3). La UI observa
 * Room como fuente de verdad: el repositorio trae de la red, guarda aquí, y la pantalla se actualiza sola.
 *
 * Solo se guardan campos que DESCRIBEN (nombre, dirección, foto…), nunca datos que deciden (stock, saldos).
 * Los UUID se guardan como texto (no hacen falta TypeConverters). Se limpia al cerrar sesión (norma N1).
 */
@Entity(tableName = "sucursal")
data class SucursalEntity(
    @PrimaryKey val id: String,
    val empresaId: String?,
    val nombre: String?,
    val direccion: String?,
    val ubicacionMaps: String?,
    val descripcion: String?,
    val fotoUrl: String?,
    val latitud: Double?,
    val longitud: Double?,
    val archivada: Boolean?,
)
