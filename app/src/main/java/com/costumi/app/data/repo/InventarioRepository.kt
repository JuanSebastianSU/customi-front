package com.costumi.app.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.FotoPrendaApi
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.CategoriaControllerApi
import com.costumi.apiclient.apis.GrupoDeStockControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.TipoEtiquetaControllerApi
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CrearPrendaRequest
import com.costumi.apiclient.models.EditarPrendaRequest
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.PrendaResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inventario del modo GESTION: prendas (lista paginada C3 + alta/edición/archivar), categorías
 * (para el selector) y aviso de stock bajo. Todo pasa por [ejecutarLlamada] (errores RFC 7807).
 */
@Singleton
class InventarioRepository @Inject constructor(
    private val prendaApi: PrendaControllerApi,
    private val categoriaApi: CategoriaControllerApi,
    private val grupoStockApi: GrupoDeStockControllerApi,
    private val tipoEtiquetaApi: TipoEtiquetaControllerApi,
    private val fotoApi: FotoPrendaApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Flujo paginado de prendas para la lista de inventario (scroll infinito). [buscar] filtra por nombre. */
    fun prendas(buscar: String? = null): Flow<PagingData<PrendaResponse>> = Pager(
        config = PagingConfig(pageSize = TAMANO_PAGINA, enablePlaceholders = false),
        pagingSourceFactory = { PrendasPagingSource(prendaApi, gson, buscar) },
    ).flow

    suspend fun stockBajo(umbral: Int = 1): RespuestaRed<List<GrupoDeStockResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { grupoStockApi.stockBajo(umbral) }
        }

    suspend fun categorias(): RespuestaRed<List<CategoriaResponse>> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { categoriaApi.listar15() }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
        }
    }

    /**
     * Catálogo del inventario por categoría (con stock): prendas de la empresa, opcionalmente de una
     * categoría y filtradas por valores de etiqueta (`"tipoId:valorId"`; AND entre dimensiones, OR dentro),
     * cada una con su stock disponible y sus etiquetas. Es la vista de "explorar por categoría".
     */
    suspend fun catalogo(
        categoriaId: UUID? = null,
        etiquetas: List<String>? = null,
    ): RespuestaRed<List<PrendaDeCatalogoResponse>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { prendaApi.catalogo(categoriaId, etiquetas?.ifEmpty { null }) }
    }

    /** Tipos de etiqueta activos con sus valores, para los filtros de color/talla del catálogo. */
    suspend fun tiposConValores(): RespuestaRed<List<TipoConValores>> = withContext(dispatchers.io) {
        when (val tipos = ejecutarLlamada(gson) { tipoEtiquetaApi.listar1() }) {
            is RespuestaRed.Fallo -> tipos
            is RespuestaRed.Exito -> {
                val out = tipos.data.filter { it.archivada != true }.mapNotNull { tipo ->
                    val id = tipo.id ?: return@mapNotNull null
                    val valores = (ejecutarLlamada(gson) { tipoEtiquetaApi.listarValores(id) } as? RespuestaRed.Exito)
                        ?.data?.filter { it.archivada != true }.orEmpty()
                    TipoConValores(tipo, valores)
                }
                RespuestaRed.Exito(out)
            }
        }
    }

    suspend fun crearPrenda(req: CrearPrendaRequest): RespuestaRed<PrendaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { prendaApi.crear2(req) } }

    suspend fun editarPrenda(id: UUID, req: EditarPrendaRequest): RespuestaRed<PrendaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { prendaApi.editar(id, req) } }

    suspend fun archivar(id: UUID): RespuestaRed<PrendaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { prendaApi.archivar(id) } }

    suspend fun activar(id: UUID): RespuestaRed<PrendaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { prendaApi.activar(id) } }

    /**
     * Sube la foto (multipart, part `archivo`). Gateado: el backend da 503 si el almacenamiento
     * no está configurado y 415 si el archivo no es una imagen; ambos llegan como [RespuestaRed.Fallo].
     */
    suspend fun subirFoto(
        id: UUID,
        bytes: ByteArray,
        mime: String,
        nombreArchivo: String,
    ): RespuestaRed<PrendaResponse> = withContext(dispatchers.io) {
        val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val parte = MultipartBody.Part.createFormData("archivo", nombreArchivo, cuerpo)
        ejecutarLlamada(gson) { fotoApi.subirFoto(id, parte) }
    }

    companion object {
        const val TAMANO_PAGINA = 20
    }
}
