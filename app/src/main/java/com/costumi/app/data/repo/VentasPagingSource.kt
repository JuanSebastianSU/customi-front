package com.costumi.app.data.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.VentaControllerApi
import com.costumi.apiclient.models.VentaResponse
import com.google.gson.Gson

/** Paginación (Paging 3, C3) del listado de ventas. */
class VentasPagingSource(
    private val api: VentaControllerApi,
    private val gson: Gson,
    /** Codigo de retiro que escribio el usuario; null o vacio = sin filtrar. */
    private val buscar: String? = null,
    /** Rango de fechas (A8); null = sin acotar. */
    private val desde: java.time.LocalDate? = null,
    private val hasta: java.time.LocalDate? = null,
) : PagingSource<Int, VentaResponse>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VentaResponse> {
        val pagina = params.key ?: 0
        return when (val r = ejecutarLlamada(gson) {
            api.listar(buscar = buscar?.ifBlank { null }, desde = desde, hasta = hasta, pagina = pagina, tamano = params.loadSize)
        }) {
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

    override fun getRefreshKey(state: PagingState<Int, VentaResponse>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
}
