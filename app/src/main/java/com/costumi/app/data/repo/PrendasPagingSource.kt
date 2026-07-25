package com.costumi.app.data.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.models.PrendaResponse
import com.google.gson.Gson

/**
 * Fuente de paginación (Paging 3, C3) para el inventario de prendas. El backend pagina 0-based y
 * responde `{contenido, pagina, totalPaginas}`; los errores llegan por [ejecutarLlamada] (RFC 7807).
 */
class PrendasPagingSource(
    private val api: PrendaControllerApi,
    private val gson: Gson,
    /** Texto que escribio el usuario; null o vacio = sin filtrar. */
    private val buscar: String? = null,
) : PagingSource<Int, PrendaResponse>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PrendaResponse> {
        val pagina = params.key ?: 0
        return when (val r = ejecutarLlamada(gson) { api.listar4(buscar = buscar?.ifBlank { null }, pagina = pagina, tamano = params.loadSize) }) {
            is RespuestaRed.Fallo -> LoadResult.Error(RuntimeException(r.error.mensaje))
            is RespuestaRed.Exito -> {
                val contenido = r.data.contenido.orEmpty()
                val totalPaginas = r.data.totalPaginas ?: 1
                LoadResult.Page(
                    data = contenido,
                    prevKey = if (pagina == 0) null else pagina - 1,
                    nextKey = if (pagina + 1 >= totalPaginas || contenido.isEmpty()) null else pagina + 1,
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PrendaResponse>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
}
