package com.costumi.app.ui.gestion.clientes

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.ClientesRepository
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.EstadoDeCuentaResponse
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
 * Cartera de clientes: los toggles deben ir en la dirección correcta según el estado actual (un cliente
 * archivado se ACTIVA, uno activo se ARCHIVA; en lista negra se QUITA, etc.). Invertirlos sin querer haría lo
 * contrario de lo que el usuario tocó. También cubre el estado de cuenta (desglose de deuda).
 */
class ClientesViewModelTest {

    private lateinit var repo: ClientesRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun cliente(archivada: Boolean = false, enNegra: Boolean = false) =
        ClienteResponse(id = UUID.randomUUID(), archivada = archivada, enListaNegra = enNegra)

    private fun eventos(vm: ClientesViewModel): MutableList<EventoCliente> {
        val out = mutableListOf<EventoCliente>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun ver_estado_de_cuenta_ok_lo_emite() {
        coEvery { repo.estadoCuenta(any()) } returns RespuestaRed.Exito(EstadoDeCuentaResponse())
        val vm = ClientesViewModel(repo)
        val e = eventos(vm)
        vm.verEstadoCuenta(cliente())
        assertTrue(e.any { it is EventoCliente.EstadoCuenta })
    }

    @Test
    fun ver_estado_de_cuenta_fallo_avisa_error() {
        coEvery { repo.estadoCuenta(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_ENCONTRADO, "no existe"))
        val vm = ClientesViewModel(repo)
        val e = eventos(vm)
        vm.verEstadoCuenta(cliente())
        assertTrue(e.any { it is EventoCliente.Error })
    }

    @Test
    fun cliente_archivado_se_activa() {
        coEvery { repo.activar(any()) } returns RespuestaRed.Exito(cliente())
        val vm = ClientesViewModel(repo)
        vm.alternarArchivado(cliente(archivada = true))
        coVerify(exactly = 1) { repo.activar(any()) }
        coVerify(exactly = 0) { repo.archivar(any()) }
    }

    @Test
    fun cliente_activo_se_archiva() {
        coEvery { repo.archivar(any()) } returns RespuestaRed.Exito(cliente())
        val vm = ClientesViewModel(repo)
        vm.alternarArchivado(cliente(archivada = false))
        coVerify(exactly = 1) { repo.archivar(any()) }
        coVerify(exactly = 0) { repo.activar(any()) }
    }

    @Test
    fun lista_negra_se_toggle_en_la_direccion_correcta() {
        coEvery { repo.cambiarListaNegra(any(), any()) } returns RespuestaRed.Exito(cliente())
        val vm = ClientesViewModel(repo)
        vm.alternarListaNegra(cliente(enNegra = false)) // no esta -> ponerlo (true)
        coVerify(exactly = 1) { repo.cambiarListaNegra(any(), true) }
        vm.alternarListaNegra(cliente(enNegra = true))  // ya esta -> quitarlo (false)
        coVerify(exactly = 1) { repo.cambiarListaNegra(any(), false) }
    }
}
