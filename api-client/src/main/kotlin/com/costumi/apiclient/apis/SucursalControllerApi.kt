package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.EditarSucursalRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarSucursalRequest
import com.costumi.apiclient.models.SucursalResponse

interface SucursalControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param id 
     * @return [SucursalResponse]
     */
    @POST("api/v1/empresas/{empresaId}/sucursales/{id}/activar")
    suspend fun activar1(@Path("empresaId") empresaId: java.util.UUID, @Path("id") id: java.util.UUID): Response<SucursalResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param id 
     * @return [SucursalResponse]
     */
    @POST("api/v1/empresas/{empresaId}/sucursales/{id}/archivar")
    suspend fun archivar1(@Path("empresaId") empresaId: java.util.UUID, @Path("id") id: java.util.UUID): Response<SucursalResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param id 
     * @param editarSucursalRequest 
     * @return [SucursalResponse]
     */
    @PATCH("api/v1/empresas/{empresaId}/sucursales/{id}")
    suspend fun editar3(@Path("empresaId") empresaId: java.util.UUID, @Path("id") id: java.util.UUID, @Body editarSucursalRequest: EditarSucursalRequest): Response<SucursalResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [kotlin.collections.List<SucursalResponse>]
     */
    @GET("api/v1/empresas/{empresaId}/sucursales")
    suspend fun listar9(@Path("empresaId") empresaId: java.util.UUID): Response<kotlin.collections.List<SucursalResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param registrarSucursalRequest 
     * @return [SucursalResponse]
     */
    @POST("api/v1/empresas/{empresaId}/sucursales")
    suspend fun registrar3(@Path("empresaId") empresaId: java.util.UUID, @Body registrarSucursalRequest: RegistrarSucursalRequest): Response<SucursalResponse>

}
