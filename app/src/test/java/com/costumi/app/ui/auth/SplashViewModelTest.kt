package com.costumi.app.ui.auth

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.ModoApp
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.Rol
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ruteo del Splash. Decide adónde entra la app al abrir: al home del modo (sesión válida), al login (sin
 * sesión o token inválido) o muestra "Reintentar" (error recuperable como falta de red con sesión guardada).
 * Un error aquí manda al usuario a la pantalla equivocada o lo desloguea sin querer.
 */
class SplashViewModelTest {

    private lateinit var repo: AuthRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun destinoDe(vm: SplashViewModel) = vm.destino.replayCache.lastOrNull()

    @Test
    fun sin_sesion_va_a_login() {
        every { repo.haySesion() } returns false
        val vm = SplashViewModel(repo)
        assertEquals(DestinoSplash.Login, destinoDe(vm))
    }

    @Test
    fun sesion_valida_va_al_home_del_modo() {
        every { repo.haySesion() } returns true
        coEvery { repo.rolActual() } returns RespuestaRed.Exito(Rol.CLIENTE)
        val vm = SplashViewModel(repo)
        assertEquals(DestinoSplash.Home(ModoApp.CLIENTE), destinoDe(vm))
    }

    @Test
    fun token_invalido_401_va_a_login() {
        every { repo.haySesion() } returns true
        coEvery { repo.rolActual() } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_AUTORIZADO, "expiro"))
        val vm = SplashViewModel(repo)
        assertEquals(DestinoSplash.Login, destinoDe(vm))
    }

    @Test
    fun sin_conexion_con_sesion_muestra_error_recuperable_no_desloguea() {
        every { repo.haySesion() } returns true
        coEvery { repo.rolActual() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red"))
        val vm = SplashViewModel(repo)
        assertTrue(vm.error.value != null)          // muestra Reintentar
        assertNull(destinoDe(vm))                    // NO manda a login (no borra la sesión por falta de red)
    }
}
