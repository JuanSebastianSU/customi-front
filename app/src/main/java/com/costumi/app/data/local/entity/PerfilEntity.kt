package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de la propia cuenta del usuario (RF-14, `PLAN_ROOM_OFFLINE.md` A6). Evita el parpadeo al abrir
 * Perfil: la pantalla se pinta con lo guardado y refresca por detrás.
 *
 * Una sola fila (`id = 0`), serializada a JSON como [MiEmpresaEntity]. **Nada sensible** (N2): el perfil no
 * trae contraseña ni tokens; solo datos que la pantalla muestra (nombre, teléfono, email, foto). Se limpia al
 * cerrar sesión (norma N1).
 */
@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
)
