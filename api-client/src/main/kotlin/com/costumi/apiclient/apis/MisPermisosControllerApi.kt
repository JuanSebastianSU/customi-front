package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CapacidadDto
import com.costumi.apiclient.models.ProblemDetail

interface MisPermisosControllerApi {
    /**
     * GET api/v1/empleados/me/permisos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<CapacidadDto>]
     */
    @GET("api/v1/empleados/me/permisos")
    suspend fun mias(): Response<kotlin.collections.List<CapacidadDto>>

}
