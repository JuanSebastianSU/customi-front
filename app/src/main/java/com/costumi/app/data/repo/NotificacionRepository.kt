package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.NotificacionControllerApi
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.EnviarNotificacionRequest
import com.costumi.apiclient.models.EstadoDeCanales
import com.costumi.apiclient.models.NotificacionResponse
import com.costumi.apiclient.models.RecordatorioResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Notificaciones del modo GESTION: bandeja, envío manual y recordatorio de vencidas. */
@Singleton
class NotificacionRepository @Inject constructor(
    private val notificacionApi: NotificacionControllerApi,
    private val clienteApi: ClienteControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /**
     * Bandeja paginada (Paging 3, scroll infinito). [buscar] filtra en el servidor por el mensaje, así
     * que encuentra el aviso esté en la página que esté. Antes se pedía una sola página de 100 y el
     * registro #101 quedaba invisible.
     */
    fun notificaciones(buscar: String? = null): Flow<PagingData<NotificacionResponse>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            PaginaRemotaPagingSource(
                pedir = { pagina, tamano ->
                    ejecutarLlamada(gson) {
                        notificacionApi.listar7(buscar = buscar?.ifBlank { null }, pagina = pagina, tamano = tamano)
                    }
                },
                items = { it.contenido.orEmpty() },
                totalPaginas = { it.totalPaginas ?: 1 },
            )
        },
    ).flow

    suspend fun enviar(
        canal: EnviarNotificacionRequest.Canal,
        clienteId: UUID?,
        mensaje: String,
    ): RespuestaRed<NotificacionResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { notificacionApi.enviar(EnviarNotificacionRequest(canal, clienteId, mensaje)) }
    }

    /** Estado de configuración de los canales (push FCM / WhatsApp): para avisar si están apagados. */
    suspend fun estadoCanales(): RespuestaRed<EstadoDeCanales> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { notificacionApi.estadoDeCanales() } }

    suspend fun recordarVencidas(): RespuestaRed<RecordatorioResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { notificacionApi.recordarVencidas() } }

    suspend fun recordarProximas(): RespuestaRed<RecordatorioResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { notificacionApi.recordarProximas() } }

    suspend fun avisarStockBajo(): RespuestaRed<RecordatorioResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { notificacionApi.avisarStockBajo() } }

    /** Clientes (no archivados) para el selector del envío. */
    suspend fun clientes(): RespuestaRed<List<ClienteResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { clienteApi.listar14(buscar = null, conPendientes = false, filtro = null, incluirArchivados = false, pagina = 0, tamano = 100) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty())
        }
    }
}
