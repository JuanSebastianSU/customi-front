package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CambiarListaNegraRequest
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.CrearClienteRequest
import com.costumi.apiclient.models.DeviceTokenRequest
import com.costumi.apiclient.models.EditarClienteRequest
import com.costumi.apiclient.models.EstadoDeCuentaResponse
import com.costumi.apiclient.models.FavoritoResponse
import com.costumi.apiclient.models.GuardarFavoritoRequest
import com.costumi.apiclient.models.HistorialItem
import com.costumi.apiclient.models.MiDeudaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaClienteResponse
import com.costumi.apiclient.models.RespuestaPaginadaHistorialItem

interface ClienteControllerApi {
    /**
     * POST api/v1/clientes/{id}/activar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [ClienteResponse]
     */
    @POST("api/v1/clientes/{id}/activar")
    suspend fun activar5(@Path("id") id: java.util.UUID): Response<ClienteResponse>

    /**
     * POST api/v1/clientes/{id}/archivar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [ClienteResponse]
     */
    @POST("api/v1/clientes/{id}/archivar")
    suspend fun archivar4(@Path("id") id: java.util.UUID): Response<ClienteResponse>

    /**
     * POST api/v1/clientes/{id}/lista-negra
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param cambiarListaNegraRequest 
     * @return [ClienteResponse]
     */
    @POST("api/v1/clientes/{id}/lista-negra")
    suspend fun cambiarListaNegra(@Path("id") id: java.util.UUID, @Body cambiarListaNegraRequest: CambiarListaNegraRequest): Response<ClienteResponse>

    /**
     * POST api/v1/clientes
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param crearClienteRequest 
     * @return [ClienteResponse]
     */
    @POST("api/v1/clientes")
    suspend fun crear6(@Body crearClienteRequest: CrearClienteRequest): Response<ClienteResponse>

    /**
     * PUT api/v1/clientes/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param editarClienteRequest 
     * @return [ClienteResponse]
     */
    @PUT("api/v1/clientes/{id}")
    suspend fun editar2(@Path("id") id: java.util.UUID, @Body editarClienteRequest: EditarClienteRequest): Response<ClienteResponse>

    /**
     * GET api/v1/clientes/{id}/estado-cuenta
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [EstadoDeCuentaResponse]
     */
    @GET("api/v1/clientes/{id}/estado-cuenta")
    suspend fun estadoCuenta(@Path("id") id: java.util.UUID): Response<EstadoDeCuentaResponse>

    /**
     * POST api/v1/clientes/me/favoritos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param guardarFavoritoRequest 
     * @return [FavoritoResponse]
     */
    @POST("api/v1/clientes/me/favoritos")
    suspend fun guardarFavorito(@Body guardarFavoritoRequest: GuardarFavoritoRequest): Response<FavoritoResponse>

    /**
     * GET api/v1/clientes/{id}/historial
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [kotlin.collections.List<HistorialItem>]
     */
    @GET("api/v1/clientes/{id}/historial")
    suspend fun historial(@Path("id") id: java.util.UUID): Response<kotlin.collections.List<HistorialItem>>

    /**
     * GET api/v1/clientes
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @param conPendientes  (optional, default to false)
     * @param filtro  (optional)
     * @param incluirArchivados  (optional, default to false)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaClienteResponse]
     */
    @GET("api/v1/clientes")
    suspend fun listar14(@Query("buscar") buscar: kotlin.String? = null, @Query("conPendientes") conPendientes: kotlin.Boolean? = false, @Query("filtro") filtro: kotlin.String? = null, @Query("incluirArchivados") incluirArchivados: kotlin.Boolean? = false, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaClienteResponse>

    /**
     * GET api/v1/clientes/me/historial
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param filtro  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaHistorialItem]
     */
    @GET("api/v1/clientes/me/historial")
    suspend fun miHistorial(@Query("filtro") filtro: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaHistorialItem>

    /**
     * GET api/v1/clientes/me/operaciones/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @return [HistorialItem]
     */
    @GET("api/v1/clientes/me/operaciones/{id}")
    suspend fun miOperacion(@Path("id") id: java.util.UUID): Response<HistorialItem>

    /**
     * GET api/v1/clientes/me/deudas
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<MiDeudaResponse>]
     */
    @GET("api/v1/clientes/me/deudas")
    suspend fun misDeudas(): Response<kotlin.collections.List<MiDeudaResponse>>

    /**
     * GET api/v1/clientes/me/favoritos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<FavoritoResponse>]
     */
    @GET("api/v1/clientes/me/favoritos")
    suspend fun misFavoritos(): Response<kotlin.collections.List<FavoritoResponse>>

    /**
     * DELETE api/v1/clientes/me/favoritos/{disfrazId}
     * 
     * 
     * Responses:
     *  - 204: No Content
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param disfrazId 
     * @return [Unit]
     */
    @DELETE("api/v1/clientes/me/favoritos/{disfrazId}")
    suspend fun quitarFavorito(@Path("disfrazId") disfrazId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/clientes/{id}/device-token
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param id 
     * @param deviceTokenRequest 
     * @return [ClienteResponse]
     */
    @PUT("api/v1/clientes/{id}/device-token")
    suspend fun registrarDeviceToken(@Path("id") id: java.util.UUID, @Body deviceTokenRequest: DeviceTokenRequest): Response<ClienteResponse>

    /**
     * PUT api/v1/clientes/me/device-token
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param deviceTokenRequest 
     * @return [Unit]
     */
    @PUT("api/v1/clientes/me/device-token")
    suspend fun registrarMiDeviceToken(@Body deviceTokenRequest: DeviceTokenRequest): Response<Unit>

}
