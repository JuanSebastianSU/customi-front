package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AgregarValorRequest
import com.costumi.apiclient.models.CrearTipoEtiquetaRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RenombrarRequest
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaResponse

interface TipoEtiquetaControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @return [TipoEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta/{tipoId}/activar")
    suspend fun activarTipo(@Path("tipoId") tipoId: java.util.UUID): Response<TipoEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param valorId 
     * @return [ValorEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta/{tipoId}/valores/{valorId}/activar")
    suspend fun activarValor(@Path("tipoId") tipoId: java.util.UUID, @Path("valorId") valorId: java.util.UUID): Response<ValorEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param agregarValorRequest 
     * @return [ValorEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta/{tipoId}/valores")
    suspend fun agregarValor(@Path("tipoId") tipoId: java.util.UUID, @Body agregarValorRequest: AgregarValorRequest): Response<ValorEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @return [TipoEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta/{tipoId}/archivar")
    suspend fun archivarTipo(@Path("tipoId") tipoId: java.util.UUID): Response<TipoEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param valorId 
     * @return [ValorEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta/{tipoId}/valores/{valorId}/archivar")
    suspend fun archivarValor(@Path("tipoId") tipoId: java.util.UUID, @Path("valorId") valorId: java.util.UUID): Response<ValorEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearTipoEtiquetaRequest 
     * @return [TipoEtiquetaResponse]
     */
    @POST("api/v1/tipos-etiqueta")
    suspend fun crear(@Body crearTipoEtiquetaRequest: CrearTipoEtiquetaRequest): Response<TipoEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<TipoEtiquetaResponse>]
     */
    @GET("api/v1/tipos-etiqueta")
    suspend fun listar1(): Response<kotlin.collections.List<TipoEtiquetaResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @return [kotlin.collections.List<ValorEtiquetaResponse>]
     */
    @GET("api/v1/tipos-etiqueta/{tipoId}/valores")
    suspend fun listarValores(@Path("tipoId") tipoId: java.util.UUID): Response<kotlin.collections.List<ValorEtiquetaResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param renombrarRequest 
     * @return [TipoEtiquetaResponse]
     */
    @PATCH("api/v1/tipos-etiqueta/{tipoId}")
    suspend fun renombrar(@Path("tipoId") tipoId: java.util.UUID, @Body renombrarRequest: RenombrarRequest): Response<TipoEtiquetaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param valorId 
     * @param renombrarRequest 
     * @return [ValorEtiquetaResponse]
     */
    @PATCH("api/v1/tipos-etiqueta/{tipoId}/valores/{valorId}")
    suspend fun renombrarValor(@Path("tipoId") tipoId: java.util.UUID, @Path("valorId") valorId: java.util.UUID, @Body renombrarRequest: RenombrarRequest): Response<ValorEtiquetaResponse>

}
