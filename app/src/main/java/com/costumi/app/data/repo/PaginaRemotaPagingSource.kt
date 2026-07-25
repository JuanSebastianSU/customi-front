package com.costumi.app.data.repo

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.costumi.app.core.RespuestaRed

/**
 * `PagingSource` genérico para los listados que el backend pagina 0-based y responde
 * `{contenido, totalPaginas}`. Reemplaza a copiar un `*PagingSource` por lista: cada repo solo dice
 * **cómo pedir una página** ([pedir]) y **cómo leer el cuerpo** ([items]/[totalPaginas]).
 *
 * Es el patrón de [PrendasPagingSource]/[VentasPagingSource]/[RentasPagingSource]/[ClientesPagingSource]
 * factorizado. Esos cuatro pueden migrar a este cuando se toque su zona; no de golpe.
 *
 * [pedir] ya devuelve [RespuestaRed] (el repo envuelve la llamada en `ejecutarLlamada`, RFC 7807).
 */
class PaginaRemotaPagingSource<R : Any, T : Any>(
    private val pedir: suspend (pagina: Int, tamano: Int) -> RespuestaRed<R>,
    private val items: (R) -> List<T>,
    private val totalPaginas: (R) -> Int,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val pagina = params.key ?: 0
        return when (val r = pedir(pagina, params.loadSize)) {
            is RespuestaRed.Fallo -> LoadResult.Error(RuntimeException(r.error.mensaje))
            is RespuestaRed.Exito -> {
                val contenido = items(r.data)
                val total = totalPaginas(r.data).coerceAtLeast(1)
                LoadResult.Page(
                    data = contenido,
                    prevKey = if (pagina == 0) null else pagina - 1,
                    nextKey = if (pagina + 1 >= total || contenido.isEmpty()) null else pagina + 1,
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { it.prevKey?.plus(1) ?: it.nextKey?.minus(1) }
        }
}
