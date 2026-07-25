package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CapacidadDto
import com.costumi.apiclient.models.EstablecerPermisoRequest
import com.costumi.apiclient.models.ProblemDetail

interface PermisosEmpleadoControllerApi {
    /**
     * PUT api/v1/empleados/{usuarioId}/permisos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @param establecerPermisoRequest 
     * @return [Unit]
     */
    @PUT("api/v1/empleados/{usuarioId}/permisos")
    suspend fun establecer(@Path("usuarioId") usuarioId: java.util.UUID, @Body establecerPermisoRequest: EstablecerPermisoRequest): Response<Unit>

    /**
     * GET api/v1/empleados/{usuarioId}/permisos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [kotlin.collections.List<CapacidadDto>]
     */
    @GET("api/v1/empleados/{usuarioId}/permisos")
    suspend fun matriz(@Path("usuarioId") usuarioId: java.util.UUID): Response<kotlin.collections.List<CapacidadDto>>

}
