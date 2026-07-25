package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AceptarInvitacionRequest
import com.costumi.apiclient.models.InvitacionVistaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.TokenResponse

interface InvitacionControllerApi {
    /**
     * POST api/v1/invitaciones/aceptar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param aceptarInvitacionRequest 
     * @return [TokenResponse]
     */
    @POST("api/v1/invitaciones/aceptar")
    suspend fun aceptar(@Body aceptarInvitacionRequest: AceptarInvitacionRequest): Response<TokenResponse>

    /**
     * GET api/v1/invitaciones/{token}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param token 
     * @return [InvitacionVistaResponse]
     */
    @GET("api/v1/invitaciones/{token}")
    suspend fun ver1(@Path("token") token: kotlin.String): Response<InvitacionVistaResponse>

}
