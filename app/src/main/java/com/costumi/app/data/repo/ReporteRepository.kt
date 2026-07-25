package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ReporteExportApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.ReporteControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.apis.TipoEtiquetaControllerApi
import com.costumi.apiclient.models.ArticuloRanking
import com.costumi.apiclient.models.DisfrazRanking
import com.costumi.apiclient.models.DepositosActivosResponse
import com.costumi.apiclient.models.EmpleadoVentas
import com.costumi.apiclient.models.GananciaResponse
import com.costumi.apiclient.models.GrupoInventario
import com.costumi.apiclient.models.IngresosPorMetodo
import com.costumi.apiclient.models.IngresosResponse
import com.costumi.apiclient.models.RentaVencidaResponse
import com.costumi.apiclient.models.ResumenInventario
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaRanking
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** KPIs del Dashboard de Gestion: ingresos + resumen de inventario en un solo modelo. */
data class ResumenDashboard(
    val ingresoTotal: BigDecimal?,
    val ingresoRenta: BigDecimal?,
    val ingresoVenta: BigDecimal?,
    val valorInventario: BigDecimal?,
    val totalUnidades: Long?,
    val disponibles: Long?,
    val rentadasAhora: Long?,
    val danadas: Long?,
    val enLimpieza: Long?,
    val perdidas: Long?,
    val tasaUtilizacion: BigDecimal?,
)

/** Tablero de Reportes: ingresos, ganancia, ingresos por método, depósitos, rankings y listas. */
data class ReporteCompleto(
    val ingresos: IngresosResponse?,
    val ganancia: GananciaResponse?,
    val porMetodo: IngresosPorMetodo?,
    val depositos: DepositosActivosResponse?,
    val masVendidos: List<ArticuloRanking>,
    val masRentados: List<ArticuloRanking>,
    /** Rankings por DISFRAZ (cuentan disfraces, no las piezas en que se resuelven al cobrar). */
    val disfracesMasVendidos: List<DisfrazRanking>,
    val disfracesMasRentados: List<DisfrazRanking>,
    val rentasVencidas: List<RentaVencidaResponse>,
    val ventasPorEmpleado: List<EmpleadoVentas>,
    val tablero: List<GrupoInventario>,
    /** Serie de ingresos por día (A9), para ver la evolución en el rango elegido. */
    val serie: List<com.costumi.apiclient.models.IngresoDelDia> = emptyList(),
)

/**
 * Reportes del modo GESTION (rol DUENO/ENCARGADO). El Dashboard combina
 * `/reportes/ingresos` y `/reportes/inventario/resumen`; el tablero de Reportes suma el resto.
 */
