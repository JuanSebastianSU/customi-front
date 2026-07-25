package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.DevolverVentaRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarVentaRequest
import com.costumi.apiclient.models.RespuestaPaginadaVentaResponse
import com.costumi.apiclient.models.VentaResponse

interface VentaControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param devolverVentaRequest  (optional)
     * @return [VentaResponse]
     */
    @POST("api/v1/ventas/{id}/devolver")
    suspend fun devolver(@Path("id") id: java.util.UUID, @Body devolverVentaRequest: DevolverVentaRequest? = null): Response<VentaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaVentaResponse]
     */
    @GET("api/v1/ventas")
    suspend fun listar(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaVentaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [VentaResponse]
     */
    @GET("api/v1/ventas/{id}")
    suspend fun porId(@Path("id") id: java.util.UUID): Response<VentaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registrarVentaRequest 
     * @return [VentaResponse]
     */
    @POST("api/v1/ventas")
    suspend fun registrar(@Body registrarVentaRequest: RegistrarVentaRequest): Response<VentaResponse>

}
