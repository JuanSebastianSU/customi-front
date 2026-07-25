package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.LoginRequest
import com.costumi.apiclient.models.OlvideRequest
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RefreshRequest
import com.costumi.apiclient.models.RegistroRequest
import com.costumi.apiclient.models.RestablecerRequest
import com.costumi.apiclient.models.TokenResponse
import com.costumi.apiclient.models.UsuarioActualResponse

interface AuthControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param loginRequest 
     * @return [TokenResponse]
     */
    @POST("api/v1/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param refreshRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body refreshRequest: RefreshRequest): Response<Unit>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [UsuarioActualResponse]
     */
    @GET("api/v1/auth/me")
    suspend fun me(): Response<UsuarioActualResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param olvideRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/olvide")
    suspend fun olvide(@Body olvideRequest: OlvideRequest): Response<Unit>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param refreshRequest 
     * @return [TokenResponse]
     */
    @POST("api/v1/auth/refresh")
    suspend fun refrescar(@Body refreshRequest: RefreshRequest): Response<TokenResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param registroRequest 
     * @return [TokenResponse]
     */
    @POST("api/v1/auth/registro")
    suspend fun registro(@Body registroRequest: RegistroRequest): Response<TokenResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param restablecerRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/restablecer")
    suspend fun restablecer(@Body restablecerRequest: RestablecerRequest): Response<Unit>

}
