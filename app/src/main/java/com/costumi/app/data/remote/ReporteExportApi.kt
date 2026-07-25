package com.costumi.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.UUID

/**
 * Exportes de reportes (CSV / PDF). El cliente generado tipa el PDF como `Response<ByteArray>`
 * (sin converter), así que se definen a mano devolviendo el `ResponseBody` crudo.
 */
interface ReporteExportApi {

    @GET("api/v1/reportes/export/inventario-tablero.csv")
    suspend fun inventarioCsv(): Response<ResponseBody>

    @GET("api/v1/reportes/export/inventario-tablero.pdf")
    suspend fun inventarioPdf(): Response<ResponseBody>

    @GET("api/v1/reportes/export/rentas-vencidas.csv")
    suspend fun rentasVencidasCsv(@Query("sucursalId") sucursalId: UUID? = null): Response<ResponseBody>

    @GET("api/v1/reportes/export/rentas-vencidas.pdf")
    suspend fun rentasVencidasPdf(@Query("sucursalId") sucursalId: UUID? = null): Response<ResponseBody>
}
