package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.FotoDisfrazApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.CategoriaControllerApi
import com.costumi.apiclient.apis.CategoriaDeDisfrazControllerApi
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.DisfrazControllerApi
import com.costumi.apiclient.apis.DisfrazMarketplaceControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.apis.TipoEtiquetaControllerApi
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CategoriaDeDisfrazRequest
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaResponse
import com.costumi.apiclient.models.CrearDisfrazRequest
import com.costumi.apiclient.models.DisfrazDetalleResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.ItemDisfrazDto
import com.costumi.apiclient.models.LineaPrendaRentaDto
import com.costumi.apiclient.models.LineaPrendaVentaDto
import com.costumi.apiclient.models.RentarDisfrazRequest
import com.costumi.apiclient.models.RentarVariosDisfracesRequest
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.VenderVariosDisfracesRequest
import com.costumi.apiclient.models.SlotOpcionesResponse
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.VenderDisfrazRequest
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Un tipo de etiqueta con sus valores (para restringir el pool de un slot personalizable). */
data class TipoConValores(
    val tipo: TipoEtiquetaResponse,
    val valores: List<ValorEtiquetaResponse>,
)

/**
 * Disfraces (RF-14): unidades compuestas por slots (piezas). Aquí se soportan slots FIJA
 * (una prenda concreta); los PERSONALIZABLE (pool por categoría) se arman en el cliente.
 */
