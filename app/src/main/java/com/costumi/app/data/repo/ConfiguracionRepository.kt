package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ConfiguracionControllerApi
import com.costumi.apiclient.models.ConfiguracionRequest
import com.costumi.apiclient.models.ConfiguracionResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Configuración de la empresa (RF-12): switches, impuesto, moneda, recargo por retraso y reembolsos. */
@Singleton
class ConfiguracionRepository @Inject constructor(
    private val api: ConfiguracionControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun obtener(): RespuestaRed<ConfiguracionResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.obtener() } }

    /** El PUT es reemplazo total: hay que enviar TODOS los campos (los 4 switches no aceptan null). */
    suspend fun actualizar(req: ConfiguracionRequest): RespuestaRed<ConfiguracionResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.actualizar2(req) } }

    /** Exporta la configuración (RF-12.3) para respaldo. */
    suspend fun exportar(): RespuestaRed<ConfiguracionResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.exportar() } }

    /** Importa (restaura) la configuración desde un respaldo (RF-12.3). */
    suspend fun importar(req: ConfiguracionRequest): RespuestaRed<ConfiguracionResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.importar(req) } }
}
