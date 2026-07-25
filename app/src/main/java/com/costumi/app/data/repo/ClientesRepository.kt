package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.models.CambiarListaNegraRequest
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.CrearClienteRequest
import com.costumi.apiclient.models.EditarClienteRequest
import com.costumi.apiclient.models.EstadoDeCuentaResponse
import com.costumi.apiclient.models.HistorialItem
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Clientes del modo GESTION: lista paginada (búsqueda/archivados), ficha, lista negra e historial. */
@Singleton
class ClientesRepository @Inject constructor(
    private val api: ClienteControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    fun clientes(
        buscar: String?,
        incluirArchivados: Boolean,
        filtro: String? = null,
    ): Flow<PagingData<ClienteResponse>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { ClientesPagingSource(api, gson, buscar, incluirArchivados, filtro) },
        ).flow

    suspend fun crear(req: CrearClienteRequest): RespuestaRed<ClienteResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.crear6(req) } }

    suspend fun editar(id: UUID, req: EditarClienteRequest): RespuestaRed<ClienteResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.editar2(id, req) } }

    suspend fun archivar(id: UUID): RespuestaRed<ClienteResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.archivar4(id) } }

    suspend fun activar(id: UUID): RespuestaRed<ClienteResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.activar5(id) } }

    suspend fun cambiarListaNegra(id: UUID, enListaNegra: Boolean): RespuestaRed<ClienteResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.cambiarListaNegra(id, CambiarListaNegraRequest(enListaNegra)) }
        }

    suspend fun historial(id: UUID): RespuestaRed<List<HistorialItem>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.historial(id) } }

    /** Estado de cuenta: desglose por renta de cuánto debe el cliente y por qué (RF-7/11.5). */
    suspend fun estadoCuenta(id: UUID): RespuestaRed<EstadoDeCuentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.estadoCuenta(id) } }
}
