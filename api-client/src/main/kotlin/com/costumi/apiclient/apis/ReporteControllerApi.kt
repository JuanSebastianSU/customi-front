package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ArticuloRanking
import com.costumi.apiclient.models.DepositosActivosResponse
import com.costumi.apiclient.models.DevolucionesPorCerrarResponse
import com.costumi.apiclient.models.DisfrazRanking
import com.costumi.apiclient.models.EmpleadoVentas
import com.costumi.apiclient.models.GananciaResponse
import com.costumi.apiclient.models.GrupoInventario
import com.costumi.apiclient.models.IngresoDelDia
import com.costumi.apiclient.models.IngresosPorMetodo
import com.costumi.apiclient.models.IngresosResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RentaVencidaResponse
import com.costumi.apiclient.models.ResumenInventario
import com.costumi.apiclient.models.ValorEtiquetaRanking

interface ReporteControllerApi {
    /**
     * GET api/v1/reportes/depositos-activos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [DepositosActivosResponse]
     */
    @GET("api/v1/reportes/depositos-activos")
    suspend fun depositosActivos(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<DepositosActivosResponse>

    /**
     * GET api/v1/reportes/devoluciones-por-cerrar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [DevolucionesPorCerrarResponse]
     */
    @GET("api/v1/reportes/devoluciones-por-cerrar")
    suspend fun devolucionesPorCerrar(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<DevolucionesPorCerrarResponse>

    /**
     * GET api/v1/reportes/disfraces-mas-rentados
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @param limite  (optional, default to 10)
     * @return [kotlin.collections.List<DisfrazRanking>]
     */
    @GET("api/v1/reportes/disfraces-mas-rentados")
    suspend fun disfracesMasRentados(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null, @Query("limite") limite: kotlin.Int? = 10): Response<kotlin.collections.List<DisfrazRanking>>

    /**
     * GET api/v1/reportes/disfraces-mas-vendidos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param limite  (optional, default to 10)
     * @return [kotlin.collections.List<DisfrazRanking>]
     */
    @GET("api/v1/reportes/disfraces-mas-vendidos")
    suspend fun disfracesMasVendidos(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("limite") limite: kotlin.Int? = 10): Response<kotlin.collections.List<DisfrazRanking>>

    /**
     * GET api/v1/reportes/export/inventario-tablero.csv
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.String]
     */
    @GET("api/v1/reportes/export/inventario-tablero.csv")
    suspend fun exportInventarioTablero(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.String>

    /**
     * GET api/v1/reportes/export/inventario-tablero.pdf
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.ByteArray]
     */
    @GET("api/v1/reportes/export/inventario-tablero.pdf")
    suspend fun exportInventarioTableroPdf(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.ByteArray>

    /**
     * GET api/v1/reportes/export/rentas-vencidas.csv
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.String]
     */
    @GET("api/v1/reportes/export/rentas-vencidas.csv")
    suspend fun exportRentasVencidas(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.String>

    /**
     * GET api/v1/reportes/export/rentas-vencidas.pdf
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.ByteArray]
     */
    @GET("api/v1/reportes/export/rentas-vencidas.pdf")
    suspend fun exportRentasVencidasPdf(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.ByteArray>

    /**
     * GET api/v1/reportes/ganancia
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @return [GananciaResponse]
     */
    @GET("api/v1/reportes/ganancia")
    suspend fun ganancia(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null): Response<GananciaResponse>

    /**
     * GET api/v1/reportes/ingresos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @return [IngresosResponse]
     */
    @GET("api/v1/reportes/ingresos")
    suspend fun ingresos(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null): Response<IngresosResponse>

    /**
     * GET api/v1/reportes/ingresos-por-dia
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @return [kotlin.collections.List<IngresoDelDia>]
     */
    @GET("api/v1/reportes/ingresos-por-dia")
    suspend fun ingresosPorDia(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null): Response<kotlin.collections.List<IngresoDelDia>>

    /**
     * GET api/v1/reportes/ingresos-por-metodo
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param desde  (optional)
     * @param hasta  (optional)
     * @param sucursalId  (optional)
     * @return [IngresosPorMetodo]
     */
    @GET("api/v1/reportes/ingresos-por-metodo")
    suspend fun ingresosPorMetodo(@Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null, @Query("sucursalId") sucursalId: java.util.UUID? = null): Response<IngresosPorMetodo>

    /**
     * GET api/v1/reportes/mas-rentados
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @param limite  (optional, default to 10)
     * @return [kotlin.collections.List<ArticuloRanking>]
     */
    @GET("api/v1/reportes/mas-rentados")
    suspend fun masRentados(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null, @Query("limite") limite: kotlin.Int? = 10): Response<kotlin.collections.List<ArticuloRanking>>

    /**
     * GET api/v1/reportes/mas-vendidos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param limite  (optional, default to 10)
     * @return [kotlin.collections.List<ArticuloRanking>]
     */
    @GET("api/v1/reportes/mas-vendidos")
    suspend fun masVendidos(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("limite") limite: kotlin.Int? = 10): Response<kotlin.collections.List<ArticuloRanking>>

    /**
     * GET api/v1/reportes/rentas-por-dia
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @return [kotlin.collections.List<IngresoDelDia>]
     */
    @GET("api/v1/reportes/rentas-por-dia")
    suspend fun rentasPorDia(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null): Response<kotlin.collections.List<IngresoDelDia>>

    /**
     * GET api/v1/reportes/rentas-vencidas
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.collections.List<RentaVencidaResponse>]
     */
    @GET("api/v1/reportes/rentas-vencidas")
    suspend fun rentasVencidas(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.collections.List<RentaVencidaResponse>>

    /**
     * GET api/v1/reportes/inventario/resumen
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [ResumenInventario]
     */
    @GET("api/v1/reportes/inventario/resumen")
    suspend fun resumenDeInventario(): Response<ResumenInventario>

    /**
     * GET api/v1/reportes/inventario/tablero
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.collections.List<GrupoInventario>]
     */
    @GET("api/v1/reportes/inventario/tablero")
    suspend fun tableroDeInventario(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.collections.List<GrupoInventario>>

    /**
     * GET api/v1/reportes/ventas-por-dia
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @param desde  (optional)
     * @param hasta  (optional)
     * @return [kotlin.collections.List<IngresoDelDia>]
     */
    @GET("api/v1/reportes/ventas-por-dia")
    suspend fun ventasPorDia(@Query("sucursalId") sucursalId: java.util.UUID? = null, @Query("desde") desde: java.time.LocalDate? = null, @Query("hasta") hasta: java.time.LocalDate? = null): Response<kotlin.collections.List<IngresoDelDia>>

    /**
     * GET api/v1/reportes/ventas-por-empleado
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId  (optional)
     * @return [kotlin.collections.List<EmpleadoVentas>]
     */
    @GET("api/v1/reportes/ventas-por-empleado")
    suspend fun ventasPorEmpleado(@Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.collections.List<EmpleadoVentas>>

    /**
     * GET api/v1/reportes/ventas-por-etiqueta
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoEtiquetaId 
     * @param sucursalId  (optional)
     * @return [kotlin.collections.List<ValorEtiquetaRanking>]
     */
    @GET("api/v1/reportes/ventas-por-etiqueta")
    suspend fun ventasPorEtiqueta(@Query("tipoEtiquetaId") tipoEtiquetaId: java.util.UUID, @Query("sucursalId") sucursalId: java.util.UUID? = null): Response<kotlin.collections.List<ValorEtiquetaRanking>>

}
