package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ConfiguracionRequest
import com.costumi.apiclient.models.ConfiguracionResponse
import com.costumi.apiclient.models.ProblemDetail

interface ConfiguracionControllerApi {
    /**
     * PUT api/v1/configuracion
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param configuracionRequest 
     * @return [ConfiguracionResponse]
     */
    @PUT("api/v1/configuracion")
    suspend fun actualizar2(@Body configuracionRequest: ConfiguracionRequest): Response<ConfiguracionResponse>

    /**
     * GET api/v1/configuracion/export
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [ConfiguracionResponse]
     */
    @GET("api/v1/configuracion/export")
    suspend fun exportar(): Response<ConfiguracionResponse>

    /**
     * POST api/v1/configuracion/import
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param configuracionRequest 
     * @return [ConfiguracionResponse]
     */
    @POST("api/v1/configuracion/import")
    suspend fun importar(@Body configuracionRequest: ConfiguracionRequest): Response<ConfiguracionResponse>

    /**
     * GET api/v1/configuracion
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [ConfiguracionResponse]
     */
    @GET("api/v1/configuracion")
    suspend fun obtener(): Response<ConfiguracionResponse>

}
