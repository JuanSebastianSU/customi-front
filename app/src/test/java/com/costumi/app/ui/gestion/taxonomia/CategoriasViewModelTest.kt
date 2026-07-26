package com.costumi.app.ui.gestion.taxonomia

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.CategoriaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Categorías (taxonomía): la lista ordena los archivados al final y por nombre; archivar pide confirmación
 * mostrando cuántas prendas cuelgan; crear avisa y recarga. Cubre también vacío/error.
 */
class CategoriasViewModelTest {

    private lateinit var repo: TaxonomiaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        coEvery { repo.categorias() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun cat(nombre: String, archivada: Boolean = false) =
        CategoriaResponse(id = UUID.randomUUID(), nombre = nombre, archivada = archivada)

    private fun eventos(vm: CategoriasViewModel): MutableList<EventoCategoria> {
        val out = mutableListOf<EventoCategoria>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun sin_categorias_es_empty() {
        coEvery { repo.categorias() } returns RespuestaRed.Exito(emptyList())
        assertTrue(CategoriasViewModel(repo).estado.value is UiState.Empty)
    }

    @Test
    fun ordena_activas_por_nombre_y_archivadas_al_final() {
        coEvery { repo.categorias() } returns RespuestaRed.Exito(
            listOf(cat("Zorro"), cat("Vieja", archivada = true), cat("Abrigo")),
        )
        val visibles = (CategoriasViewModel(repo).estado.value as UiState.Success).data
        assertEquals(listOf("Abrigo", "Zorro", "Vieja"), visibles.map { it.nombre }) // activas A-Z, archivada al final
    }

    @Test
    fun error_de_red_es_error() {
        coEvery { repo.categorias() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SERVIDOR, "boom"))
        assertTrue(CategoriasViewModel(repo).estado.value is UiState.Error)
    }

    @Test
    fun crear_ok_avisa() {
        coEvery { repo.categorias() } returns RespuestaRed.Exito(emptyList())
        coEvery { repo.crearCategoria(any()) } returns RespuestaRed.Exito(cat("Nueva"))
        val vm = CategoriasViewModel(repo)
        val e = eventos(vm)
        vm.crear("Nueva")
        assertTrue(e.any { it is EventoCategoria.Info })
    }

    @Test
    fun solicitar_archivar_emite_confirmacion_con_el_conteo_de_prendas() {
        coEvery { repo.categorias() } returns RespuestaRed.Exito(emptyList())
        coEvery { repo.conteoPrendas(any()) } returns RespuestaRed.Exito(7)
        val vm = CategoriasViewModel(repo)
        val e = eventos(vm)
        vm.solicitarArchivar(cat("Abrigo"))
        val conf = e.filterIsInstance<EventoCategoria.ConfirmarArchivar>().firstOrNull()
        assertTrue(conf != null && conf.prendas == 7)
    }
}
