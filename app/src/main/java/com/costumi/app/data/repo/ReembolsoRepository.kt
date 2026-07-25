package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ReembolsoControllerApi
import com.costumi.apiclient.models.DecisionReembolsoRequest
import com.costumi.apiclient.models.SolicitarReembolsoRequest
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Reembolsos del modo GESTION: bandeja de solicitudes y decisión (aprobar/rechazar), RF-4.5/6.9. */
@Singleton
class ReembolsoRepository @Inject constructor(
    private val api: ReembolsoControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Bandeja de solicitudes de la empresa (pendientes/aprobadas/rechazadas). */
    suspend fun bandeja(buscar: String? = null): RespuestaRed<List<SolicitudDeReembolsoResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.listar3(buscar = buscar?.ifBlank { null }, tamano = TAMANO) }
                .mapear { it.contenido.orEmpty() }
        }

    /** Aprobar dispara el reembolso; exige que el item ya esté devuelto. El motivo es obligatorio. */
    suspend fun aprobar(id: UUID, motivo: String): RespuestaRed<SolicitudDeReembolsoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.aprobar(id, DecisionReembolsoRequest(motivo)) } }

    suspend fun rechazar(id: UUID, motivo: String): RespuestaRed<SolicitudDeReembolsoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.rechazar(id, DecisionReembolsoRequest(motivo)) } }

    /** El personal registra la solicitud del cliente para una operación con saldo pagado. */
    suspend fun solicitar(
        tipoConcepto: SolicitarReembolsoRequest.TipoConcepto,
        conceptoId: UUID,
        monto: BigDecimal,
        motivo: String?,
    ): RespuestaRed<SolicitudDeReembolsoResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) {
            api.solicitar(SolicitarReembolsoRequest(tipoConcepto, conceptoId, monto, null, motivo?.ifBlank { null }))
        }
    }

    private companion object {
        /** Estas listas se muestran completas en pantalla; se pide una pagina grande y se busca en el servidor. */
        const val TAMANO = 100
    }
}