@Singleton
class ReporteRepository @Inject constructor(
    private val reporteApi: ReporteControllerApi,
    private val exportApi: ReporteExportApi,
    private val tipoEtiquetaApi: TipoEtiquetaControllerApi,
    private val authApi: AuthControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Trae los dos reportes del Dashboard; si cualquiera falla, propaga ese error. */
    suspend fun dashboard(): RespuestaRed<ResumenDashboard> = withContext(dispatchers.io) {
        val ingresos = ejecutarLlamada(gson) { reporteApi.ingresos() }
        if (ingresos is RespuestaRed.Fallo) return@withContext ingresos
        val resumen = ejecutarLlamada(gson) { reporteApi.resumenDeInventario() }
        if (resumen is RespuestaRed.Fallo) return@withContext resumen

        val i = (ingresos as RespuestaRed.Exito<IngresosResponse>).data
        val r = (resumen as RespuestaRed.Exito<ResumenInventario>).data
        RespuestaRed.Exito(
            ResumenDashboard(
                ingresoTotal = i.total,
                ingresoRenta = i.ingresosPorRenta,
                ingresoVenta = i.ingresosPorVenta,
                valorInventario = r.valorInventario,
                totalUnidades = r.totalUnidades,
                disponibles = r.disponibles,
                rentadasAhora = r.rentadasAhora,
                danadas = r.danadas,
                enLimpieza = r.enLimpieza,
                perdidas = r.perdidas,
                tasaUtilizacion = r.tasaUtilizacion,
            ),
        )
    }

    /**
     * Tablero completo de reportes. Ingresos es obligatorio (si falla, error); el resto se carga
     * en paralelo y best-effort (un reporte que falle no borra el tablero).
     */
    suspend fun reportes(
        sucursalId: UUID? = null,
        desde: LocalDate? = null,
        hasta: LocalDate? = null,
    ): RespuestaRed<ReporteCompleto> = withContext(dispatchers.io) {
        coroutineScope {
            // Todos aceptan sucursal (null = toda la empresa); ingresos/ganancia también respetan la sucursal ahora.
            val ingresosD = async { ejecutarLlamada(gson) { reporteApi.ingresos(sucursalId) } }
            val gananciaD = async { ejecutarLlamada(gson) { reporteApi.ganancia(sucursalId) } }
            val metodoD = async { ejecutarLlamada(gson) { reporteApi.ingresosPorMetodo(desde, hasta, sucursalId) } }
            val depositosD = async { ejecutarLlamada(gson) { reporteApi.depositosActivos(sucursalId) } }
            val vendidosD = async { ejecutarLlamada(gson) { reporteApi.masVendidos(sucursalId, 5) } }
            val rentadosD = async { ejecutarLlamada(gson) { reporteApi.masRentados(sucursalId, desde, hasta, 5) } }
            val disfVendidosD = async { ejecutarLlamada(gson) { reporteApi.disfracesMasVendidos(sucursalId, 5) } }
            val disfRentadosD = async {
                ejecutarLlamada(gson) { reporteApi.disfracesMasRentados(sucursalId, desde, hasta, 5) }
            }
            val vencidasD = async { ejecutarLlamada(gson) { reporteApi.rentasVencidas(sucursalId) } }
            val empleadoD = async { ejecutarLlamada(gson) { reporteApi.ventasPorEmpleado(sucursalId) } }
            val tableroD = async { ejecutarLlamada(gson) { reporteApi.tableroDeInventario(sucursalId) } }
            val serieD = async { ejecutarLlamada(gson) { reporteApi.ingresosPorDia(sucursalId, desde, hasta) } }

            val ingresos = ingresosD.await()
            if (ingresos is RespuestaRed.Fallo) return@coroutineScope ingresos
            RespuestaRed.Exito(
                ReporteCompleto(
                    ingresos = (ingresos as RespuestaRed.Exito).data,
                    ganancia = gananciaD.await().datoONull(),
                    porMetodo = metodoD.await().datoONull(),
                    depositos = depositosD.await().datoONull(),
                    masVendidos = vendidosD.await().datoONull().orEmpty(),
                    masRentados = rentadosD.await().datoONull().orEmpty(),
                    disfracesMasVendidos = disfVendidosD.await().datoONull().orEmpty(),
                    disfracesMasRentados = disfRentadosD.await().datoONull().orEmpty(),
                    rentasVencidas = vencidasD.await().datoONull().orEmpty(),
                    ventasPorEmpleado = empleadoD.await().datoONull().orEmpty(),
                    tablero = tableroD.await().datoONull().orEmpty(),
                    serie = serieD.await().datoONull().orEmpty(),
                ),
            )
        }
    }

    /** Tipos de etiqueta activos (para el selector de "ventas por etiqueta"). */
    suspend fun tiposEtiqueta(): RespuestaRed<List<TipoEtiquetaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { tipoEtiquetaApi.listar1() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
        }
    }

    /** Ranking de ventas por valor de un tipo de etiqueta (RF-8), opcionalmente por sucursal. */
    suspend fun ventasPorEtiqueta(tipoId: UUID, sucursalId: UUID? = null): RespuestaRed<List<ValorEtiquetaRanking>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { reporteApi.ventasPorEtiqueta(tipoId, sucursalId) } }

    /** Sucursales de la empresa (para el filtro de reportes). */
    suspend fun sucursalesReporte(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> me.data.empresaId?.takeIf { it.isNotBlank() }?.let { empresaId ->
                when (val r = ejecutarLlamada(gson) { sucursalApi.listar9(UUID.fromString(empresaId)) }) {
                    is RespuestaRed.Fallo -> r
                    is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
                }
            } ?: RespuestaRed.Exito(emptyList())
        }
    }

    private fun <T> RespuestaRed<T>.datoONull(): T? = (this as? RespuestaRed.Exito)?.data

    /** Rentas que ya pasaron su fecha de devolucion (alerta del panel y del tablero de reportes). */
    suspend fun rentasVencidas(sucursalId: UUID? = null): RespuestaRed<List<RentaVencidaResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { reporteApi.rentasVencidas(sucursalId) } }

    suspend fun exportarInventario(pdf: Boolean): RespuestaRed<ByteArray> = withContext(dispatchers.io) {
        val r = ejecutarLlamada(gson) { if (pdf) exportApi.inventarioPdf() else exportApi.inventarioCsv() }
        when (r) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.bytes())
        }
    }

    suspend fun exportarRentasVencidas(pdf: Boolean, sucursalId: UUID? = null): RespuestaRed<ByteArray> =
        withContext(dispatchers.io) {
            val r = ejecutarLlamada(gson) {
                if (pdf) exportApi.rentasVencidasPdf(sucursalId) else exportApi.rentasVencidasCsv(sucursalId)
            }
            when (r) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.bytes())
            }
        }
}
