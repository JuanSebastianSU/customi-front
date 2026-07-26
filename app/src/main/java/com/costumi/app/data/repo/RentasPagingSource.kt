package com.costumi.app.data.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.RentaControllerApi
import com.costumi.apiclient.models.RentaResponse
import com.google.gson.Gson

/** Paginación (Paging 3, C3) del listado de rentas. */
class RentasPagingSource(
    private val api: RentaControllerApi,
    private val gson: Gson,
    /** Codigo de retiro que escribio el usuario; null o vacio = sin filtrar. */
    private val buscar: String? = null,
    /** Bandeja/estado (chip): POR_ENTREGAR, ACTIVAS, VENCIDAS, CERRADAS; null = todas. */
    private val filtro: String? = null,
    /** Si viene, acota a las rentas de ese cliente (atajo desde la ficha del cliente). */
    private val clienteId: java.util.UUID? = null,
) : PagingSource<Int, RentaResponse>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RentaResponse> {
        val pagina = params.key ?: 0
        return when (val r = ejecutarLlamada(gson) { api.listar2(clienteId = clienteId, buscar = buscar?.ifBlank { null }, filtro = filtro, pagina = pagina, tamano = params.loadSize) }) {
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

    override fun getRefreshKey(state: PagingState<Int, RentaResponse>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
}
