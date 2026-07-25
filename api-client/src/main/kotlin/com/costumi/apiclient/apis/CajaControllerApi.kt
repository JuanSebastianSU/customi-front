package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AbrirTurnoRequest
import com.costumi.apiclient.models.CerrarTurnoRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarMovimientoRequest
import com.costumi.apiclient.models.TurnoResponse

interface CajaControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param abrirTurnoRequest 
     * @return [TurnoResponse]
     */
    @POST("api/v1/caja/turnos")
    suspend fun abrir(@Body abrirTurnoRequest: AbrirTurnoRequest): Response<TurnoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param turnoId 
     * @param cerrarTurnoRequest 
     * @return [TurnoResponse]
     */
    @POST("api/v1/caja/turnos/{turnoId}/cerrar")
    suspend fun cerrar1(@Path("turnoId") turnoId: java.util.UUID, @Body cerrarTurnoRequest: CerrarTurnoRequest): Response<TurnoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<TurnoResponse>]
     */
    @GET("api/v1/caja/turnos")
    suspend fun listar16(): Response<kotlin.collections.List<TurnoResponse>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param turnoId 
     * @param registrarMovimientoRequest 
     * @return [TurnoResponse]
     */
    @POST("api/v1/caja/turnos/{turnoId}/movimientos")
    suspend fun mover1(@Path("turnoId") turnoId: java.util.UUID, @Body registrarMovimientoRequest: RegistrarMovimientoRequest): Response<TurnoResponse>

}
