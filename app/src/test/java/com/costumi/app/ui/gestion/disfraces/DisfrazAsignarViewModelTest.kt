package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.DisfrazRepository
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
import java.time.LocalDate
import java.util.UUID

/**
 * Vender/rentar un disfraz a un cliente desde mostrador. Valida en orden antes de tocar la red: cliente →
 * sucursal → (en renta) fechas. Sin estas guardas se crearían operaciones incompletas o sin dueño.
 */
class DisfrazAsignarViewModelTest {

    private lateinit var repo: DisfrazRepository
    private lateinit var pedido: PedidoDisfracesStore

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        pedido = mockk(relaxed = true)
        // init -> cargar() -> detalleAsistido; se stubbea a Fallo para no armar el UI (no afecta rentar/vender).
        coEvery { repo.detalleAsistido(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(): DisfrazAsignarViewModel {
        val handle = SavedStateHandle(mapOf("disfrazId" to UUID.randomUUID().toString(), "nombre" to "Pirata"))
        return DisfrazAsignarViewModel(repo, pedido, handle)
    }

    private fun eventos(vm: DisfrazAsignarViewModel): MutableList<EventoAsignar> {
        val out = mutableListOf<EventoAsignar>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun hayError(e: List<EventoAsignar>, frag: String) =
        e.any { it is EventoAsignar.Error && it.mensaje.contains(frag, ignoreCase = true) }

    private val retiro = LocalDate.of(2026, 1, 10)
    private val devolucion = LocalDate.of(2026, 1, 13)

    @Test
    fun rentar_sin_cliente_avisa_y_no_llama() {
        val v = vm()
        val e = eventos(v)
        v.rentar(retiro, devolucion) // clienteId sigue null
        assertTrue(hayError(e, "cliente"))
        coVerify(exactly = 0) { repo.rentarParaCliente(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun rentar_con_cliente_pero_sin_sucursal_avisa() {
        val v = vm()
        v.clienteId = UUID.randomUUID()
        val e = eventos(v)
        v.rentar(retiro, devolucion) // sucursalId null (detalle fallo no la seteo)
        assertTrue(hayError(e, "sucursal"))
    }

    @Test
    fun rentar_con_cliente_y_sucursal_pero_sin_fechas_avisa() {
        val v = vm()
        v.clienteId = UUID.randomUUID()
        v.sucursalId = UUID.randomUUID()
        val e = eventos(v)
        v.rentar(retiro = null, devolucion = null)
        assertTrue(hayError(e, "fechas"))
    }

    @Test
    fun rentar_completo_llama_al_repo_y_avisa_exito() {
        coEvery { repo.rentarParaCliente(any(), any(), any(), any(), any(), any(), any()) } returns RespuestaRed.Exito(UUID.randomUUID())
        val v = vm()
        v.clienteId = UUID.randomUUID()
        v.sucursalId = UUID.randomUUID()
        val e = eventos(v)
        v.rentar(retiro, devolucion)
        coVerify(exactly = 1) { repo.rentarParaCliente(any(), any(), any(), any(), any(), any(), any()) }
        assertTrue(e.any { it is EventoAsignar.Exito })
    }

    @Test
    fun vender_sin_cliente_avisa() {
        val v = vm()
        val e = eventos(v)
        v.vender()
        assertTrue(hayError(e, "cliente"))
        coVerify(exactly = 0) { repo.venderParaCliente(any(), any(), any(), any(), any()) }
    }

    @Test
    fun vender_completo_llama_al_repo() {
        coEvery { repo.venderParaCliente(any(), any(), any(), any(), any()) } returns RespuestaRed.Exito(UUID.randomUUID())
        val v = vm()
        v.clienteId = UUID.randomUUID()
        v.sucursalId = UUID.randomUUID()
        v.vender()
        coVerify(exactly = 1) { repo.venderParaCliente(any(), any(), any(), any(), any()) }
    }

    @Test
    fun agregar_al_pedido_agrega_al_carrito_y_avisa() {
        val v = vm()
        val e = eventos(v)
        v.agregarAlPedido()
        coVerify(exactly = 1) { pedido.agregar(any()) }
        assertTrue(e.any { it is EventoAsignar.Exito })
    }
}
