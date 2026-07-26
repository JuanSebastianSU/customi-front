package com.costumi.app.data.remote

import com.costumi.apiclient.models.PerfilResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Subida de la foto de perfil del propio usuario. `multipart/form-data` con el part `archivo`; el
 * cliente generado lo modela como `@Body` (incorrecto para Retrofit), así que se define a mano
 * (mismo caso que [FotoPrendaApi]/[FotoDisfrazApi]). 415 si no es imagen, 503 si S3 no está.
 */
interface FotoPerfilApi {

    @Multipart
    @POST("api/v1/perfil/foto")
    suspend fun subirFoto(
        @Part archivo: MultipartBody.Part,
    ): Response<PerfilResponse>
}
