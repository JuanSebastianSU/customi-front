package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaAuditoriaResponse

interface AuditoriaControllerApi {
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
     * @return [RespuestaPaginadaAuditoriaResponse]
     */
    @GET("api/v1/auditoria")
    suspend fun listar19(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaAuditoriaResponse>

}
