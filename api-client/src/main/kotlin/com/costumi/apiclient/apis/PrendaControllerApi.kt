package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CrearPrendaRequest
import com.costumi.apiclient.models.EditarPrendaRequest
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaPrendaResponse
import com.costumi.apiclient.models.SubirFotoRequest

interface PrendaControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [PrendaResponse]
     */
    @POST("api/v1/prendas/{id}/activar")
    suspend fun activar(@Path("id") id: java.util.UUID): Response<PrendaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [PrendaResponse]
     */
    @POST("api/v1/prendas/{id}/archivar")
    suspend fun archivar(@Path("id") id: java.util.UUID): Response<PrendaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param categoriaId  (optional)
     * @param etiqueta  (optional)
     * @return [kotlin.collections.List<PrendaDeCatalogoResponse>]
     */
    @GET("api/v1/prendas/catalogo")
    suspend fun catalogo(@Query("categoriaId") categoriaId: java.util.UUID? = null, @Query("etiqueta") etiqueta: @JvmSuppressWildcards kotlin.collections.List<kotlin.String>? = null): Response<kotlin.collections.List<PrendaDeCatalogoResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearPrendaRequest 
     * @return [PrendaResponse]
     */
    @POST("api/v1/prendas")
    suspend fun crear2(@Body crearPrendaRequest: CrearPrendaRequest): Response<PrendaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param editarPrendaRequest 
     * @return [PrendaResponse]
     */
    @PUT("api/v1/prendas/{id}")
    suspend fun editar(@Path("id") id: java.util.UUID, @Body editarPrendaRequest: EditarPrendaRequest): Response<PrendaResponse>

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
     * @return [RespuestaPaginadaPrendaResponse]
     */
    @GET("api/v1/prendas")
    suspend fun listar4(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaPrendaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param subirFotoRequest  (optional)
     * @return [PrendaResponse]
     */
    @POST("api/v1/prendas/{id}/foto")
    suspend fun subirFoto(@Path("id") id: java.util.UUID, @Body subirFotoRequest: SubirFotoRequest? = null): Response<PrendaResponse>

}
