package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.DecisionReembolsoRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaSolicitudDeReembolsoResponse
import com.costumi.apiclient.models.SolicitarReembolsoDeClienteRequest
import com.costumi.apiclient.models.SolicitarReembolsoRequest
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse

interface ReembolsoControllerApi {
    /**
     * POST api/v1/reembolsos/{id}/aprobar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param decisionReembolsoRequest 
     * @return [SolicitudDeReembolsoResponse]
     */
    @POST("api/v1/reembolsos/{id}/aprobar")
    suspend fun aprobar(@Path("id") id: java.util.UUID, @Body decisionReembolsoRequest: DecisionReembolsoRequest): Response<SolicitudDeReembolsoResponse>

    /**
     * GET api/v1/reembolsos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @param filtro  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaSolicitudDeReembolsoResponse]
     */
    @GET("api/v1/reembolsos")
    suspend fun listar3(@Query("buscar") buscar: kotlin.String? = null, @Query("filtro") filtro: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaSolicitudDeReembolsoResponse>

    /**
     * POST api/v1/reembolsos/{id}/rechazar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param decisionReembolsoRequest 
     * @return [SolicitudDeReembolsoResponse]
     */
    @POST("api/v1/reembolsos/{id}/rechazar")
    suspend fun rechazar(@Path("id") id: java.util.UUID, @Body decisionReembolsoRequest: DecisionReembolsoRequest): Response<SolicitudDeReembolsoResponse>

    /**
     * POST api/v1/reembolsos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param solicitarReembolsoRequest 
     * @return [SolicitudDeReembolsoResponse]
     */
    @POST("api/v1/reembolsos")
    suspend fun solicitar(@Body solicitarReembolsoRequest: SolicitarReembolsoRequest): Response<SolicitudDeReembolsoResponse>

    /**
     * POST api/v1/reembolsos/cliente
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param solicitarReembolsoDeClienteRequest 
     * @return [SolicitudDeReembolsoResponse]
     */
    @POST("api/v1/reembolsos/cliente")
    suspend fun solicitarComoCliente(@Body solicitarReembolsoDeClienteRequest: SolicitarReembolsoDeClienteRequest): Response<SolicitudDeReembolsoResponse>

}
