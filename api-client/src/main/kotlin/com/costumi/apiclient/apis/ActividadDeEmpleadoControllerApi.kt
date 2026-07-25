package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ActividadResponse
import com.costumi.apiclient.models.ProblemDetail

interface ActividadDeEmpleadoControllerApi {
    /**
     * GET api/v1/empleados/{usuarioId}/actividad
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [ActividadResponse]
     */
    @GET("api/v1/empleados/{usuarioId}/actividad")
    suspend fun actividad(@Path("usuarioId") usuarioId: java.util.UUID): Response<ActividadResponse>

}
