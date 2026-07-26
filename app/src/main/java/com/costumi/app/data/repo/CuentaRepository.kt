package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.TipoError
import com.costumi.app.core.mapear
import com.costumi.app.data.local.dao.PedidoDao
import com.costumi.app.data.local.entity.PedidoEntity
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.apis.ReembolsoControllerApi
import com.costumi.apiclient.models.EmpresaResponse
import com.costumi.apiclient.models.HistorialItem
import com.costumi.apiclient.models.RegistrarEmpresaRequest
import com.costumi.apiclient.models.SolicitarReembolsoDeClienteRequest
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuenta del CLIENTE del marketplace: "Mis Pedidos" (historial en todas las tiendas) y "Registrar
 * mi tienda" (abrir una empresa, que el SUPERADMIN aprueba y lo promueve a Dueño).
 */
@Singleton
class CuentaRepository @Inject constructor(
    private val clienteApi: ClienteControllerApi,
    private val empresaApi: EmpresaControllerApi,
    private val reembolsoApi: ReembolsoControllerApi,
    private val pedidoDao: PedidoDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun miHistorial(): RespuestaRed<List<HistorialItem>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { clienteApi.miHistorial() }.mapear { it.contenido.orEmpty() }
    }

    /**
     * Historial cacheado (Room, `PLAN_ROOM_OFFLINE.md` A4). La UI observa esto y el repo sincroniza con
     * [refrescarHistorial]. Reconstruye el `HistorialItem` completo (con sus líneas) desde el JSON guardado.
     */
    fun observarHistorial(): Flow<List<HistorialItem>> =
        pedidoDao.observarTodos().map { lista -> lista.mapNotNull { it.aItem() } }

    /** Trae el historial desde la red y **escribe en Room** (los datos llegan por el Flow). Solo devuelve el error. */
    suspend fun refrescarHistorial(): RespuestaRed<Unit> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { clienteApi.miHistorial() }.mapear { it.contenido.orEmpty() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                pedidoDao.reemplazar(r.data.mapIndexedNotNull { i, item -> item.aEntity(i) })
                RespuestaRed.Exito(Unit)
            }
        }
    }

    /** Borra el historial cacheado (se llama al cerrar sesión, norma N1). */
    suspend fun limpiarHistorial() = withContext(dispatchers.io) { pedidoDao.limpiar() }

    private fun HistorialItem.aEntity(orden: Int): PedidoEntity? {
        val id = operacionId?.toString() ?: return null
        return PedidoEntity(operacionId = id, orden = orden, json = gson.toJson(this))
    }

    private fun PedidoEntity.aItem(): HistorialItem? =
        runCatching { gson.fromJson(json, HistorialItem::class.java) }.getOrNull()

    /** Solicita el reembolso de una operación propia (RF-18.9): la tienda la aprueba/rechaza. */
    suspend fun solicitarReembolso(
        pedido: HistorialItem,
        motivo: String,
    ): RespuestaRed<SolicitudDeReembolsoResponse> = withContext(dispatchers.io) {
        val empresaId = pedido.empresaId
        val conceptoId = pedido.operacionId
        val monto = pedido.monto
        val tipo = when (pedido.tipo?.uppercase()) {
            "RENTA" -> SolicitarReembolsoDeClienteRequest.TipoConcepto.RENTA
            "VENTA" -> SolicitarReembolsoDeClienteRequest.TipoConcepto.VENTA
            else -> null
        }
        if (empresaId == null || conceptoId == null || monto == null || tipo == null) {
            return@withContext RespuestaRed.Fallo(
                ErrorApi(TipoError.DESCONOCIDO, "No se puede solicitar el reembolso de este pedido."),
            )
        }
        val req = SolicitarReembolsoDeClienteRequest(empresaId, tipo, conceptoId, monto, motivo)
        ejecutarLlamada(gson) { reembolsoApi.solicitarComoCliente(req) }
    }

    suspend fun registrarTienda(
        nombre: String,
        ubicacion: String?,
        contacto: String?,
    ): RespuestaRed<EmpresaResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) {
            empresaApi.registrar2(RegistrarEmpresaRequest(nombre, ubicacion, contacto))
        }
    }
}
