package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ActualizarPlantillaRequest
import com.costumi.apiclient.models.PlantillaResponse
import com.costumi.apiclient.models.ProblemDetail

interface PlantillaNotificacionControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipo 
     * @param actualizarPlantillaRequest 
     * @return [PlantillaResponse]
     */
    @PUT("api/v1/notificaciones/plantillas/{tipo}")
    suspend fun actualizar1(@Path("tipo") tipo: kotlin.String, @Body actualizarPlantillaRequest: ActualizarPlantillaRequest): Response<PlantillaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<PlantillaResponse>]
     */
    @GET("api/v1/notificaciones/plantillas")
    suspend fun listar17(): Response<kotlin.collections.List<PlantillaResponse>>

}
