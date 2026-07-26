package com.costumi.app.ui.gestion.inventario

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.InventarioRepository
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.CrearPrendaRequest
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaResponse
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
 * Formulario de prenda: alta vs edición van a endpoints distintos; la foto es best-effort (si falla, la
 * prenda queda guardada igual y se avisa); y las etiquetas disponibles descartan tipos archivados y tipos
 * sin valores. Son reglas fáciles de romper al tocar el guardado o la clasificación.
 */
class PrendaFormViewModelTest {

    private lateinit var repo: InventarioRepository
    private lateinit var taxonomia: TaxonomiaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        taxonomia = mockk(relaxed = true)
        // init -> cargarCategorias() (when, hay que stubbear) + cargarEtiquetas() (as?, no crashea).
        coEvery { repo.categorias() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
        coEvery { taxonomia.tiposEtiqueta() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(prendaId: String? = null): PrendaFormViewModel {
        val handle = SavedStateHandle(if (prendaId != null) mapOf("id" to prendaId) else emptyMap())
        return PrendaFormViewModel(repo, taxonomia, handle)
    }

    private fun eventos(vm: PrendaFormViewModel): MutableList<EventoForm> {
        val out = mutableListOf<EventoForm>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun guardar(vm: PrendaFormViewModel, foto: ByteArray? = null) = vm.guardar(
        nombre = "Capa", categoriaId = UUID.randomUUID(), tipo = CrearPrendaRequest.TipoArticulo.RENTA,
        precioRenta = null, precioVenta = null, costo = null, deposito = null,
        valorReposicion = null, valorDano = null, fotoBytes = foto,
    )

    @Test
    fun en_alta_llama_a_crear_no_a_editar() {
        coEvery { repo.crearPrenda(any()) } returns RespuestaRed.Exito(PrendaResponse(id = UUID.randomUUID()))
        val vm = vm(prendaId = null)
        guardar(vm)
        coVerify(exactly = 1) { repo.crearPrenda(any()) }
        coVerify(exactly = 0) { repo.editarPrenda(any(), any()) }
    }

    @Test
    fun en_edicion_llama_a_editar_no_a_crear() {
        coEvery { repo.editarPrenda(any(), any()) } returns RespuestaRed.Exito(PrendaResponse(id = UUID.randomUUID()))
        val vm = vm(prendaId = UUID.randomUUID().toString())
        guardar(vm)
        coVerify(exactly = 1) { repo.editarPrenda(any(), any()) }
        coVerify(exactly = 0) { repo.crearPrenda(any()) }
    }

    @Test
    fun foto_fallida_igual_guarda_la_prenda_con_aviso() {
        coEvery { repo.crearPrenda(any()) } returns RespuestaRed.Exito(PrendaResponse(id = UUID.randomUUID()))
        coEvery { repo.subirFoto(any(), any(), any(), any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_CONFIGURADO, "S3 off"))
        val vm = vm(prendaId = null)
        val e = eventos(vm)
        guardar(vm, foto = byteArrayOf(1, 2, 3))
        val guardada = e.filterIsInstance<EventoForm.Guardada>().firstOrNull()
        assertTrue("deberia emitir Guardada", guardada != null)
        assertTrue("con aviso de foto", guardada!!.avisoFoto != null)
    }

    @Test
    fun si_falla_el_guardado_no_intenta_subir_la_foto() {
        coEvery { repo.crearPrenda(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.VALIDACION, "nombre requerido"))
        val vm = vm(prendaId = null)
        val e = eventos(vm)
        guardar(vm, foto = byteArrayOf(1, 2, 3))
        assertTrue(e.any { it is EventoForm.Error })
        coVerify(exactly = 0) { repo.subirFoto(any(), any(), any(), any()) }
    }

    @Test
    fun etiquetas_descarta_tipos_archivados_y_sin_valores() {
        val tipoOk = TipoEtiquetaResponse(id = UUID.randomUUID(), nombre = "Color", archivada = false)
        val tipoSinValores = TipoEtiquetaResponse(id = UUID.randomUUID(), nombre = "Talla", archivada = false)
        val tipoArchivado = TipoEtiquetaResponse(id = UUID.randomUUID(), nombre = "Viejo", archivada = true)
        coEvery { taxonomia.tiposEtiqueta() } returns RespuestaRed.Exito(listOf(tipoOk, tipoSinValores, tipoArchivado))
        coEvery { taxonomia.valores(tipoOk.id!!) } returns RespuestaRed.Exito(
            listOf(ValorEtiquetaResponse(id = UUID.randomUUID(), valor = "Rojo", archivada = false)),
        )
        coEvery { taxonomia.valores(tipoSinValores.id!!) } returns RespuestaRed.Exito(emptyList())

        val vm = vm(prendaId = null) // init dispara cargarEtiquetas con estos stubs
        val disponibles = vm.etiquetasDisponibles.value

        // Solo el tipo activo CON valores queda: "Talla" (sin valores) y "Viejo" (archivado) se descartan.
        assertEquals(1, disponibles.size)
        assertEquals("Color", disponibles.first().tipo.nombre)
    }
}
