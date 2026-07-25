package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.PlantillaNotificacionControllerApi
import com.costumi.apiclient.models.ActualizarPlantillaRequest
import com.costumi.apiclient.models.PlantillaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Plantillas de mensajes automaticos de la empresa (RF-11): listar las 6 y personalizar cada una. */
@Singleton
class PlantillaRepository @Inject constructor(
    private val api: PlantillaNotificacionControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun plantillas(): RespuestaRed<List<PlantillaResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.listar17() } }

    suspend fun actualizar(tipo: String, texto: String, activa: Boolean): RespuestaRed<PlantillaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.actualizar1(tipo, ActualizarPlantillaRequest(texto, activa)) }
        }
}
