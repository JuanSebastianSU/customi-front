package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AjusteDeStockRequest
import com.costumi.apiclient.models.CrearGrupoDeStockRequest
import com.costumi.apiclient.models.EntradaDeStockRequest
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.MoverUnidadesRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.TransferirStockRequest

interface GrupoDeStockControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param grupoId 
     * @param ajusteDeStockRequest 
     * @return [GrupoDeStockResponse]
     */
    @POST("api/v1/grupos-stock/{grupoId}/ajuste")
    suspend fun ajustar(@Path("grupoId") grupoId: java.util.UUID, @Body ajusteDeStockRequest: AjusteDeStockRequest): Response<GrupoDeStockResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param prendaId 
     * @param crearGrupoDeStockRequest 
     * @return [GrupoDeStockResponse]
     */
    @POST("api/v1/prendas/{prendaId}/grupos-stock")
    suspend fun crear3(@Path("prendaId") prendaId: java.util.UUID, @Body crearGrupoDeStockRequest: CrearGrupoDeStockRequest): Response<GrupoDeStockResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param grupoId 
     * @return [Unit]
     */
    @DELETE("api/v1/grupos-stock/{grupoId}")
    suspend fun eliminar(@Path("grupoId") grupoId: java.util.UUID): Response<Unit>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param prendaId 
     * @return [kotlin.collections.List<GrupoDeStockResponse>]
     */
    @GET("api/v1/prendas/{prendaId}/grupos-stock")
    suspend fun listar5(@Path("prendaId") prendaId: java.util.UUID): Response<kotlin.collections.List<GrupoDeStockResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param grupoId 
     * @param moverUnidadesRequest 
     * @return [GrupoDeStockResponse]
     */
    @POST("api/v1/grupos-stock/{grupoId}/mover")
    suspend fun mover(@Path("grupoId") grupoId: java.util.UUID, @Body moverUnidadesRequest: MoverUnidadesRequest): Response<GrupoDeStockResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param grupoId 
     * @param entradaDeStockRequest 
     * @return [GrupoDeStockResponse]
     */
    @POST("api/v1/grupos-stock/{grupoId}/entrada")
    suspend fun reabastecer(@Path("grupoId") grupoId: java.util.UUID, @Body entradaDeStockRequest: EntradaDeStockRequest): Response<GrupoDeStockResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param umbral  (optional, default to 1)
     * @return [kotlin.collections.List<GrupoDeStockResponse>]
     */
    @GET("api/v1/grupos-stock/stock-bajo")
    suspend fun stockBajo(@Query("umbral") umbral: kotlin.Int? = 1): Response<kotlin.collections.List<GrupoDeStockResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param grupoId 
     * @param transferirStockRequest 
     * @return [GrupoDeStockResponse]
     */
    @POST("api/v1/grupos-stock/{grupoId}/transferir")
    suspend fun transferir(@Path("grupoId") grupoId: java.util.UUID, @Body transferirStockRequest: TransferirStockRequest): Response<GrupoDeStockResponse>

}
