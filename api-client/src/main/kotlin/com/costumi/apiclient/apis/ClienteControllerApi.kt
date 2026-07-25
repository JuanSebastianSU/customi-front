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
import com.costumi.apiclient.models.HistorialItem
import com.costumi.apiclient.models.MiDeudaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaClienteResponse

interface ClienteControllerApi {
    /**
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
    suspend fun crear7(@Body crearClienteRequest: CrearClienteRequest): Response<ClienteResponse>

    /**
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
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<HistorialItem>]
     */
    @GET("api/v1/clientes/me/historial")
    suspend fun miHistorial(): Response<kotlin.collections.List<HistorialItem>>

    /**
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
