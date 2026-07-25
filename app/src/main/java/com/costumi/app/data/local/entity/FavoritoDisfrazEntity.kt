package com.costumi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un disfraz guardado como favorito por el cliente. Guarda un **snapshot** (nombre, foto, precios) para
 * que "Mis guardados" se pinte sin red, y los ids para volver a abrir el disfraz.
 *
 * Hoy la persistencia es **local** (este dispositivo). Sincronizarlos entre dispositivos con la cuenta
 * necesita backend (ver «Favoritos del cliente» en el lote de PROGRESS.md).
 */
@Entity(tableName = "favorito_disfraz")
data class FavoritoDisfrazEntity(
    @PrimaryKey val disfrazId: String,
    val empresaId: String,
    val nombre: String,
    val fotoUrl: String?,
    val precioRenta: String?,
    val precioVenta: String?,
    /** Momento en que se guardó (para ordenar: lo más reciente primero). */
    val guardadoEn: Long,
)
