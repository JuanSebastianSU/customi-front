package com.costumi.app.data.repo

import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.apiclient.models.EmpresaVitrinaResponse
import com.costumi.apiclient.models.FavoritoResponse

/** Convierte la vitrina de empresa (red) a entidad de cache (Room). Descarta las que no traen id. */
fun EmpresaVitrinaResponse.aEntity(): EmpresaEntity? {
    val idTexto = id?.toString() ?: return null
    return EmpresaEntity(
        id = idTexto,
        nombre = nombre?.takeIf { it.isNotBlank() } ?: "Tienda",
        ciudad = ciudad?.takeIf { it.isNotBlank() },
        logoUrl = logoUrl?.takeIf { it.isNotBlank() },
        portadaUrl = portadaUrl?.takeIf { it.isNotBlank() },
    )
}

/** Convierte un favorito del server a la entidad de cache local (Room). Descarta los sin ids. */
fun FavoritoResponse.aEntity(): FavoritoDisfrazEntity? {
    val did = disfrazId?.toString() ?: return null
    val eid = empresaId?.toString() ?: return null
    return FavoritoDisfrazEntity(
        disfrazId = did,
        empresaId = eid,
        nombre = nombre?.takeIf { it.isNotBlank() } ?: "Disfraz",
        fotoUrl = fotoUrl,
        precioRenta = precioRenta?.toPlainString(),
        precioVenta = precioVenta?.toPlainString(),
        guardadoEn = guardadoEn?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(),
    )
}
