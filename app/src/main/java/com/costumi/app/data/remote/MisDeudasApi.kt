package com.costumi.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Lo que el propio cliente debe: multas y saldos, en todas las tiendas (`GET /clientes/me/deudas`,
 * RF-7/11.5). Se define a mano porque el endpoint es nuevo y el `:api-client` se genera del contrato ya
 * desplegado.
 *
 * Se resuelve por el usuario del token, asi que no recibe ningun id.
 */
interface MisDeudasApi {

    @GET("api/v1/clientes/me/deudas")
    suspend fun mias(): Response<List<MiDeudaDto>>
}

/**
 * Una deuda por renta, con la tienda a la que corresponde.
 *
 * Trae el **desglose** y no solo el total: una multa sin explicacion es justo lo que el cliente va a
 * venir a reclamar. `multa` es lo que los cargos superaron al deposito; `saldo`, lo que falta pagar.
 */
data class MiDeudaDto(
    val empresaId: UUID?,
    val empresaNombre: String?,
    val rentaId: UUID?,
    val codigoRetiro: String?,
    val estado: String?,
    val fechaRetiro: LocalDate?,
    val fechaDevolucion: LocalDate?,
    val importe: BigDecimal?,
    val cargoPorDanos: BigDecimal?,
    val cargoPorRetraso: BigDecimal?,
    val deposito: BigDecimal?,
    val multa: BigDecimal?,
    val pagado: BigDecimal?,
    val saldo: BigDecimal?,
)
