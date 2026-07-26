package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.apis.VentaControllerApi
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DevolverVentaRequest
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.RegistrarVentaRequest
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.VentaResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Ventas (POS) del modo GESTION: listado paginado, datos para armar la venta, registrar y devolver. */
@Singleton
class VentaRepository @Inject constructor(
    private val ventaApi: VentaControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val authApi: AuthControllerApi,
    private val clienteApi: ClienteControllerApi,
    private val prendaApi: PrendaControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** [buscar] filtra por codigo de retiro (el que el cliente muestra en la tienda); [estado] filtra por chip. */
    fun ventas(
        buscar: String? = null,
        estado: VentaControllerApi.EstadoListar? = null,
        desde: java.time.LocalDate? = null,
        hasta: java.time.LocalDate? = null,
    ): Flow<PagingData<VentaResponse>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { VentasPagingSource(ventaApi, gson, buscar, estado, desde, hasta) },
    ).flow

    suspend fun sucursales(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> {
                val empresaId = me.data.empresaId
                if (empresaId.isNullOrBlank()) {
                    RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tu usuario no tiene empresa asignada."))
                } else {
                    when (val r = ejecutarLlamada(gson) { sucursalApi.listar9(UUID.fromString(empresaId)) }) {
                        is RespuestaRed.Fallo -> r
                        is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
                    }
                }
            }
        }
    }

    suspend fun clientesParaSelector(): RespuestaRed<List<ClienteResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { clienteApi.listar14(buscar = null, conPendientes = false, filtro = null, incluirArchivados = false, pagina = 0, tamano = 100) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty())
        }
    }

    suspend fun prendasParaSelector(): RespuestaRed<List<PrendaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { prendaApi.listar4(pagina = 0, tamano = 100) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty().filter { it.archivada != true })
        }
    }

    suspend fun registrar(req: RegistrarVentaRequest): RespuestaRed<VentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { ventaApi.registrar(req) } }

    suspend fun devolver(id: UUID, req: DevolverVentaRequest): RespuestaRed<VentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { ventaApi.devolver(id, req) } }
}
