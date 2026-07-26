package com.costumi.app.ui.auth

import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.AuthRepository
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
 * Recuperación de contraseña: "olvidé" (enviar correo) y "restablecer" (con el código). Validan las entradas
 * antes de tocar la red y recortan lo que mandan.
 */
class RecuperacionViewModelTest {

    private val main = UnconfinedTestDispatcher()
    private val repo = mockk<AuthRepository>(relaxed = true)

    @Before fun antes() = Dispatchers.setMain(main)
    @After fun despues() = Dispatchers.resetMain()

    private fun eventos(flujo: kotlinx.coroutines.flow.Flow<EventoAuth>): MutableList<EventoAuth> {
        val out = mutableListOf<EventoAuth>()
        CoroutineScope(main).launch { flujo.collect { out.add(it) } }
        return out
    }

    private fun hayError(e: List<EventoAuth>, frag: String) =
        e.any { it is EventoAuth.Error && it.mensaje.contains(frag, ignoreCase = true) }

    // ---- Recuperar (olvidé) ----

    @Test
    fun recuperar_con_email_vacio_no_llama_al_backend() {
        val vm = RecuperarViewModel(repo)
        val e = eventos(vm.eventos)
        vm.enviar(email = "   ")
        assertTrue(hayError(e, "correo"))
        coVerify(exactly = 0) { repo.olvide(any()) }
    }

    @Test
    fun recuperar_valido_envia_con_email_recortado() {
        coEvery { repo.olvide(any()) } returns RespuestaRed.Exito(Unit)
        val vm = RecuperarViewModel(repo)
        vm.enviar(email = "  a@b.com  ")
        coVerify(exactly = 1) { repo.olvide("a@b.com") }
    }

    // ---- Restablecer (con código) ----

    @Test
    fun restablecer_con_campos_vacios_no_llama() {
        val vm = RestablecerViewModel(repo)
        val e = eventos(vm.eventos)
        vm.restablecer(token = "", password = "12345678", confirmar = "12345678")
        assertTrue(hayError(e, "codigo"))
        coVerify(exactly = 0) { repo.restablecer(any(), any()) }
    }

    @Test
    fun restablecer_con_password_corta_no_llama() {
        val vm = RestablecerViewModel(repo)
        val e = eventos(vm.eventos)
        vm.restablecer(token = "tok", password = "corta", confirmar = "corta")
        assertTrue(hayError(e, "al menos 8"))
        coVerify(exactly = 0) { repo.restablecer(any(), any()) }
    }

    @Test
    fun restablecer_con_password_distinta_no_llama() {
        val vm = RestablecerViewModel(repo)
        val e = eventos(vm.eventos)
        vm.restablecer(token = "tok", password = "12345678", confirmar = "87654321")
        assertTrue(hayError(e, "no coinciden"))
        coVerify(exactly = 0) { repo.restablecer(any(), any()) }
    }

    @Test
    fun restablecer_valido_llama_con_token_recortado() {
        coEvery { repo.restablecer(any(), any()) } returns RespuestaRed.Exito(Unit)
        val vm = RestablecerViewModel(repo)
        vm.restablecer(token = "  tok123  ", password = "12345678", confirmar = "12345678")
        coVerify(exactly = 1) { repo.restablecer("tok123", "12345678") }
    }
}
