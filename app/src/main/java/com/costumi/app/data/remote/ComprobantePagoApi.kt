package com.costumi.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.UUID

/**
 * Descarga del comprobante de pago en PDF. El cliente generado lo tipa como `Response<ByteArray>`
 * (Retrofit no tiene converter para eso), así que se define a mano devolviendo el `ResponseBody`.
 */
interface ComprobantePagoApi {

    @GET("api/v1/pagos/comprobante.pdf")
    suspend fun comprobante(@Query("conceptoId") conceptoId: UUID): Response<ResponseBody>
}
