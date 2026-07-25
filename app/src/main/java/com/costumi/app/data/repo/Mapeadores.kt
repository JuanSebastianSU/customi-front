package com.costumi.app.data.repo

import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.apiclient.models.EmpresaVitrinaResponse

/** Convierte la vitrina de empresa (red) a entidad de cache (Room). Descarta las que no traen id. */
fun EmpresaVitrinaResponse.aEntity(): EmpresaEntity? {
    val idTexto = id?.toString() ?: return null
    return EmpresaEntity(
        id = idTexto,
        nombre = nombre?.takeIf { it.isNotBlank() } ?: "Tienda",
        ciudad = null,
        logoUrl = null,
    )
}
