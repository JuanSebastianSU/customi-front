package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.core.TipoError
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.DisfrazControllerApi
import com.costumi.apiclient.apis.DisfrazMarketplaceControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.models.DisfrazDetalleResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import com.costumi.apiclient.models.RentarDisfrazRequest
import com.costumi.apiclient.models.RentarDisfrazResponse
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SlotOpcionesResponse
import com.costumi.apiclient.models.VenderDisfrazRequest
import com.costumi.apiclient.models.VenderDisfrazResponse
import java.time.LocalDate
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Marketplace del cliente (RF-14/18). Las tiendas se cachean en Room (fuente de verdad: la UI
 * observa [observarEmpresas] y el repo sincroniza de la red). El catalogo de una tienda se sirve
 * directo de la red (se cacheara en Room mas adelante, Fase 8).
 */
@Singleton
class MarketplaceRepository @Inject constructor(
    private val api: MarketplaceControllerApi,
    private val disfrazApi: DisfrazMarketplaceControllerApi,
    private val disfrazAccionApi: DisfrazControllerApi,
    private val empresaDao: EmpresaDao,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Flujo de tiendas cacheadas: la pantalla lo observa y se refresca solo al sincronizar. */
    fun observarEmpresas(): Flow<List<EmpresaEntity>> = empresaDao.observarTodas()

    /** Sincroniza la lista de tiendas desde la red y actualiza la caché. */
    suspend fun refrescarEmpresas(buscar: String?): RespuestaRed<Unit> = withContext(dispatchers.io) {
        val filtro = buscar?.trim()?.takeIf { it.isNotBlank() }
        when (val r = ejecutarLlamada(gson) { api.empresas(filtro) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                empresaDao.reemplazar(r.data.mapNotNull { it.aEntity() })
                RespuestaRed.Exito(Unit)
            }
        }
    }

    /**
     * Busca tiendas en el servidor por nombre. **No toca la cache** a proposito: `refrescarEmpresas`
     * la reemplaza, asi que buscar por esa via dejaria al usuario con solo las coincidencias guardadas
     * y sin nada que mostrar sin conexion.
     */
    suspend fun buscarEmpresas(texto: String): RespuestaRed<List<EmpresaEntity>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.empresas(texto.trim()) }
                .mapear { lista -> lista.mapNotNull { it.aEntity() } }
        }

    /** Catalogo (prendas en vitrina) de una tienda; opcionalmente filtrado por categoria. */
    suspend fun catalogo(empresaId: String, categoriaId: UUID? = null): RespuestaRed<List<PrendaVitrinaResponse>> =
        withContext(dispatchers.io) {
            val uuid = runCatching { UUID.fromString(empresaId) }.getOrNull()
                ?: return@withContext RespuestaRed.Fallo(
                    ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."),
                )
            ejecutarLlamada(gson) { api.catalogo1(uuid, categoriaId) }
        }

    /** Sucursales (puntos de retiro) publicas de una tienda. */
    suspend fun sucursales(empresaId: UUID): RespuestaRed<List<com.costumi.apiclient.models.SucursalVitrinaResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.sucursales(empresaId) }
        }

    /** Disfraces en vitrina de una tienda (apartado Disfraces). */
    suspend fun disfraces(empresaId: String): RespuestaRed<List<DisfrazResponse>> =
        withContext(dispatchers.io) {
            val uuid = runCatching { UUID.fromString(empresaId) }.getOrNull()
                ?: return@withContext RespuestaRed.Fallo(
                    ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."),
                )
            ejecutarLlamada(gson) { disfrazApi.listar18(uuid) }
        }

    /** Detalle de un disfraz (estructura de slots + disponibilidad) para armarlo. */
    suspend fun disfrazDetalle(empresaId: UUID, disfrazId: UUID): RespuestaRed<DisfrazDetalleResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { disfrazApi.detalle(empresaId, disfrazId) }
        }

    /** Opciones concretas ("ruleta") de un slot personalizable, filtrables por valores de etiqueta. */
    suspend fun opcionesDeSlot(
        empresaId: UUID,
        disfrazId: UUID,
        orden: Int,
        valores: List<UUID>? = null,
    ): RespuestaRed<SlotOpcionesResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { disfrazApi.opcionesDeSlot(empresaId, disfrazId, orden, valores) }
        }

    /** El cliente renta el disfraz a su nombre (empresa = la tienda; su ficha sale del token). */
    suspend fun rentarDisfraz(
        empresaId: UUID,
        disfrazId: UUID,
        sucursalId: UUID,
        retiro: LocalDate,
        devolucion: LocalDate,
        selecciones: List<SeleccionSlotDto>,
    ): RespuestaRed<RentarDisfrazResponse> = withContext(dispatchers.io) {
        val req = RentarDisfrazRequest(
            sucursalId = sucursalId,
            fechaRetiro = retiro,
            fechaDevolucion = devolucion,
            empresaId = empresaId,
            clienteId = null,
            selecciones = selecciones,
        )
        ejecutarLlamada(gson) { disfrazAccionApi.rentar(disfrazId, req) }
    }

    /** El cliente compra el disfraz a su nombre (empresa = la tienda; su ficha sale del token). */
    suspend fun venderDisfraz(
        empresaId: UUID,
        disfrazId: UUID,
        sucursalId: UUID,
        selecciones: List<SeleccionSlotDto>,
    ): RespuestaRed<VenderDisfrazResponse> = withContext(dispatchers.io) {
        val req = VenderDisfrazRequest(
            sucursalId = sucursalId,
            empresaId = empresaId,
            clienteId = null,
            selecciones = selecciones,
        )
        ejecutarLlamada(gson) { disfrazAccionApi.vender(disfrazId, req) }
    }
}
