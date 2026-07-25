package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.EnviarNotificacionRequest
import com.costumi.apiclient.models.EstadoDeCanales
import com.costumi.apiclient.models.NotificacionResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RecordatorioResponse
import com.costumi.apiclient.models.RespuestaPaginadaNotificacionResponse
import com.costumi.apiclient.models.ResultadoDePrueba

interface NotificacionControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [RecordatorioResponse]
     */
    @POST("api/v1/notificaciones/avisar-stock-bajo")
    suspend fun avisarStockBajo(): Response<RecordatorioResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param enviarNotificacionRequest 
     * @return [NotificacionResponse]
     */
    @POST("api/v1/notificaciones")
    suspend fun enviar(@Body enviarNotificacionRequest: EnviarNotificacionRequest): Response<NotificacionResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [EstadoDeCanales]
     */
    @GET("api/v1/notificaciones/estado-canales")
    suspend fun estadoDeCanales(): Response<EstadoDeCanales>

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
     * @return [RespuestaPaginadaNotificacionResponse]
     */
    @GET("api/v1/notificaciones")
    suspend fun listar7(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaNotificacionResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param clienteId 
     * @return [ResultadoDePrueba]
     */
    @POST("api/v1/notificaciones/probar-push/{clienteId}")
    suspend fun probarPush(@Path("clienteId") clienteId: java.util.UUID): Response<ResultadoDePrueba>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [RecordatorioResponse]
     */
    @POST("api/v1/notificaciones/recordar-proximas")
    suspend fun recordarProximas(): Response<RecordatorioResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [RecordatorioResponse]
     */
    @POST("api/v1/notificaciones/recordar-vencidas")
    suspend fun recordarVencidas(): Response<RecordatorioResponse>

}
