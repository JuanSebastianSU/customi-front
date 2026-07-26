package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.apiclient.models.CrearDisfrazRequest
import com.costumi.apiclient.models.DisfrazResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Formulario de disfraz (feature insignia): alta vs edición van a endpoints distintos y la foto es
 * best-effort (si falla, el disfraz queda guardado igual y se avisa; si falla el guardado, no sube la foto).
 */
class DisfrazFormViewModelTest {

    private lateinit var repo: DisfrazRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        // init -> cargar() consume prendasParaSelector con `is Fallo`; se stubbea a Fallo para que
        // cargar() salga temprano sin castear un mock a Exito.
        coEvery { repo.prendasParaSelector() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(disfrazId: String? = null): DisfrazFormViewModel {
        val handle = SavedStateHandle(if (disfrazId != null) mapOf("disfrazId" to disfrazId) else emptyMap())
        return DisfrazFormViewModel(repo, handle)
    }

    private fun eventos(vm: DisfrazFormViewModel): MutableList<EventoDisfrazForm> {
        val out = mutableListOf<EventoDisfrazForm>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun en_alta_llama_a_crear_no_a_editar() {
        coEvery { repo.crear(any()) } returns RespuestaRed.Exito(DisfrazResponse(id = UUID.randomUUID()))
        val vm = vm(disfrazId = null)
        vm.guardar(CrearDisfrazRequest())
        coVerify(exactly = 1) { repo.crear(any()) }
        coVerify(exactly = 0) { repo.editar(any(), any()) }
    }

    @Test
    fun en_edicion_llama_a_editar_no_a_crear() {
        coEvery { repo.editar(any(), any()) } returns RespuestaRed.Exito(DisfrazResponse(id = UUID.randomUUID()))
        val vm = vm(disfrazId = UUID.randomUUID().toString())
        vm.guardar(CrearDisfrazRequest())
        coVerify(exactly = 1) { repo.editar(any(), any()) }
        coVerify(exactly = 0) { repo.crear(any()) }
    }

    @Test
    fun foto_fallida_igual_guarda_con_aviso() {
        coEvery { repo.crear(any()) } returns RespuestaRed.Exito(DisfrazResponse(id = UUID.randomUUID()))
        coEvery { repo.subirFoto(any(), any(), any(), any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_CONFIGURADO, "S3 off"))
        val vm = vm(disfrazId = null)
        val e = eventos(vm)
        vm.guardar(CrearDisfrazRequest(), fotoBytes = byteArrayOf(1, 2, 3))
        val guardado = e.filterIsInstance<EventoDisfrazForm.Guardado>().firstOrNull()
        assertTrue(guardado != null && guardado.avisoFoto != null)
    }

    @Test
    fun si_falla_el_guardado_no_sube_la_foto() {
        coEvery { repo.crear(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.VALIDACION, "faltan slots"))
        val vm = vm(disfrazId = null)
        val e = eventos(vm)
        vm.guardar(CrearDisfrazRequest(), fotoBytes = byteArrayOf(1, 2, 3))
        assertTrue(e.any { it is EventoDisfrazForm.Error })
        coVerify(exactly = 0) { repo.subirFoto(any(), any(), any(), any()) }
    }
}
