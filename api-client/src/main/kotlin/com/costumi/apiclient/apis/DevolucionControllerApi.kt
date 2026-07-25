package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.DevolucionResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RegistrarDevolucionRequest
import com.costumi.apiclient.models.RespuestaPaginadaDevolucionResponse

interface DevolucionControllerApi {
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
     * @return [RespuestaPaginadaDevolucionResponse]
     */
    @GET("api/v1/devoluciones")
    suspend fun listar13(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaDevolucionResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registrarDevolucionRequest 
     * @return [DevolucionResponse]
     */
    @POST("api/v1/devoluciones")
    suspend fun registrar4(@Body registrarDevolucionRequest: RegistrarDevolucionRequest): Response<DevolucionResponse>

}
