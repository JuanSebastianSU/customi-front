package com.costumi.app.data.remote

import com.costumi.apiclient.models.EmpresaResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Logo y portada de la propia tienda (RF-18.3): `multipart/form-data` con el part `archivo`. El cliente
 * generado los modela como `@Body` (incorrecto), así que se definen a mano (como [FotoPrendaApi]). La
 * empresa sale del token. 415 si no es imagen, 503 si el almacenamiento (S3) no está configurado.
 */
interface FotoEmpresaApi {

    @Multipart
    @POST("api/v1/empresas/mia/logo")
    suspend fun subirLogo(@Part archivo: MultipartBody.Part): Response<EmpresaResponse>

    @Multipart
    @POST("api/v1/empresas/mia/portada")
    suspend fun subirPortada(@Part archivo: MultipartBody.Part): Response<EmpresaResponse>
}
