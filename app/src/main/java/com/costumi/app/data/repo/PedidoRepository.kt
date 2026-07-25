package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.CarritoAbiertoDto
import com.costumi.app.data.remote.MisCarritosApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.CarritoControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.CheckoutRentaResponse
import com.costumi.apiclient.models.CheckoutRequest
import com.costumi.apiclient.models.CheckoutResponse
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SucursalVitrinaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pedidos del cliente en el marketplace: resolver la sucursal de una tienda, carrito y checkout.
 * El carrito esta acotado a (sucursal, tipo RENTA/VENTA); el backend resuelve la ficha del cliente
 * desde el token, por eso no enviamos clienteId.
 */
@Singleton
class PedidoRepository @Inject constructor(
    private val carritoApi: CarritoControllerApi,
    private val marketplaceApi: MarketplaceControllerApi,
    private val misCarritosApi: MisCarritosApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /**
     * Sucursales (puntos de retiro) de una tienda, desde la vitrina pública del marketplace
     * (RF-18.5). Ya vienen filtradas a las activas; el cliente elige dónde retirar.
     */
    suspend fun sucursales(empresaId: String): RespuestaRed<List<SucursalVitrinaResponse>> =
        withContext(dispatchers.io) {
            val uuid = empresaId.aUuid()
                ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."))
            ejecutarLlamada(gson) { marketplaceApi.sucursales(uuid) }
        }

    suspend fun agregarAlCarrito(
        empresaId: String,
        sucursalId: String,
        prendaId: String,
        tipo: AgregarItemRequest.Tipo,
        cantidad: Int,
        fechaRetiro: LocalDate?,
        fechaDevolucion: LocalDate?,
    ): RespuestaRed<CarritoResponse> = withContext(dispatchers.io) {
        val emp = empresaId.aUuid()
        val suc = sucursalId.aUuid()
        val pr = prendaId.aUuid()
        if (emp == null || suc == null || pr == null) {
            return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Datos del articulo no validos."))
        }
        val req = AgregarItemRequest(
            sucursalId = suc,
            tipo = tipo,
            prendaId = pr,
            empresaId = emp,
            cantidad = cantidad,
            fechaRetiro = fechaRetiro,
            fechaDevolucion = fechaDevolucion,
        )
        ejecutarLlamada(gson) { carritoApi.agregarItem(req) }
    }

    /** Agrega un DISFRAZ al carrito con la elección de prenda por slot ([selecciones]). */
    suspend fun agregarDisfrazAlCarrito(
        empresaId: String,
        sucursalId: String,
        disfrazId: String,
        tipo: AgregarItemRequest.Tipo,
        selecciones: List<SeleccionSlotDto>,
        cantidad: Int,
        fechaRetiro: LocalDate?,
        fechaDevolucion: LocalDate?,
    ): RespuestaRed<CarritoResponse> = withContext(dispatchers.io) {
        val emp = empresaId.aUuid()
        val suc = sucursalId.aUuid()
        val dis = disfrazId.aUuid()
        if (emp == null || suc == null || dis == null) {
            return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Datos del disfraz no validos."))
        }
        val req = AgregarItemRequest(
            sucursalId = suc,
            tipo = tipo,
            disfrazId = dis,
            empresaId = emp,
            selecciones = selecciones,
            cantidad = cantidad.coerceAtLeast(1),
            fechaRetiro = fechaRetiro,
            fechaDevolucion = fechaDevolucion,
        )
        ejecutarLlamada(gson) { carritoApi.agregarItem(req) }
    }

    suspend fun carritoPendiente(
        sucursalId: String,
        tipo: CarritoControllerApi.TipoPendiente,
        empresaId: String,
    ): RespuestaRed<CarritoResponse> = withContext(dispatchers.io) {
        val suc = sucursalId.aUuid()
            ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Sucursal no valida."))
        ejecutarLlamada(gson) { carritoApi.pendiente(suc, tipo, empresaId.aUuid()) }
    }

    /** Quita una linea del carrito; el backend devuelve el carrito ya sin ella. */
    suspend fun quitarDelCarrito(
        lineaId: String,
        sucursalId: String,
        tipo: CarritoControllerApi.TipoQuitarItem,
        empresaId: String,
    ): RespuestaRed<CarritoResponse> = withContext(dispatchers.io) {
        val linea = lineaId.aUuid()
            ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Articulo no valido."))
        val suc = sucursalId.aUuid()
            ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Sucursal no valida."))
        ejecutarLlamada(gson) { carritoApi.quitarItem(linea, suc, tipo, empresaId.aUuid()) }
    }

    suspend fun checkoutVenta(empresaId: String, sucursalId: String): RespuestaRed<CheckoutResponse> =
        withContext(dispatchers.io) {
            val suc = sucursalId.aUuid()
                ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Sucursal no valida."))
            ejecutarLlamada(gson) { carritoApi.checkout(CheckoutRequest(sucursalId = suc, empresaId = empresaId.aUuid())) }
        }

    suspend fun checkoutRenta(empresaId: String, sucursalId: String): RespuestaRed<CheckoutRentaResponse> =
        withContext(dispatchers.io) {
            val suc = sucursalId.aUuid()
                ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Sucursal no valida."))
            ejecutarLlamada(gson) { carritoApi.checkoutRenta(CheckoutRequest(sucursalId = suc, empresaId = empresaId.aUuid())) }
        }

    /**
     * Los carritos abiertos del cliente en todas las tiendas. Sin esto no habia forma de volver a uno:
     * consultarlo exige saber de antemano empresa, sucursal y tipo.
     */
    suspend fun misCarritos(): RespuestaRed<List<CarritoAbiertoDto>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { misCarritosApi.mios() } }

    private fun String.aUuid(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
}
