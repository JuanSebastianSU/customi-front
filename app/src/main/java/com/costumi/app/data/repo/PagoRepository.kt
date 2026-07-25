package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ComprobantePagoApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.PagoControllerApi
import com.costumi.apiclient.apis.RentaControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.apis.VentaControllerApi
import com.costumi.apiclient.models.ComprobanteResponse
import com.costumi.apiclient.models.CobroMixtoResponse
import com.costumi.apiclient.models.IntentoDePagoRequest
import com.costumi.apiclient.models.IntentoDePagoResponse
import com.costumi.apiclient.models.PagoResponse
import com.costumi.apiclient.models.RegistrarCobroMixtoRequest
import com.costumi.apiclient.models.RegistrarPagoRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Tipo de concepto que se cobra: una VENTA o una RENTA. */
enum class TipoConcepto { VENTA, RENTA }

/** Operación cobrable, unificada para el selector de Pagos (venta o renta). */
data class OperacionPago(
    val id: UUID,
    val tipo: TipoConcepto,
    val sucursalId: UUID?,
    val total: BigDecimal?,
    val estado: String?,
    /** Codigo de retiro (V-XXXX / R-XXXX): lo que se coteja contra el que muestra el cliente al retirar. */
    val codigoRetiro: String?,
)

/** Un artículo del concepto que se cobra (línea de la venta/renta), con foto para el detalle de cobros. */
data class ArticuloConcepto(
    val fotoUrl: String?,
    val nombre: String,
    val cantidad: Int,
    val precio: BigDecimal?,
    val subtotal: BigDecimal?,
    val porDia: Boolean,
)

/** Pagos del modo GESTION: libro mayor por concepto (venta/renta), cobros y comprobante. */
@Singleton
class PagoRepository @Inject constructor(
    private val pagoApi: PagoControllerApi,
    private val comprobanteApi: ComprobantePagoApi,
    private val ventaApi: VentaControllerApi,
    private val rentaApi: RentaControllerApi,
    private val authApi: AuthControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Operaciones cobrables del tipo pedido, paginadas (reusa los paging source de ventas/rentas). */
    /** [buscar] filtra por codigo de retiro (el que el cliente muestra en la tienda). */
    fun operaciones(tipo: TipoConcepto, buscar: String? = null): Flow<PagingData<OperacionPago>> {
        val config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        return when (tipo) {
            TipoConcepto.VENTA -> Pager(config) { VentasPagingSource(ventaApi, gson, buscar) }.flow
                .map { data -> data.map { OperacionPago(it.id!!, TipoConcepto.VENTA, it.sucursalId, it.total, it.estado, it.codigoRetiro) } }
            TipoConcepto.RENTA -> Pager(config) { RentasPagingSource(rentaApi, gson, buscar) }.flow
                .map { data -> data.map { OperacionPago(it.id!!, TipoConcepto.RENTA, it.sucursalId, it.importe, it.estado, it.codigoRetiro) } }
        }
    }

    /** Comprobante consolidado del concepto: pagos, cobrado/reembolsado, saldo neto, depósito e impuesto. */
    suspend fun comprobante(conceptoId: UUID): RespuestaRed<ComprobanteResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { pagoApi.comprobante(conceptoId) } }

    /** Artículos (con foto) de la operación que se cobra, trayendo la venta/renta por id. */
    suspend fun articulos(tipo: TipoConcepto, conceptoId: UUID): RespuestaRed<List<ArticuloConcepto>> =
        withContext(dispatchers.io) {
            when (tipo) {
                TipoConcepto.VENTA -> when (val r = ejecutarLlamada(gson) { ventaApi.porId(conceptoId) }) {
                    is RespuestaRed.Fallo -> r
                    is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.lineas.orEmpty().map {
                        ArticuloConcepto(it.fotoUrl, it.nombre ?: "Articulo", it.cantidad ?: 1,
                            it.precioUnitario, it.subtotal, porDia = false)
                    })
                }
                TipoConcepto.RENTA -> when (val r = ejecutarLlamada(gson) { rentaApi.porId1(conceptoId) }) {
                    is RespuestaRed.Fallo -> r
                    is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.lineas.orEmpty().map {
                        ArticuloConcepto(it.fotoUrl, it.nombre ?: "Articulo", it.cantidad ?: 1,
                            it.precioPorDia, null, porDia = true)
                    })
                }
            }
        }

    suspend fun registrar(req: RegistrarPagoRequest): RespuestaRed<PagoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { pagoApi.registrar1(req) } }

    suspend fun registrarMixto(req: RegistrarCobroMixtoRequest): RespuestaRed<CobroMixtoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { pagoApi.registrarMixto(req) } }

    /** Inicia un pago en línea: devuelve la URL del checkout alojado (o el error si la pasarela no está lista). */
    suspend fun intento(req: IntentoDePagoRequest): RespuestaRed<IntentoDePagoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { pagoApi.intento(req) } }

    /** Comprobante en PDF como bytes. */
    suspend fun comprobantePdf(conceptoId: UUID): RespuestaRed<ByteArray> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { comprobanteApi.comprobante(conceptoId) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.bytes())
        }
    }

    /** Sucursal por defecto (la primera activa de la empresa) cuando el concepto no la trae. */
    suspend fun sucursalPorDefecto(): RespuestaRed<UUID> = withContext(dispatchers.io) {
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> {
                val empresaId = me.data.empresaId
                if (empresaId.isNullOrBlank()) {
                    RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tu usuario no tiene empresa asignada."))
                } else {
                    when (val r = ejecutarLlamada(gson) { sucursalApi.listar9(UUID.fromString(empresaId)) }) {
                        is RespuestaRed.Fallo -> r
                        is RespuestaRed.Exito -> {
                            val id = r.data.firstOrNull { it.archivada != true }?.id
                            if (id == null) RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "No hay sucursales activas."))
                            else RespuestaRed.Exito(id)
                        }
                    }
                }
            }
        }
    }
}
