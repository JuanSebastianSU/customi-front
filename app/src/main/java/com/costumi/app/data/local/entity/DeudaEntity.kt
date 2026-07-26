package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de las multas/saldos del propio cliente (RF-7/11.5, `PLAN_ROOM_OFFLINE.md` A5). Útil para
 * consultar sin conexión qué se debe y por qué, sin arrancar en blanco.
 *
 * ⚠️ **N3/N4:** esto se muestra, **no decide**. El importe a cobrar SIEMPRE se reconfirma contra el servidor
 * antes de pagar (esta pantalla es solo informativa; el pago va por otro flujo con confirmación server-side).
 * Y cuando se muestre caché sin red hay que avisarlo (N4) — el banner llega con B3.
 *
 * El adapter usa el DTO completo (desglose de cargos), así que se guarda serializado a JSON por fila, con
 * clave [rentaId] y [orden] para conservar el orden del servidor. Se limpia al cerrar sesión (norma N1):
 * son datos personales del cliente que se cierra.
 */
@Entity(tableName = "deuda")
data class DeudaEntity(
    @PrimaryKey val rentaId: String,
    val orden: Int,
    val json: String,
)
