package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.CajaControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.AbrirTurnoRequest
import com.costumi.apiclient.models.CerrarTurnoRequest
import com.costumi.apiclient.models.RegistrarMovimientoRequest
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TurnoResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Caja del modo GESTION: turnos (abrir/cerrar), movimientos y arqueo por método (RF-6.3/6.10). */
@Singleton
class CajaRepository @Inject constructor(
    private val cajaApi: CajaControllerApi,
    private val authApi: AuthControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun turnos(): RespuestaRed<List<TurnoResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { cajaApi.listar16() } }

    suspend fun abrir(sucursalId: UUID, fondoInicial: BigDecimal): RespuestaRed<TurnoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { cajaApi.abrir(AbrirTurnoRequest(sucursalId, fondoInicial)) } }

    suspend fun cerrar(turnoId: UUID, efectivoContado: BigDecimal): RespuestaRed<TurnoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { cajaApi.cerrar1(turnoId, CerrarTurnoRequest(efectivoContado)) } }

    suspend fun registrarMovimiento(turnoId: UUID, req: RegistrarMovimientoRequest): RespuestaRed<TurnoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { cajaApi.mover1(turnoId, req) } }

    /** Sucursales activas de la empresa, para elegir dónde abrir el turno. */
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
}
