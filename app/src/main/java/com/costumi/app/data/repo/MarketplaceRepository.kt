package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.core.TipoError
import com.costumi.app.data.local.dao.DisfrazVitrinaDao
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.dao.PrendaVitrinaDao
import com.costumi.app.data.local.entity.DisfrazVitrinaEntity
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.local.entity.PrendaVitrinaEntity
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.DisfrazControllerApi
import com.costumi.apiclient.apis.DisfrazMarketplaceControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.models.DisfrazDetalleResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.EtiquetaVitrinaDto
import com.costumi.apiclient.models.PrendaVitrinaResponse
import com.costumi.apiclient.models.SlotDto
import java.math.BigDecimal
import com.costumi.apiclient.models.RentarDisfrazRequest
import com.costumi.apiclient.models.RentarDisfrazResponse
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SlotOpcionesResponse
import com.costumi.apiclient.models.VenderDisfrazRequest
import com.costumi.apiclient.models.VenderDisfrazResponse
import java.time.LocalDate
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val prendaVitrinaDao: PrendaVitrinaDao,
    private val disfrazVitrinaDao: DisfrazVitrinaDao,
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
                empresaDao.reemplazar(r.data.contenido.orEmpty().mapNotNull { it.aEntity() })
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
                .mapear { pag -> pag.contenido.orEmpty().mapNotNull { it.aEntity() } }
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

    /**
     * Catálogo cacheado de una tienda (`PLAN_ROOM_OFFLINE.md` A1). La UI observa esto (Room como fuente de
     * verdad) y el repo lo sincroniza con [refrescarCatalogo]. Se filtra por `empresaId` para no mezclar
     * tiendas. El filtro por categoría/etiqueta se sigue haciendo en la app sobre esta lista, así que aquí
     * NO se filtra por la red (guardar un subconjunto filtrado dejaría la caché incompleta, §9.1).
     */
    fun observarCatalogo(empresaId: String): Flow<List<PrendaVitrinaResponse>> =
        prendaVitrinaDao.observarDeEmpresa(empresaId).map { lista -> lista.map { it.aResponse() } }

    /** Trae el catálogo completo de la tienda desde la red y **escribe en Room** (reemplaza solo esa tienda). */
    suspend fun refrescarCatalogo(empresaId: String): RespuestaRed<Unit> = withContext(dispatchers.io) {
        val uuid = runCatching { UUID.fromString(empresaId) }.getOrNull()
            ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."))
        when (val r = ejecutarLlamada(gson) { api.catalogo1(uuid, null) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                prendaVitrinaDao.reemplazarDeEmpresa(empresaId, r.data.mapNotNull { it.aEntity(empresaId) })
                RespuestaRed.Exito(Unit)
            }
        }
    }

    /** Borra la caché del catálogo (se llama al cerrar sesión, norma N1). */
    suspend fun limpiarCacheCatalogo() = withContext(dispatchers.io) { prendaVitrinaDao.limpiar() }

    /** Sucursales (puntos de retiro) publicas de una tienda. */
    suspend fun sucursales(empresaId: UUID): RespuestaRed<List<com.costumi.apiclient.models.SucursalVitrinaResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { api.sucursales(empresaId) }
        }

    /** Disfraces destacados del marketplace (carrusel del home). */
    suspend fun destacados(): RespuestaRed<List<com.costumi.apiclient.models.DisfrazDestacadoResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.destacados() } }

    /** Detalle de la tienda (nombre/descripción/ciudad/portada) para la cabecera del catálogo. */
    suspend fun detalleEmpresa(empresaId: String): RespuestaRed<com.costumi.apiclient.models.EmpresaVitrinaResponse> =
        withContext(dispatchers.io) {
            val uuid = runCatching { UUID.fromString(empresaId) }.getOrNull()
                ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."))
            ejecutarLlamada(gson) { api.empresa(uuid) }
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

    /**
     * Disfraces cacheados de una tienda (`PLAN_ROOM_OFFLINE.md` A2). La UI observa esto (Room como fuente de
     * verdad) y el repo lo sincroniza con [refrescarDisfraces]. Se filtra por `empresaId` para no mezclar
     * tiendas. Solo se cachea lo que la vitrina pinta; el detalle (slots/disponibilidad) se pide a la red.
     */
    fun observarDisfraces(empresaId: String): Flow<List<DisfrazResponse>> =
        disfrazVitrinaDao.observarDeEmpresa(empresaId).map { lista -> lista.map { it.aResponse() } }

    /** Trae los disfraces de la tienda desde la red y **escribe en Room** (reemplaza solo esa tienda). */
    suspend fun refrescarDisfraces(empresaId: String): RespuestaRed<Unit> = withContext(dispatchers.io) {
        val uuid = runCatching { UUID.fromString(empresaId) }.getOrNull()
            ?: return@withContext RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tienda no valida."))
        when (val r = ejecutarLlamada(gson) { disfrazApi.listar18(uuid) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                disfrazVitrinaDao.reemplazarDeEmpresa(empresaId, r.data.mapNotNull { it.aEntity(empresaId) })
                RespuestaRed.Exito(Unit)
            }
        }
    }

    /** Borra la caché de disfraces (se llama al cerrar sesión, norma N1). */
    suspend fun limpiarCacheDisfraces() = withContext(dispatchers.io) { disfrazVitrinaDao.limpiar() }

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

    // --- Mapeadores DTO <-> Entity del catálogo (UUID/precio como texto, etiquetas como JSON) ---

    /** null si la prenda no trae id (sin clave primaria no se puede cachear). [empresaId] viene del contexto. */
    private fun PrendaVitrinaResponse.aEntity(empresaId: String): PrendaVitrinaEntity? {
        val idTexto = id?.toString() ?: return null
        return PrendaVitrinaEntity(
            id = idTexto,
            empresaId = empresaId,
            nombre = nombre,
            tipoArticulo = tipoArticulo,
            precioRenta = precioRenta?.toPlainString(),
            precioVenta = precioVenta?.toPlainString(),
            categoria = categoria,
            fotoUrl = fotoUrl,
            etiquetasJson = etiquetas?.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
        )
    }

    private fun PrendaVitrinaEntity.aResponse(): PrendaVitrinaResponse = PrendaVitrinaResponse(
        id = runCatching { UUID.fromString(id) }.getOrNull(),
        nombre = nombre,
        tipoArticulo = tipoArticulo,
        precioRenta = precioRenta?.let { runCatching { BigDecimal(it) }.getOrNull() },
        precioVenta = precioVenta?.let { runCatching { BigDecimal(it) }.getOrNull() },
        categoria = categoria,
        fotoUrl = fotoUrl,
        etiquetas = etiquetasJson?.let {
            runCatching { gson.fromJson(it, Array<EtiquetaVitrinaDto>::class.java).toList() }.getOrNull()
        },
    )

    // --- Mapeadores DTO <-> Entity de disfraces (mismos criterios; los slots NO se cachean, solo el conteo) ---

    private fun DisfrazResponse.aEntity(empresaId: String): DisfrazVitrinaEntity? {
        val idTexto = id?.toString() ?: return null
        return DisfrazVitrinaEntity(
            id = idTexto,
            empresaId = empresaId,
            nombre = nombre,
            categoria = categoria,
            tipo = tipo?.value,
            precioRentaGeneral = precioRentaGeneral?.toPlainString(),
            precioRentaSugerido = precioRentaSugerido?.toPlainString(),
            precioVentaGeneral = precioVentaGeneral?.toPlainString(),
            precioVentaSugerido = precioVentaSugerido?.toPlainString(),
            fotoUrl = fotoUrl,
            piezas = slots?.size,
        )
    }

    private fun DisfrazVitrinaEntity.aResponse(): DisfrazResponse = DisfrazResponse(
        id = runCatching { UUID.fromString(id) }.getOrNull(),
        empresaId = runCatching { UUID.fromString(empresaId) }.getOrNull(),
        nombre = nombre,
        categoria = categoria,
        tipo = tipo?.let { runCatching { DisfrazResponse.Tipo.valueOf(it) }.getOrNull() },
        precioRentaGeneral = precioRentaGeneral?.let { runCatching { BigDecimal(it) }.getOrNull() },
        precioRentaSugerido = precioRentaSugerido?.let { runCatching { BigDecimal(it) }.getOrNull() },
        precioVentaGeneral = precioVentaGeneral?.let { runCatching { BigDecimal(it) }.getOrNull() },
        precioVentaSugerido = precioVentaSugerido?.let { runCatching { BigDecimal(it) }.getOrNull() },
        fotoUrl = fotoUrl,
        // La vitrina solo usa `slots.size` (el "N piezas"); reconstruimos una lista del conteo cacheado.
        // El contenido real de los slots se pide a la red al abrir el detalle, así que basta con el tamaño.
        slots = piezas?.let { n -> List(n) { SlotDto(ejePrenda = SlotDto.EjePrenda.FIJA) } },
    )
}
