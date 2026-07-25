package com.costumi.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

/**
 * Descarga del contrato de renta en PDF. El cliente generado lo tipa como `Response<ByteArray>`
 * (Retrofit no tiene converter para eso), así que se define a mano devolviendo el `ResponseBody`.
 */
interface ContratoRentaApi {

    @GET("api/v1/rentas/{id}/contrato.pdf")
    suspend fun contrato(@Path("id") id: UUID): Response<ResponseBody>
}
