package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.data.remote.MisDeudasApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Multas y saldos del propio cliente, en todas las tiendas (RF-7/11.5). */
@Singleton
class MisDeudasRepository @Inject constructor(
    private val api: MisDeudasApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun mias(): RespuestaRed<List<MiDeudaDto>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.mias() } }
}
