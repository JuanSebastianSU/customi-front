package com.costumi.app.data.repo

import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.DevolucionControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.RentaControllerApi
import com.costumi.apiclient.models.DevolucionResponse
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.RegistrarDevolucionRequest
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import com.costumi.app.core.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Devoluciones de renta (RF-5): checklist de piezas + liquidacion del deposito e historial. */
@Singleton
class DevolucionRepository @Inject constructor(
    private val devolucionApi: DevolucionControllerApi,
    private val prendaApi: PrendaControllerApi,
    private val rentaApi: RentaControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Mapa rentaId → (código de retiro, nombre del cliente): la devolución solo trae el rentaId. */
    suspend fun infoRentas(): Map<String, Pair<String?, String?>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { rentaApi.listar2(pagina = 0, tamano = 500) }) {
            is RespuestaRed.Exito -> r.data.contenido.orEmpty().mapNotNull { renta ->
                renta.id?.toString()?.let { it to (renta.codigoRetiro to renta.clienteNombre) }
            }.toMap()
            is RespuestaRed.Fallo -> emptyMap()
        }
    }

    /** Historial de devoluciones ya liquidadas de la empresa. */
    suspend fun historial(buscar: String? = null): RespuestaRed<List<DevolucionResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { devolucionApi.listar13(buscar = buscar?.ifBlank { null }, tamano = TAMANO) }
                .mapear { it.contenido.orEmpty() }
        }

    /** Registra la devolucion (checklist + cargos). El backend cierra la renta si se resolvieron todas las unidades. */
    suspend fun registrar(req: RegistrarDevolucionRequest): RespuestaRed<DevolucionResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { devolucionApi.registrar4(req) } }

    /** Prendas activas, para resolver nombres en el checklist. */
    suspend fun prendas(): RespuestaRed<List<PrendaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { prendaApi.listar4(pagina = 0, tamano = 200) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty())
        }
    }

    private companion object {
        /** Estas listas se muestran completas en pantalla; se pide una pagina grande y se busca en el servidor. */
        const val TAMANO = 100
    }
}
