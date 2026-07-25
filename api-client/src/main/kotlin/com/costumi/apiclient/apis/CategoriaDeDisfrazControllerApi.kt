package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CategoriaDeDisfrazRequest
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.costumi.apiclient.models.ProblemDetail

interface CategoriaDeDisfrazControllerApi {
    /**
     * POST api/v1/disfraces/categorias/{id}/activar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [CategoriaDeDisfrazResponse]
     */
    @POST("api/v1/disfraces/categorias/{id}/activar")
    suspend fun activar4(@Path("id") id: java.util.UUID): Response<CategoriaDeDisfrazResponse>

    /**
     * POST api/v1/disfraces/categorias/{id}/archivar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [CategoriaDeDisfrazResponse]
     */
    @POST("api/v1/disfraces/categorias/{id}/archivar")
    suspend fun archivar3(@Path("id") id: java.util.UUID): Response<CategoriaDeDisfrazResponse>

    /**
     * POST api/v1/disfraces/categorias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param categoriaDeDisfrazRequest 
     * @return [CategoriaDeDisfrazResponse]
     */
    @POST("api/v1/disfraces/categorias")
    suspend fun crear5(@Body categoriaDeDisfrazRequest: CategoriaDeDisfrazRequest): Response<CategoriaDeDisfrazResponse>

    /**
     * GET api/v1/disfraces/categorias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<CategoriaDeDisfrazResponse>]
     */
    @GET("api/v1/disfraces/categorias")
    suspend fun listar12(): Response<kotlin.collections.List<CategoriaDeDisfrazResponse>>

    /**
     * PATCH api/v1/disfraces/categorias/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param categoriaDeDisfrazRequest 
     * @return [CategoriaDeDisfrazResponse]
     */
    @PATCH("api/v1/disfraces/categorias/{id}")
    suspend fun renombrar1(@Path("id") id: java.util.UUID, @Body categoriaDeDisfrazRequest: CategoriaDeDisfrazRequest): Response<CategoriaDeDisfrazResponse>

}
