package com.costumi.app.data.remote

import com.costumi.apiclient.models.PrendaResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.UUID

/**
 * Subida de foto de prenda (RF-2.9). El endpoint es `multipart/form-data` con el part `archivo`;
 * el cliente generado lo modela como `@Body File` (incorrecto para Retrofit), así que se define
 * a mano aquí. Gateado: 415 si no es imagen, 503 si el almacenamiento (S3) no está configurado.
 */
interface FotoPrendaApi {

    @Multipart
    @POST("api/v1/prendas/{id}/foto")
    suspend fun subirFoto(
        @Path("id") id: UUID,
        @Part archivo: MultipartBody.Part,
    ): Response<PrendaResponse>
}
