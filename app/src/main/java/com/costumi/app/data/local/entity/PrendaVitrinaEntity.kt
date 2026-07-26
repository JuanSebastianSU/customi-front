package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché local del catálogo (prendas en vitrina) de una tienda (RF-18, `PLAN_ROOM_OFFLINE.md` A1). La UI
 * observa Room como fuente de verdad: el repo trae de la red, guarda aquí, y la pantalla se pinta al instante.
 *
 * Solo se guardan campos que DESCRIBEN (nombre, foto, precios de catálogo, etiquetas), nunca lo que decide
 * (stock/disponibilidad, norma N3). Se indexa por [empresaId] porque el catálogo SIEMPRE se consulta por
 * tienda y el refresco reemplaza solo las prendas de esa tienda (no borra el caché de otras).
 *
 * Sin TypeConverters (misma filosofía que [SucursalEntity]): el UUID va como texto, los precios `BigDecimal`
 * como texto plano, y las etiquetas como JSON (se (de)serializan en el mapeador del repositorio). Se limpia
 * al cerrar sesión (norma N1).
 */
@Entity(tableName = "prenda_vitrina", indices = [Index("empresaId")])
data class PrendaVitrinaEntity(
    @PrimaryKey val id: String,
    val empresaId: String,
    val nombre: String?,
    val tipoArticulo: String?,
    val precioRenta: String?,
    val precioVenta: String?,
    val categoria: String?,
    val fotoUrl: String?,
    /** JSON de `List<EtiquetaVitrinaDto>` (solo tipo+valor); null si la prenda no trae etiquetas. */
    val etiquetasJson: String?,
)
