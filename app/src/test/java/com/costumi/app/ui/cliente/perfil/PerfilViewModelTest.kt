package com.costumi.app.ui.cliente.perfil

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.app.data.repo.MembresiaRepository
import com.costumi.app.data.repo.PerfilRepository
import com.costumi.app.data.repo.PushRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validación del cambio de contraseña en el ViewModel: cuando los datos no cumplen (campos vacíos, no
 * coinciden, muy corta), **avisa y NO llama al backend**. Es una guarda de UX + evita un round-trip inútil.
 */
class PerfilViewModelTest {

    private val main = UnconfinedTestDispatcher()
    private lateinit var perfilRepo: PerfilRepository
    private lateinit var authRepo: AuthRepository

    @Before
    fun antes() {
        Dispatchers.setMain(main)
        perfilRepo = mockk(relaxed = true)
        authRepo = mockk(relaxed = true)
        // init del VM: observar perfil + refrescar + leer membresía. Se stubbea lo mínimo para que no falle.
        every { perfilRepo.observarPerfil() } returns flowOf(null)
        coEvery { perfilRepo.refrescarPerfil() } returns RespuestaRed.Exito(Unit)
        coEvery { authRepo.me() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun vm() = PerfilViewModel(
        authRepo,
        mockk<CuentaRepository>(relaxed = true),
        perfilRepo,
        mockk<MembresiaRepository>(relaxed = true),
        mockk<PushRepository>(relaxed = true),
    )

    /** Colecta los eventos del VM en una lista (subscribe antes de disparar la acción). */
    private fun capturarEventos(vm: PerfilViewModel): MutableList<EventoPerfil> {
        val eventos = mutableListOf<EventoPerfil>()
        CoroutineScope(main).launch { vm.eventos.collect { eventos.add(it) } }
        return eventos
    }

    private fun huboError(eventos: List<EventoPerfil>, fragmento: String) =
        eventos.any { it is EventoPerfil.Error && it.mensaje.contains(fragmento, ignoreCase = true) }

    @Test
    fun campos_vacios_avisan_y_no_llaman_al_backend() {
        val vm = vm()
        val eventos = capturarEventos(vm)
        vm.cambiarContrasena(actual = "", nueva = "12345678", repetida = "12345678")
        assertTrue(huboError(eventos, "Completa"))
        coVerify(exactly = 0) { perfilRepo.cambiarContrasena(any(), any()) }
    }

    @Test
    fun contrasenas_que_no_coinciden_avisan() {
        val vm = vm()
        val eventos = capturarEventos(vm)
        vm.cambiarContrasena(actual = "vieja", nueva = "12345678", repetida = "87654321")
        assertTrue(huboError(eventos, "no coincide"))
        coVerify(exactly = 0) { perfilRepo.cambiarContrasena(any(), any()) }
    }

    @Test
    fun contrasena_corta_avisa() {
        val vm = vm()
        val eventos = capturarEventos(vm)
        vm.cambiarContrasena(actual = "vieja", nueva = "corta", repetida = "corta")
        assertTrue(huboError(eventos, "al menos 8"))
        coVerify(exactly = 0) { perfilRepo.cambiarContrasena(any(), any()) }
    }

    @Test
    fun contrasena_valida_si_llama_al_backend() {
        coEvery { perfilRepo.cambiarContrasena(any(), any()) } returns RespuestaRed.Exito(Unit)
        val vm = vm()
        vm.cambiarContrasena(actual = "vieja", nueva = "12345678", repetida = "12345678")
        coVerify(exactly = 1) { perfilRepo.cambiarContrasena("vieja", "12345678") }
    }
}
