package com.costumi.app.data.remote

import com.costumi.apiclient.models.SucursalResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.UUID

/**
 * Subida de la foto de una sucursal (la "foto de la tienda"). `multipart/form-data` con el part
 * `archivo`; el cliente generado lo modela como `@Body` (incorrecto), así que se define a mano.
 * 415 si no es imagen, 503 si el almacenamiento (S3) no está configurado.
 */
interface FotoSucursalApi {

    @Multipart
    @POST("api/v1/empresas/{empresaId}/sucursales/{id}/foto")
    suspend fun subirFoto(
        @Path("empresaId") empresaId: UUID,
        @Path("id") id: UUID,
        @Part archivo: MultipartBody.Part,
    ): Response<SucursalResponse>
}
