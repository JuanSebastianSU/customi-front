package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CrearRentaRequest
import com.costumi.apiclient.models.ExtenderRentaRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RentaResponse
import com.costumi.apiclient.models.RespuestaPaginadaRentaResponse
import com.costumi.apiclient.models.ResumenDeRentasResponse

interface RentaControllerApi {
    /**
     * POST api/v1/rentas/{id}/cancelar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: java.util.UUID): Response<RentaResponse>

    /**
     * POST api/v1/rentas/{id}/cerrar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas/{id}/cerrar")
    suspend fun cerrar(@Path("id") id: java.util.UUID): Response<RentaResponse>

    /**
     * GET api/v1/rentas/{id}/contrato.pdf
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [kotlin.ByteArray]
     */
    @GET("api/v1/rentas/{id}/contrato.pdf")
    suspend fun contrato(@Path("id") id: java.util.UUID): Response<kotlin.ByteArray>

    /**
     * POST api/v1/rentas
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearRentaRequest 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas")
    suspend fun crear1(@Body crearRentaRequest: CrearRentaRequest): Response<RentaResponse>

    /**
     * POST api/v1/rentas/{id}/devolver
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas/{id}/devolver")
    suspend fun devolver1(@Path("id") id: java.util.UUID): Response<RentaResponse>

    /**
     * POST api/v1/rentas/{id}/entregar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas/{id}/entregar")
    suspend fun entregar(@Path("id") id: java.util.UUID): Response<RentaResponse>

    /**
     * POST api/v1/rentas/{id}/extender
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param extenderRentaRequest 
     * @return [RentaResponse]
     */
    @POST("api/v1/rentas/{id}/extender")
    suspend fun extender(@Path("id") id: java.util.UUID, @Body extenderRentaRequest: ExtenderRentaRequest): Response<RentaResponse>

    /**
     * GET api/v1/rentas
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param clienteId  (optional)
     * @param buscar  (optional)
     * @param filtro  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaRentaResponse]
     */
    @GET("api/v1/rentas")
    suspend fun listar2(@Query("clienteId") clienteId: java.util.UUID? = null, @Query("buscar") buscar: kotlin.String? = null, @Query("filtro") filtro: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaRentaResponse>

    /**
     * GET api/v1/rentas/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [RentaResponse]
     */
    @GET("api/v1/rentas/{id}")
    suspend fun porId1(@Path("id") id: java.util.UUID): Response<RentaResponse>

    /**
     * GET api/v1/rentas/resumen
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [ResumenDeRentasResponse]
     */
    @GET("api/v1/rentas/resumen")
    suspend fun resumen(): Response<ResumenDeRentasResponse>

}
