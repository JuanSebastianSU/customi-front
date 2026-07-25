package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CrearCategoriaRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RenombrarRequest

interface CategoriaControllerApi {
    /**
     * POST api/v1/categorias/{id}/activar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [CategoriaResponse]
     */
    @POST("api/v1/categorias/{id}/activar")
    suspend fun activar6(@Path("id") id: java.util.UUID): Response<CategoriaResponse>

    /**
     * POST api/v1/categorias/{id}/archivar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [CategoriaResponse]
     */
    @POST("api/v1/categorias/{id}/archivar")
    suspend fun archivar5(@Path("id") id: java.util.UUID): Response<CategoriaResponse>

    /**
     * POST api/v1/categorias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearCategoriaRequest 
     * @return [CategoriaResponse]
     */
    @POST("api/v1/categorias")
    suspend fun crear7(@Body crearCategoriaRequest: CrearCategoriaRequest): Response<CategoriaResponse>

    /**
     * GET api/v1/categorias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<CategoriaResponse>]
     */
    @GET("api/v1/categorias")
    suspend fun listar15(): Response<kotlin.collections.List<CategoriaResponse>>

    /**
     * PATCH api/v1/categorias/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param renombrarRequest 
     * @return [CategoriaResponse]
     */
    @PATCH("api/v1/categorias/{id}")
    suspend fun renombrar2(@Path("id") id: java.util.UUID, @Body renombrarRequest: RenombrarRequest): Response<CategoriaResponse>

}
