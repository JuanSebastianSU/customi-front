package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarEmpresaRequest

interface EmpresaControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/{id}/aprobar")
    suspend fun aprobar1(@Path("id") id: java.util.UUID): Response<EmpresaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<EmpresaResumenResponse>]
     */
    @GET("api/v1/empresas")
    suspend fun listar8(): Response<kotlin.collections.List<EmpresaResumenResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [EmpresaResponse]
     */
    @GET("api/v1/empresas/mia")
    suspend fun mia(): Response<EmpresaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<EmpresaPendienteResponse>]
     */
    @GET("api/v1/empresas/pendientes")
    suspend fun pendientes(): Response<kotlin.collections.List<EmpresaPendienteResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/{id}/reactivar")
    suspend fun reactivar(@Path("id") id: java.util.UUID): Response<EmpresaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/{id}/rechazar")
    suspend fun rechazar1(@Path("id") id: java.util.UUID): Response<EmpresaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registrarEmpresaRequest 
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas")
    suspend fun registrar2(@Body registrarEmpresaRequest: RegistrarEmpresaRequest): Response<EmpresaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/{id}/suspender")
    suspend fun suspender(@Path("id") id: java.util.UUID): Response<EmpresaResponse>

}
