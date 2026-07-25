package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CambiarContextoRequest
import com.costumi.apiclient.models.MembresiaEstadoResponse
import com.costumi.apiclient.models.MembresiaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.TokenResponse

interface MembresiaControllerApi {
    /**
     * POST api/v1/auth/contexto
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param cambiarContextoRequest 
     * @return [TokenResponse]
     */
    @POST("api/v1/auth/contexto")
    suspend fun cambiarContexto(@Body cambiarContextoRequest: CambiarContextoRequest): Response<TokenResponse>

    /**
     * POST api/v1/auth/me/desvincularme
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [MembresiaEstadoResponse]
     */
    @POST("api/v1/auth/me/desvincularme")
    suspend fun desvincularme(): Response<MembresiaEstadoResponse>

    /**
     * GET api/v1/auth/me/membresias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<MembresiaResponse>]
     */
    @GET("api/v1/auth/me/membresias")
    suspend fun mias1(): Response<kotlin.collections.List<MembresiaResponse>>

}
