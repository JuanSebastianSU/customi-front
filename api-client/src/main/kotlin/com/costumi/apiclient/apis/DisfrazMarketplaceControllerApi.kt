package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.DisfrazDetalleResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.SlotOpcionesResponse

interface DisfrazMarketplaceControllerApi {
    /**
     * GET api/v1/marketplace/empresas/{empresaId}/disfraces/{disfrazId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param disfrazId 
     * @return [DisfrazDetalleResponse]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/disfraces/{disfrazId}")
    suspend fun detalle(@Path("empresaId") empresaId: java.util.UUID, @Path("disfrazId") disfrazId: java.util.UUID): Response<DisfrazDetalleResponse>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}/disfraces
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [kotlin.collections.List<DisfrazResponse>]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/disfraces")
    suspend fun listar18(@Path("empresaId") empresaId: java.util.UUID): Response<kotlin.collections.List<DisfrazResponse>>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}/disfraces/{disfrazId}/slots/{orden}/opciones
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param disfrazId 
     * @param orden 
     * @param valores  (optional)
     * @return [SlotOpcionesResponse]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/disfraces/{disfrazId}/slots/{orden}/opciones")
    suspend fun opcionesDeSlot(@Path("empresaId") empresaId: java.util.UUID, @Path("disfrazId") disfrazId: java.util.UUID, @Path("orden") orden: kotlin.Int, @Query("valores") valores: @JvmSuppressWildcards kotlin.collections.List<java.util.UUID>? = null): Response<SlotOpcionesResponse>

}
