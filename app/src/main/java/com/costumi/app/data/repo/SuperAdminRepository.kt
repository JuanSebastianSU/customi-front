package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.models.EmpresaPendienteResponse
import com.costumi.apiclient.models.EmpresaResponse
import com.costumi.apiclient.models.EmpresaResumenResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Panel SUPERADMIN (RF-15.3): cola de solicitudes de tienda (aprobar/rechazar) y listado de empresas
 * ACTIVAS/SUSPENDIDAS para suspenderlas/reactivarlas.
 */
@Singleton
class SuperAdminRepository @Inject constructor(
    private val empresaApi: EmpresaControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun pendientes(): RespuestaRed<List<EmpresaPendienteResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.pendientes() } }

    /** Empresas gestionables (ACTIVAS y SUSPENDIDAS) para suspender/reactivar. */
    suspend fun empresas(): RespuestaRed<List<EmpresaResumenResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.listar8() } }

    suspend fun aprobar(id: UUID): RespuestaRed<EmpresaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.aprobar1(id) } }

    suspend fun rechazar(id: UUID): RespuestaRed<EmpresaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.rechazar1(id) } }

    suspend fun suspender(id: UUID): RespuestaRed<EmpresaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.suspender(id) } }

    suspend fun reactivar(id: UUID): RespuestaRed<EmpresaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empresaApi.reactivar(id) } }
}
