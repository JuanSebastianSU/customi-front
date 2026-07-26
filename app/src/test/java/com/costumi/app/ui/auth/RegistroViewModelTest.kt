package com.costumi.app.ui.auth

import com.costumi.app.core.Rol
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
 * Validación del registro: los datos que no cumplen (vacíos / contraseña corta / no coinciden) avisan y
 * **no** llaman al backend. La primera línea de defensa antes de crear una cuenta.
 */
class RegistroViewModelTest {

    private val main = UnconfinedTestDispatcher()
    private val repo = mockk<AuthRepository>(relaxed = true)

    @Before fun antes() = Dispatchers.setMain(main)
    @After fun despues() = Dispatchers.resetMain()

    private fun eventos(vm: RegistroViewModel): MutableList<EventoAuth> {
        val out = mutableListOf<EventoAuth>()
        CoroutineScope(main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun hayError(e: List<EventoAuth>, frag: String) =
        e.any { it is EventoAuth.Error && it.mensaje.contains(frag, ignoreCase = true) }

    @Test
    fun campos_vacios_no_registran() {
        val vm = RegistroViewModel(repo)
        val e = eventos(vm)
        vm.registrar(email = "", password = "12345678", confirmar = "12345678")
        assertTrue(hayError(e, "Completa"))
        coVerify(exactly = 0) { repo.registro(any(), any()) }
    }

    @Test
    fun contrasena_corta_no_registra() {
        val vm = RegistroViewModel(repo)
        val e = eventos(vm)
        vm.registrar(email = "a@b.com", password = "corta", confirmar = "corta")
        assertTrue(hayError(e, "al menos 8"))
        coVerify(exactly = 0) { repo.registro(any(), any()) }
    }

    @Test
    fun contrasenas_distintas_no_registran() {
        val vm = RegistroViewModel(repo)
        val e = eventos(vm)
        vm.registrar(email = "a@b.com", password = "12345678", confirmar = "87654321")
        assertTrue(hayError(e, "no coinciden"))
        coVerify(exactly = 0) { repo.registro(any(), any()) }
    }

    @Test
    fun datos_validos_registran_con_el_email_recortado() {
        coEvery { repo.registro(any(), any()) } returns RespuestaRed.Exito(Rol.CLIENTE)
        val vm = RegistroViewModel(repo)
        vm.registrar(email = "  a@b.com  ", password = "12345678", confirmar = "12345678")
        coVerify(exactly = 1) { repo.registro("a@b.com", "12345678") }
    }
}
