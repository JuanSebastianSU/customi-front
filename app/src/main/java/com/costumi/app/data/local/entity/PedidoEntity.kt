package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caché local de "Mis pedidos": el historial de compras/rentas del cliente (RF-16.6, `PLAN_ROOM_OFFLINE.md`
 * A4). **El mejor candidato para caché:** es historial, ya no cambia, y así la pantalla abre al instante.
 *
 * Un pedido trae sus **líneas** anidadas (`HistorialItem.lineas`) y el adapter las agrupa por disfraz. En vez
 * de una tabla 1-N (pedido + líneas) con borrado en cascada, se guarda el `HistorialItem` completo como JSON
 * por fila: es data inmutable de solo mostrar, el adapter necesita el item entero, y evita el riesgo de las
 * relaciones (mismo criterio que [DeudaEntity]). Clave [operacionId] y [orden] para conservar el orden del
 * servidor. Se limpia al cerrar sesión (N1): es el historial personal de la cuenta que se cierra.
 */
@Entity(tableName = "pedido")
data class PedidoEntity(
    @PrimaryKey val operacionId: String,
    val orden: Int,
    val json: String,
)