@Singleton
class DisfrazRepository @Inject constructor(
    private val disfrazApi: DisfrazControllerApi,
    private val marketplaceDisfrazApi: DisfrazMarketplaceControllerApi,
    private val prendaApi: PrendaControllerApi,
    private val categoriaApi: CategoriaControllerApi,
    private val categoriaDisfrazApi: CategoriaDeDisfrazControllerApi,
    private val fotoApi: FotoDisfrazApi,
    private val authApi: AuthControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val clienteApi: ClienteControllerApi,
    private val tipoEtiquetaApi: TipoEtiquetaControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Tipos de etiqueta activos con sus valores, para restringir el pool de un slot personalizable. */
    suspend fun tiposConValores(): RespuestaRed<List<TipoConValores>> = withContext(dispatchers.io) {
        when (val tipos = ejecutarLlamada(gson) { tipoEtiquetaApi.listar1() }) {
            is RespuestaRed.Fallo -> tipos
            is RespuestaRed.Exito -> {
                val activos = tipos.data.filter { it.archivada != true }
                val out = activos.mapNotNull { tipo ->
                    val id = tipo.id ?: return@mapNotNull null
                    val valores = (ejecutarLlamada(gson) { tipoEtiquetaApi.listarValores(id) } as? RespuestaRed.Exito)
                        ?.data?.filter { it.archivada != true }.orEmpty()
                    TipoConValores(tipo, valores)
                }
                RespuestaRed.Exito(out)
            }
        }
    }
    /** empresaId del staff (de su token vía /auth/me), para las consultas de disfraz "marketplace". */
    private suspend fun empresaId(): RespuestaRed<UUID> =
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> me.data.empresaId?.takeIf { it.isNotBlank() }
                ?.let { RespuestaRed.Exito(UUID.fromString(it)) }
                ?: RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tu usuario no tiene empresa asignada."))
        }

    /** Detalle + disponibilidad del disfraz (para armarlo en modo asistido). */
    suspend fun detalleAsistido(disfrazId: UUID): RespuestaRed<Pair<UUID, DisfrazDetalleResponse>> =
        withContext(dispatchers.io) {
            when (val e = empresaId()) {
                is RespuestaRed.Fallo -> e
                is RespuestaRed.Exito -> when (val d = ejecutarLlamada(gson) { marketplaceDisfrazApi.detalle(e.data, disfrazId) }) {
                    is RespuestaRed.Fallo -> d
                    is RespuestaRed.Exito -> RespuestaRed.Exito(e.data to d.data)
                }
            }
        }

    /** "Ruleta" de un slot (opciones concretas) para el modo asistido. */
    suspend fun opcionesDeSlotAsistido(empresaId: UUID, disfrazId: UUID, orden: Int): RespuestaRed<SlotOpcionesResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { marketplaceDisfrazApi.opcionesDeSlot(empresaId, disfrazId, orden, null) }
        }

    /** Clientes (fichas) para el selector de "a quién" se le vende/renta el disfraz. */
    suspend fun clientesParaSelector(): RespuestaRed<List<ClienteResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) {
            clienteApi.listar14(null, false, null, false, 0, 200)
        }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty())
        }
    }

    /** Sucursales activas de la empresa (para el retiro). */
    suspend fun sucursalesParaSelector(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val e = empresaId()) {
            is RespuestaRed.Fallo -> e
            is RespuestaRed.Exito -> when (val r = ejecutarLlamada(gson) { sucursalApi.listar9(e.data) }) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
            }
        }
    }

    /** Renta asistida del disfraz a un cliente (empresa del token; clienteId obligatorio). */
    suspend fun rentarParaCliente(
        disfrazId: UUID,
        clienteId: UUID,
        sucursalId: UUID,
        retiro: LocalDate,
        devolucion: LocalDate,
        cantidad: Int,
        selecciones: List<SeleccionSlotDto>,
    ): RespuestaRed<UUID> = withContext(dispatchers.io) {
        val req = RentarDisfrazRequest(
            sucursalId = sucursalId, fechaRetiro = retiro, fechaDevolucion = devolucion,
            empresaId = null, clienteId = clienteId, cantidad = cantidad, selecciones = selecciones,
        )
        when (val r = ejecutarLlamada(gson) { disfrazApi.rentar(disfrazId, req) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> r.data.rentaId
                ?.let { RespuestaRed.Exito(it) }
                ?: RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "No se creo la renta."))
        }
    }

    /** Venta asistida del disfraz a un cliente. */
    suspend fun venderParaCliente(
        disfrazId: UUID,
        clienteId: UUID,
        sucursalId: UUID,
        cantidad: Int,
        selecciones: List<SeleccionSlotDto>,
    ): RespuestaRed<UUID> = withContext(dispatchers.io) {
        val req = VenderDisfrazRequest(
            sucursalId = sucursalId, empresaId = null, clienteId = clienteId,
            cantidad = cantidad, selecciones = selecciones,
        )
        when (val r = ejecutarLlamada(gson) { disfrazApi.vender(disfrazId, req) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> r.data.ventaId
                ?.let { RespuestaRed.Exito(it) }
                ?: RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "No se creo la venta."))
        }
    }

    /**
     * Renta un pedido en UNA sola renta al mismo cliente: disfraces ({@code items}) y/o prendas sueltas
     * ({@code lineas}). Es la base de "Nueva renta" cuando mezcla prendas y disfraces.
     */
    suspend fun rentarVarios(
        clienteId: UUID,
        sucursalId: UUID,
        retiro: LocalDate,
        devolucion: LocalDate,
        items: List<ItemDisfrazDto>,
        lineas: List<LineaPrendaRentaDto> = emptyList(),
    ): RespuestaRed<UUID> = withContext(dispatchers.io) {
        val req = RentarVariosDisfracesRequest(
            sucursalId = sucursalId, fechaRetiro = retiro, fechaDevolucion = devolucion,
            empresaId = null, clienteId = clienteId,
            items = items.ifEmpty { null }, lineas = lineas.ifEmpty { null },
        )
        when (val r = ejecutarLlamada(gson) { disfrazApi.rentarVarios(req) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> r.data.rentaId
                ?.let { RespuestaRed.Exito(it) }
                ?: RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "No se creo la renta."))
        }
    }

    /**
     * Vende un pedido en UNA sola venta al mismo cliente: disfraces ({@code items}) y/o prendas sueltas
     * ({@code lineas}). Es la base de "Nueva venta" cuando mezcla prendas y disfraces.
     */
    suspend fun venderVarios(
        clienteId: UUID?,
        sucursalId: UUID,
        items: List<ItemDisfrazDto>,
        lineas: List<LineaPrendaVentaDto> = emptyList(),
    ): RespuestaRed<UUID> = withContext(dispatchers.io) {
        val req = VenderVariosDisfracesRequest(
            sucursalId = sucursalId, empresaId = null, clienteId = clienteId,
            items = items.ifEmpty { null }, lineas = lineas.ifEmpty { null },
        )
        when (val r = ejecutarLlamada(gson) { disfrazApi.venderVarios(req) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> r.data.ventaId
                ?.let { RespuestaRed.Exito(it) }
                ?: RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "No se creo la venta."))
        }
    }
    /** Sube la foto del disfraz (multipart, part `archivo`). Devuelve el disfraz con su `fotoUrl`. */
    suspend fun subirFoto(
        id: UUID,
        bytes: ByteArray,
        mime: String,
        nombreArchivo: String,
    ): RespuestaRed<DisfrazResponse> = withContext(dispatchers.io) {
        val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val parte = MultipartBody.Part.createFormData("archivo", nombreArchivo, cuerpo)
        ejecutarLlamada(gson) { fotoApi.subirFoto(id, parte) }
    }
    suspend fun disfraces(buscar: String? = null): RespuestaRed<List<DisfrazResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                disfrazApi.listar11(buscar = buscar?.ifBlank { null }, tamano = TAMANO_CATALOGO)
            }.mapear { it.contenido.orEmpty() }
        }

    /** Un disfraz por id (para precargar la edición); se busca en el listado. */
    suspend fun disfraz(id: UUID): RespuestaRed<DisfrazResponse> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { disfrazApi.listar11(tamano = TAMANO_CATALOGO) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> {
                val d = r.data.contenido.orEmpty().firstOrNull { it.id == id }
                if (d != null) RespuestaRed.Exito(d)
                else RespuestaRed.Fallo(
                    com.costumi.app.core.ErrorApi(
                        com.costumi.app.core.TipoError.NO_ENCONTRADO, "El disfraz ya no existe.",
                    ),
                )
            }
        }
    }

    /** Categorías de PRENDA activas — solo para filtrar el picker de elementos de un slot personalizable. */
    suspend fun categoriasParaPool(): RespuestaRed<List<CategoriaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { categoriaApi.listar15() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
        }
    }

    // --- Categorías de DISFRAZ (taxonomía propia, SEPARADA de las de prenda) ---

    /** Categorías de disfraz de la empresa (ej. "Piratas"). Para el form y la lista de disfraces. */
    suspend fun categoriasDeDisfraz(): RespuestaRed<List<CategoriaDeDisfrazResponse>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { categoriaDisfrazApi.listar12() }
    }

    suspend fun crearCategoriaDisfraz(nombre: String): RespuestaRed<CategoriaDeDisfrazResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaDisfrazApi.crear6(CategoriaDeDisfrazRequest(nombre)) }
        }

    suspend fun renombrarCategoriaDisfraz(id: UUID, nombre: String): RespuestaRed<CategoriaDeDisfrazResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaDisfrazApi.renombrar1(id, CategoriaDeDisfrazRequest(nombre)) }
        }

    suspend fun archivarCategoriaDisfraz(id: UUID): RespuestaRed<CategoriaDeDisfrazResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaDisfrazApi.archivar3(id) }
        }

    suspend fun activarCategoriaDisfraz(id: UUID): RespuestaRed<CategoriaDeDisfrazResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaDisfrazApi.activar4(id) }
        }

    suspend fun crear(req: CrearDisfrazRequest): RespuestaRed<DisfrazResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { disfrazApi.crear5(req) } }

    suspend fun editar(id: UUID, req: CrearDisfrazRequest): RespuestaRed<DisfrazResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { disfrazApi.editar1(id, req) } }

    suspend fun archivar(id: UUID): RespuestaRed<DisfrazResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { disfrazApi.archivar2(id) } }

    suspend fun activar(id: UUID): RespuestaRed<DisfrazResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { disfrazApi.activar3(id) } }

    /** true/false si el disfraz completo se puede armar con el stock disponible. */
    suspend fun disponibilidad(id: UUID): RespuestaRed<Boolean> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { disfrazApi.disponibilidad(id, null) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.disponible == true)
        }
    }

    /** Prendas para el selector de un slot FIJA (no paginado: primera página amplia). */
    suspend fun prendasParaSelector(): RespuestaRed<List<PrendaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { prendaApi.listar4(pagina = 0, tamano = 100) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.contenido.orEmpty().filter { it.archivada != true })
        }
    }

    /**
     * Catálogo del inventario para elegir las opciones (elementos) de una parte personalizable: prendas
     * de la empresa, opcionalmente de una categoría y filtradas por valores de etiqueta ("tipoId:valorId"),
     * cada una con su stock disponible y sus etiquetas.
     */
    suspend fun catalogoInventario(
        categoriaId: UUID? = null,
        etiquetas: List<String>? = null,
    ): RespuestaRed<List<PrendaDeCatalogoResponse>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { prendaApi.catalogo(categoriaId, etiquetas) }
    }

    private companion object {
        /** El catalogo de disfraces se muestra completo en la pantalla; la busqueda va al servidor. */
        const val TAMANO_CATALOGO = 100
    }
}
