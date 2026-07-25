package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.GrupoDeStockControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.AjusteDeStockRequest
import com.costumi.apiclient.models.CrearGrupoDeStockRequest
import com.costumi.apiclient.models.EntradaDeStockRequest
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.MoverUnidadesRequest
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TransferirStockRequest
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grupos de stock de una prenda (RF-13/14): variantes por sucursal con conteo por estado, y las
 * operaciones de inventario (entrada, ajuste, mover, transferir, borrar). Las sucursales del tenant
 * se resuelven vía `/auth/me` (empresaId) + `/empresas/{id}/sucursales`.
 */
@Singleton
class GruposStockRepository @Inject constructor(
    private val grupoApi: GrupoDeStockControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val authApi: AuthControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun grupos(prendaId: UUID): RespuestaRed<List<GrupoDeStockResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.listar5(prendaId) } }

    /** Sucursales activas del tenant (para el nombre, alta de grupo y transferencia). */
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
                        is RespuestaRed.Exito ->
                            RespuestaRed.Exito(r.data.filter { it.archivada != true })
                    }
                }
            }
        }
    }

    suspend fun crearGrupo(prendaId: UUID, req: CrearGrupoDeStockRequest): RespuestaRed<GrupoDeStockResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.crear3(prendaId, req) } }

    suspend fun entrada(grupoId: UUID, cantidad: Int): RespuestaRed<GrupoDeStockResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { grupoApi.reabastecer(grupoId, EntradaDeStockRequest(cantidad)) }
        }

    suspend fun ajuste(grupoId: UUID, req: AjusteDeStockRequest): RespuestaRed<GrupoDeStockResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.ajustar(grupoId, req) } }

    suspend fun mover(grupoId: UUID, req: MoverUnidadesRequest): RespuestaRed<GrupoDeStockResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.mover(grupoId, req) } }

    suspend fun transferir(grupoId: UUID, req: TransferirStockRequest): RespuestaRed<GrupoDeStockResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.transferir(grupoId, req) } }

    suspend fun eliminar(grupoId: UUID): RespuestaRed<Unit> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { grupoApi.eliminar(grupoId) } }
}
