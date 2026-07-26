package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de una empresa del marketplace (RF-14.2). La UI observa Room como fuente de verdad:
 * el repositorio trae de la red, guarda aquí, y la pantalla se actualiza sola. Los campos se
 * ampliarán al cablear el repositorio del marketplace (Fase 3).
 */
@Entity(tableName = "empresa")
data class EmpresaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val ciudad: String?,
    val logoUrl: String?,
    val portadaUrl: String?,
)
