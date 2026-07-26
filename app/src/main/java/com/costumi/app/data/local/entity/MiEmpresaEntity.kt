package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de la propia tienda del usuario (RF-15.1, `PLAN_ROOM_OFFLINE.md` A6). Reemplaza la caché en
 * memoria que tenía `MiEmpresaRepository`: una sola fuente de verdad, y ahora sobrevive a que el proceso
 * muera (el nombre encabeza Gestión y aparece al instante al reabrir).
 *
 * Es de **una sola fila** (`id = 0`): es la tienda del usuario actual, no una lista. El form de identidad
 * necesita el `EmpresaResponse` completo, así que se guarda serializado a JSON en una sola columna (no hay
 * coste de migración por-campo, y las migraciones son destructivas por ser caché, N7). Se limpia al cerrar
 * sesión (norma N1): si no, el próximo dueño en el mismo teléfono vería la tienda anterior.
 */
@Entity(tableName = "mi_empresa")
data class MiEmpresaEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
)
