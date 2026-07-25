package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.TipoError
import com.costumi.app.core.mapear
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
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun miHistorial(): RespuestaRed<List<HistorialItem>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { clienteApi.miHistorial() }.mapear { it.contenido.orEmpty() }
    }

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
