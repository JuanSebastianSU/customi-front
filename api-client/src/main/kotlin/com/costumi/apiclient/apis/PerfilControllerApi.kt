package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ActualizarPerfilRequest
import com.costumi.apiclient.models.CambiarContrasenaRequest
import com.costumi.apiclient.models.PerfilResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.SubirFotoRequest

interface PerfilControllerApi {
    /**
     * PUT api/v1/perfil
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param actualizarPerfilRequest 
     * @return [PerfilResponse]
     */
    @PUT("api/v1/perfil")
    suspend fun actualizar(@Body actualizarPerfilRequest: ActualizarPerfilRequest): Response<PerfilResponse>

    /**
     * POST api/v1/perfil/contrasena
     * 
     * 
     * Responses:
     *  - 204: No Content
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param cambiarContrasenaRequest 
     * @return [Unit]
     */
    @POST("api/v1/perfil/contrasena")
    suspend fun cambiarContrasena(@Body cambiarContrasenaRequest: CambiarContrasenaRequest): Response<Unit>

    /**
     * POST api/v1/perfil/foto
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param subirFotoRequest  (optional)
     * @return [PerfilResponse]
     */
    @POST("api/v1/perfil/foto")
    suspend fun subirFoto1(@Body subirFotoRequest: SubirFotoRequest? = null): Response<PerfilResponse>

    /**
     * GET api/v1/perfil
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [PerfilResponse]
     */
    @GET("api/v1/perfil")
    suspend fun ver(): Response<PerfilResponse>

}
