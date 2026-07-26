package com.costumi.app.ui.gestion.identidad

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.MiEmpresaRepository
import com.costumi.apiclient.models.EmpresaResponse
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

/**
 * Identidad de la tienda: el nombre es obligatorio (si está en blanco, avisa y no guarda); al guardar/subir
 * foto con éxito actualiza la tienda visible y avisa; si falla, avisa error.
 */
class IdentidadTiendaViewModelTest {

    private lateinit var repo: MiEmpresaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun eventos(vm: IdentidadTiendaViewModel): MutableList<EventoIdentidad> {
        val out = mutableListOf<EventoIdentidad>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun editar_con_nombre_en_blanco_avisa_y_no_guarda() {
        val vm = IdentidadTiendaViewModel(repo)
        val e = eventos(vm)
        vm.editar(nombre = "   ", descripcion = null, ciudad = null, ubicacion = null, contacto = null)
        assertTrue(e.any { it is EventoIdentidad.Error && it.mensaje.contains("obligatorio") })
        coVerify(exactly = 0) { repo.editar(any(), any(), any(), any(), any()) }
    }

    @Test
    fun editar_valido_guarda_actualiza_la_tienda_y_avisa() {
        coEvery { repo.editar(any(), any(), any(), any(), any()) } returns RespuestaRed.Exito(EmpresaResponse(nombre = "Nueva"))
        val vm = IdentidadTiendaViewModel(repo)
        val e = eventos(vm)
        vm.editar(nombre = "Nueva", descripcion = null, ciudad = null, ubicacion = null, contacto = null)
        coVerify(exactly = 1) { repo.editar("Nueva", any(), any(), any(), any()) }
        assertTrue(e.any { it is EventoIdentidad.Info })
        assertTrue(vm.empresa.value?.nombre == "Nueva")
    }

    @Test
    fun subir_logo_fallido_avisa_error() {
        coEvery { repo.subirLogo(any(), any(), any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_CONFIGURADO, "S3 off"))
        val vm = IdentidadTiendaViewModel(repo)
        val e = eventos(vm)
        vm.subirLogo(byteArrayOf(1), "image/jpeg", "logo.jpg")
        assertTrue(e.any { it is EventoIdentidad.Error })
    }
}
