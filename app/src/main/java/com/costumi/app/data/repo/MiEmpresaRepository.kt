package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.MiEmpresaApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.models.EmpresaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La propia tienda del usuario (RF-15.1). Se cachea en memoria: el nombre encabeza Gestion y no cambia
 * dentro de una sesion, asi que no tiene sentido volver a pedirlo cada vez que se abre el panel.
 */
@Singleton
class MiEmpresaRepository @Inject constructor(
    private val api: MiEmpresaApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    private var cache: EmpresaResponse? = null

    suspend fun mia(): RespuestaRed<EmpresaResponse> {
        cache?.let { return RespuestaRed.Exito(it) }
        return withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.mia() }.also { r ->
                if (r is RespuestaRed.Exito) cache = r.data
            }
        }
    }

    /** Al cerrar sesion la tienda deja de ser la misma: si no, el proximo dueno veria el nombre anterior. */
    fun limpiar() {
        cache = null
    }
}
