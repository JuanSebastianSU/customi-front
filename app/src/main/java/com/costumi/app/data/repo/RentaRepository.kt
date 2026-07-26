package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ContratoRentaApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.RentaControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.CrearRentaRequest
import com.costumi.apiclient.models.ExtenderRentaRequest
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.RentaResponse
import com.costumi.apiclient.models.SucursalResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Rentas del modo GESTION: listado paginado, datos para armar la renta, alta y ciclo de estados. */
@Singleton
class RentaRepository @Inject constructor(
    private val rentaApi: RentaControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val authApi: AuthControllerApi,
    private val clienteApi: ClienteControllerApi,
    private val prendaApi: PrendaControllerApi,
    private val contratoApi: ContratoRentaApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** [buscar] filtra por codigo de retiro; [filtro] es la bandeja/estado (chip), null = todas. */
    fun rentas(buscar: String? = null, filtro: String? = null): Flow<PagingData<RentaResponse>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { RentasPagingSource(rentaApi, gson, buscar, filtro) },
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

    suspend fun crear(req: CrearRentaRequest): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { rentaApi.crear1(req) } }

    suspend fun entregar(id: UUID): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { rentaApi.entregar(id) } }

    suspend fun devolver(id: UUID): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { rentaApi.devolver1(id) } }

    suspend fun cerrar(id: UUID): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { rentaApi.cerrar(id) } }

    suspend fun cancelar(id: UUID): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { rentaApi.cancelar(id) } }

    suspend fun extender(id: UUID, nuevaFecha: LocalDate): RespuestaRed<RentaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { rentaApi.extender(id, ExtenderRentaRequest(nuevaFecha)) }
        }

    /** Descarga el contrato (PDF) de la renta como bytes. */
    suspend fun contrato(id: UUID): RespuestaRed<ByteArray> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { contratoApi.contrato(id) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.bytes())
        }
    }
}
