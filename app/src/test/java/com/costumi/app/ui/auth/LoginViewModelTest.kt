package com.costumi.app.ui.auth

import com.costumi.app.core.Rol
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.AuthRepository
import com.costumi.app.data.repo.InvitacionRepository
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
 * Login e invitaciones: valida entradas antes de tocar la red, recorta el correo, y saca el token del texto
 * pegado (enlace o código). Estas guardas evitan llamadas inútiles y errores por datos mal formados.
 */
class LoginViewModelTest {

    private val main = UnconfinedTestDispatcher()
    private val repo = mockk<AuthRepository>(relaxed = true)
    private val invitaciones = mockk<InvitacionRepository>(relaxed = true)

    @Before fun antes() = Dispatchers.setMain(main)
    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = LoginViewModel(repo, invitaciones)

    private fun eventos(vm: LoginViewModel): MutableList<EventoAuth> {
        val out = mutableListOf<EventoAuth>()
        CoroutineScope(main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun hayError(e: List<EventoAuth>, frag: String) =
        e.any { it is EventoAuth.Error && it.mensaje.contains(frag, ignoreCase = true) }

    @Test
    fun login_con_campos_vacios_no_llama_al_backend() {
        val vm = vm()
        val e = eventos(vm)
        vm.login(email = "", password = "")
        assertTrue(hayError(e, "correo"))
        coVerify(exactly = 0) { repo.login(any(), any()) }
    }

    @Test
    fun login_valido_llama_con_email_recortado() {
        coEvery { repo.login(any(), any()) } returns RespuestaRed.Exito(Rol.CLIENTE)
        val vm = vm()
        vm.login(email = "  a@b.com ", password = "secreta1")
        coVerify(exactly = 1) { repo.login("a@b.com", "secreta1") }
    }

    @Test
    fun aceptar_invitacion_sin_terminos_avisa() {
        val vm = vm()
        val e = eventos(vm)
        vm.aceptarInvitacion(token = "tok", password = "12345678", aceptaTerminos = false)
        assertTrue(hayError(e, "términos"))
        coVerify(exactly = 0) { invitaciones.aceptar(any(), any(), any()) }
    }

    @Test
    fun aceptar_invitacion_con_password_corta_avisa() {
        val vm = vm()
        val e = eventos(vm)
        vm.aceptarInvitacion(token = "tok", password = "corta", aceptaTerminos = true)
        assertTrue(hayError(e, "al menos 8"))
        coVerify(exactly = 0) { invitaciones.aceptar(any(), any(), any()) }
    }

    @Test
    fun ver_invitacion_saca_el_token_del_enlace_pegado() {
        coEvery { invitaciones.ver(any()) } returns RespuestaRed.Fallo(
            com.costumi.app.core.ErrorApi(com.costumi.app.core.TipoError.NO_ENCONTRADO, "x"),
        )
        val vm = vm()
        // De una URL con querystring se queda con el último segmento del path.
        vm.verInvitacion("https://costumi.app/invitacion/ABC123?ref=wa")
        coVerify(exactly = 1) { invitaciones.ver("ABC123") }
    }
}
