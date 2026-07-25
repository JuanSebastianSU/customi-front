package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CobroMixtoResponse
import com.costumi.apiclient.models.ComprobanteResponse
import com.costumi.apiclient.models.EstadoDeposito
import com.costumi.apiclient.models.IntentoDePagoDeClienteRequest
import com.costumi.apiclient.models.IntentoDePagoRequest
import com.costumi.apiclient.models.IntentoDePagoResponse
import com.costumi.apiclient.models.PagoResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarCobroMixtoRequest
import com.costumi.apiclient.models.RegistrarPagoRequest
import com.costumi.apiclient.models.SaldoResponse
import com.costumi.apiclient.models.WebhookPagoRequest

interface PagoControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param conceptoId 
     * @return [ComprobanteResponse]
     */
    @GET("api/v1/pagos/comprobante")
    suspend fun comprobante(@Query("conceptoId") conceptoId: java.util.UUID): Response<ComprobanteResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param conceptoId 
     * @return [kotlin.ByteArray]
     */
    @GET("api/v1/pagos/comprobante.pdf")
    suspend fun comprobantePdf(@Query("conceptoId") conceptoId: java.util.UUID): Response<kotlin.ByteArray>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param conceptoId 
     * @return [EstadoDeposito]
     */
    @GET("api/v1/pagos/deposito")
    suspend fun deposito(@Query("conceptoId") conceptoId: java.util.UUID): Response<EstadoDeposito>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param intentoDePagoRequest 
     * @return [IntentoDePagoResponse]
     */
    @POST("api/v1/pagos/intento")
    suspend fun intento(@Body intentoDePagoRequest: IntentoDePagoRequest): Response<IntentoDePagoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param intentoDePagoDeClienteRequest 
     * @return [IntentoDePagoResponse]
     */
    @POST("api/v1/pagos/intento/cliente")
    suspend fun intentoDeCliente(@Body intentoDePagoDeClienteRequest: IntentoDePagoDeClienteRequest): Response<IntentoDePagoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param conceptoId 
     * @return [kotlin.collections.List<PagoResponse>]
     */
    @GET("api/v1/pagos")
    suspend fun listar6(@Query("conceptoId") conceptoId: java.util.UUID): Response<kotlin.collections.List<PagoResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registrarPagoRequest 
     * @return [PagoResponse]
     */
    @POST("api/v1/pagos")
    suspend fun registrar1(@Body registrarPagoRequest: RegistrarPagoRequest): Response<PagoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registrarCobroMixtoRequest 
     * @return [CobroMixtoResponse]
     */
    @POST("api/v1/pagos/mixto")
    suspend fun registrarMixto(@Body registrarCobroMixtoRequest: RegistrarCobroMixtoRequest): Response<CobroMixtoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param conceptoId 
     * @return [SaldoResponse]
     */
    @GET("api/v1/pagos/saldo")
    suspend fun saldo(@Query("conceptoId") conceptoId: java.util.UUID): Response<SaldoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param webhookPagoRequest 
     * @param xSignature  (optional)
     * @return [Unit]
     */
    @POST("api/v1/pagos/webhook")
    suspend fun webhook(@Body webhookPagoRequest: WebhookPagoRequest, @Header("X-Signature") xSignature: kotlin.String? = null): Response<Unit>

}
