package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.PagoControllerApi
import com.costumi.apiclient.models.IntentoDePagoDeClienteRequest
import com.costumi.apiclient.models.IntentoDePagoResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pago en línea iniciado por el propio CLIENTE (RF-6.11/14.4): paga con tarjeta el total de su venta/renta
 * y obtiene la URL de checkout de la pasarela. El backend valida que el monto cubra el total pendiente.
 */
@Singleton
class PagoClienteRepository @Inject constructor(
    private val pagoApi: PagoControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun intento(
        empresaId: UUID,
        sucursalId: UUID,
        tipoConcepto: IntentoDePagoDeClienteRequest.TipoConcepto,
        conceptoId: UUID,
        monto: BigDecimal,
        moneda: String = "COP",
    ): RespuestaRed<IntentoDePagoResponse> = withContext(dispatchers.io) {
        val req = IntentoDePagoDeClienteRequest(empresaId, sucursalId, tipoConcepto, conceptoId, monto, moneda)
        ejecutarLlamada(gson) { pagoApi.intentoDeCliente(req) }
    }
}
