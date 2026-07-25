package com.costumi.app.data.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.models.ClienteResponse
import com.google.gson.Gson

/** Paginación (Paging 3, C3) de clientes con búsqueda e inclusión de archivados. */
class ClientesPagingSource(
    private val api: ClienteControllerApi,
    private val gson: Gson,
    private val buscar: String?,
    private val incluirArchivados: Boolean,
    private val filtro: String?,
) : PagingSource<Int, ClienteResponse>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ClienteResponse> {
        val pagina = params.key ?: 0
        val r = ejecutarLlamada(gson) {
            api.listar14(
                buscar = buscar?.takeIf { it.isNotBlank() },
                conPendientes = false,
                filtro = filtro,
                incluirArchivados = incluirArchivados,
                pagina = pagina,
                tamano = params.loadSize,
            )
        }
        return when (r) {
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

    override fun getRefreshKey(state: PagingState<Int, ClienteResponse>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
}
