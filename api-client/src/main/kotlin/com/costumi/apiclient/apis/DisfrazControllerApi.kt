package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ConteoPorPrendaResponse
import com.costumi.apiclient.models.CrearDisfrazRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.DisponibilidadResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RentarDisfrazRequest
import com.costumi.apiclient.models.RentarDisfrazResponse
import com.costumi.apiclient.models.RentarVariosDisfracesRequest
import com.costumi.apiclient.models.RespuestaPaginadaDisfrazResponse
import com.costumi.apiclient.models.SubirFotoRequest
import com.costumi.apiclient.models.VenderDisfrazRequest
import com.costumi.apiclient.models.VenderDisfrazResponse
import com.costumi.apiclient.models.VenderVariosDisfracesRequest

interface DisfrazControllerApi {
    /**
     * POST api/v1/disfraces/{disfrazId}/activar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @return [DisfrazResponse]
     */
    @POST("api/v1/disfraces/{disfrazId}/activar")
    suspend fun activar3(@Path("disfrazId") disfrazId: java.util.UUID): Response<DisfrazResponse>

    /**
     * POST api/v1/disfraces/{disfrazId}/archivar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @return [DisfrazResponse]
     */
    @POST("api/v1/disfraces/{disfrazId}/archivar")
    suspend fun archivar2(@Path("disfrazId") disfrazId: java.util.UUID): Response<DisfrazResponse>

    /**
     * GET api/v1/disfraces/conteo-por-prenda/{prendaId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param prendaId 
     * @return [ConteoPorPrendaResponse]
     */
    @GET("api/v1/disfraces/conteo-por-prenda/{prendaId}")
    suspend fun conteoPorPrenda(@Path("prendaId") prendaId: java.util.UUID): Response<ConteoPorPrendaResponse>

    /**
     * POST api/v1/disfraces
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearDisfrazRequest 
     * @return [DisfrazResponse]
     */
    @POST("api/v1/disfraces")
    suspend fun crear4(@Body crearDisfrazRequest: CrearDisfrazRequest): Response<DisfrazResponse>

    /**
     * GET api/v1/disfraces/{disfrazId}/disponibilidad
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @param empresaId  (optional)
     * @return [DisponibilidadResponse]
     */
    @GET("api/v1/disfraces/{disfrazId}/disponibilidad")
    suspend fun disponibilidad(@Path("disfrazId") disfrazId: java.util.UUID, @Query("empresaId") empresaId: java.util.UUID? = null): Response<DisponibilidadResponse>

    /**
     * PUT api/v1/disfraces/{disfrazId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @param crearDisfrazRequest 
     * @return [DisfrazResponse]
     */
    @PUT("api/v1/disfraces/{disfrazId}")
    suspend fun editar1(@Path("disfrazId") disfrazId: java.util.UUID, @Body crearDisfrazRequest: CrearDisfrazRequest): Response<DisfrazResponse>

    /**
     * GET api/v1/disfraces
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param categoriaId  (optional)
     * @param buscar  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaDisfrazResponse]
     */
    @GET("api/v1/disfraces")
    suspend fun listar11(@Query("categoriaId") categoriaId: java.util.UUID? = null, @Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaDisfrazResponse>

    /**
     * POST api/v1/disfraces/{disfrazId}/rentar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @param rentarDisfrazRequest 
     * @return [RentarDisfrazResponse]
     */
    @POST("api/v1/disfraces/{disfrazId}/rentar")
    suspend fun rentar(@Path("disfrazId") disfrazId: java.util.UUID, @Body rentarDisfrazRequest: RentarDisfrazRequest): Response<RentarDisfrazResponse>

    /**
     * POST api/v1/disfraces/rentar-varios
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param rentarVariosDisfracesRequest 
     * @return [RentarDisfrazResponse]
     */
    @POST("api/v1/disfraces/rentar-varios")
    suspend fun rentarVarios(@Body rentarVariosDisfracesRequest: RentarVariosDisfracesRequest): Response<RentarDisfrazResponse>

    /**
     * POST api/v1/disfraces/{disfrazId}/foto
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @param subirFotoRequest  (optional)
     * @return [DisfrazResponse]
     */
    @POST("api/v1/disfraces/{disfrazId}/foto")
    suspend fun subirFoto3(@Path("disfrazId") disfrazId: java.util.UUID, @Body subirFotoRequest: SubirFotoRequest? = null): Response<DisfrazResponse>

    /**
     * POST api/v1/disfraces/{disfrazId}/vender
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @param venderDisfrazRequest 
     * @return [VenderDisfrazResponse]
     */
    @POST("api/v1/disfraces/{disfrazId}/vender")
    suspend fun vender(@Path("disfrazId") disfrazId: java.util.UUID, @Body venderDisfrazRequest: VenderDisfrazRequest): Response<VenderDisfrazResponse>

    /**
     * POST api/v1/disfraces/vender-varios
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param venderVariosDisfracesRequest 
     * @return [VenderDisfrazResponse]
     */
    @POST("api/v1/disfraces/vender-varios")
    suspend fun venderVarios(@Body venderVariosDisfracesRequest: VenderVariosDisfracesRequest): Response<VenderDisfrazResponse>

}
