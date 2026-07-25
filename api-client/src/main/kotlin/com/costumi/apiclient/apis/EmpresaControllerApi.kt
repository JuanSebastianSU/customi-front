package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.EditarMiTiendaRequest
import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import com.costumi.apiclient.models.FijarHorarioRequest
import com.costumi.apiclient.models.HorarioResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarEmpresaRequest
import com.costumi.apiclient.models.SubirFotoRequest

interface EmpresaControllerApi {
    /**
     * POST api/v1/empresas/{id}/aprobar
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
     * PATCH api/v1/empresas/mia
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param editarMiTiendaRequest 
     * @return [EmpresaResponse]
     */
    @PATCH("api/v1/empresas/mia")
    suspend fun editarMia(@Body editarMiTiendaRequest: EditarMiTiendaRequest): Response<EmpresaResponse>

    /**
     * PUT api/v1/empresas/mia/horario
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param fijarHorarioRequest 
     * @return [kotlin.collections.List<HorarioResponse>]
     */
    @PUT("api/v1/empresas/mia/horario")
    suspend fun fijarHorarioMio(@Body fijarHorarioRequest: FijarHorarioRequest): Response<kotlin.collections.List<HorarioResponse>>

    /**
     * GET api/v1/empresas/mia/horario
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<HorarioResponse>]
     */
    @GET("api/v1/empresas/mia/horario")
    suspend fun horarioMio(): Response<kotlin.collections.List<HorarioResponse>>

    /**
     * GET api/v1/empresas
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
     * GET api/v1/empresas/mia
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
     * GET api/v1/empresas/pendientes
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
     * POST api/v1/empresas/{id}/reactivar
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
     * POST api/v1/empresas/{id}/rechazar
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
     * POST api/v1/empresas
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
     * POST api/v1/empresas/mia/logo
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param subirFotoRequest  (optional)
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/mia/logo")
    suspend fun subirLogo(@Body subirFotoRequest: SubirFotoRequest? = null): Response<EmpresaResponse>

    /**
     * POST api/v1/empresas/mia/portada
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param subirFotoRequest  (optional)
     * @return [EmpresaResponse]
     */
    @POST("api/v1/empresas/mia/portada")
    suspend fun subirPortada(@Body subirFotoRequest: SubirFotoRequest? = null): Response<EmpresaResponse>

    /**
     * POST api/v1/empresas/{id}/suspender
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
