package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuditoriaControllerApi
import com.costumi.apiclient.models.AuditoriaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Auditoria del modo GESTION: trail de eventos de la empresa (solo lectura, acotado al tenant). */
@Singleton
class AuditoriaRepository @Inject constructor(
    private val auditoriaApi: AuditoriaControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun registros(buscar: String? = null): RespuestaRed<List<AuditoriaResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { auditoriaApi.listar19(buscar = buscar?.ifBlank { null }, tamano = TAMANO) }
                .mapear { it.contenido.orEmpty() }
        }

    private companion object {
        /** Estas listas se muestran completas en pantalla; se pide una pagina grande y se busca en el servidor. */
        const val TAMANO = 100
    }
}
