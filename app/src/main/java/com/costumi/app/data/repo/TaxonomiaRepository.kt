package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.CategoriaControllerApi
import com.costumi.apiclient.apis.ConteoDeDependenciasControllerApi
import com.costumi.apiclient.apis.TipoEtiquetaControllerApi
import com.costumi.apiclient.models.AgregarValorRequest
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CrearCategoriaRequest
import com.costumi.apiclient.models.CrearTipoEtiquetaRequest
import com.costumi.apiclient.models.RenombrarRequest
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Taxonomía del inventario (RF-13.6, "avanzado"): categorías (y luego tipos de etiqueta) con
 * alta/renombrar/archivar y el conteo de prendas que cuelgan (para confirmar antes de archivar).
 */
@Singleton
class TaxonomiaRepository @Inject constructor(
    private val categoriaApi: CategoriaControllerApi,
    private val tipoApi: TipoEtiquetaControllerApi,
    private val conteoApi: ConteoDeDependenciasControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun categorias(): RespuestaRed<List<CategoriaResponse>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { categoriaApi.listar15() }
    }

    suspend fun crearCategoria(nombre: String): RespuestaRed<CategoriaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaApi.crear8(CrearCategoriaRequest(nombre)) }
        }

    suspend fun renombrarCategoria(id: UUID, nombre: String): RespuestaRed<CategoriaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { categoriaApi.renombrar2(id, RenombrarRequest(nombre)) }
        }

    suspend fun archivarCategoria(id: UUID): RespuestaRed<CategoriaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { categoriaApi.archivar5(id) } }

    suspend fun activarCategoria(id: UUID): RespuestaRed<CategoriaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { categoriaApi.activar6(id) } }

    /** Cuántas prendas activas cuelgan de la categoría (para confirmar antes de archivar). */
    suspend fun conteoPrendas(id: UUID): RespuestaRed<Int> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { conteoApi.deCategoria(id) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.prendasActivas ?: 0)
        }
    }

    // --- Tipos de etiqueta ---

    suspend fun tiposEtiqueta(): RespuestaRed<List<TipoEtiquetaResponse>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { tipoApi.listar1() }
    }

    suspend fun crearTipo(
        nombre: String,
        defineVariante: Boolean,
        seleccionablePorCliente: Boolean,
    ): RespuestaRed<TipoEtiquetaResponse> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) {
            tipoApi.crear(CrearTipoEtiquetaRequest(nombre, defineVariante, seleccionablePorCliente, null))
        }
    }

    suspend fun renombrarTipo(id: UUID, nombre: String): RespuestaRed<TipoEtiquetaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { tipoApi.renombrar(id, RenombrarRequest(nombre)) }
        }

    suspend fun archivarTipo(id: UUID): RespuestaRed<TipoEtiquetaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { tipoApi.archivarTipo(id) } }

    suspend fun activarTipo(id: UUID): RespuestaRed<TipoEtiquetaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { tipoApi.activarTipo(id) } }

    suspend fun conteoTipo(id: UUID): RespuestaRed<Int> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { conteoApi.deTipoEtiqueta(id) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.prendasActivas ?: 0)
        }
    }

    // --- Valores de un tipo de etiqueta ---

    suspend fun valores(tipoId: UUID): RespuestaRed<List<ValorEtiquetaResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { tipoApi.listarValores(tipoId) } }

    suspend fun agregarValor(tipoId: UUID, valor: String): RespuestaRed<ValorEtiquetaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { tipoApi.agregarValor(tipoId, AgregarValorRequest(valor)) }
        }

    suspend fun renombrarValor(tipoId: UUID, valorId: UUID, valor: String): RespuestaRed<ValorEtiquetaResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { tipoApi.renombrarValor(tipoId, valorId, RenombrarRequest(valor)) }
        }

    suspend fun archivarValor(tipoId: UUID, valorId: UUID): RespuestaRed<ValorEtiquetaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { tipoApi.archivarValor(tipoId, valorId) } }

    suspend fun activarValor(tipoId: UUID, valorId: UUID): RespuestaRed<ValorEtiquetaResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { tipoApi.activarValor(tipoId, valorId) } }

    suspend fun conteoValor(tipoId: UUID, valorId: UUID): RespuestaRed<Int> = withContext(dispatchers.io) {
        when (val r = ejecutarLlamada(gson) { conteoApi.deValorEtiqueta(tipoId, valorId) }) {
            is RespuestaRed.Fallo -> r
            is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.prendasActivas ?: 0)
        }
    }
}
