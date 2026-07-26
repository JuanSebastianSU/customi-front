package com.costumi.app.ui.gestion.ventas

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.VentaRepository
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.apiclient.models.RegistrarVentaRequest
import com.costumi.apiclient.models.VentaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import java.math.BigDecimal
import java.util.UUID

/**
 * POS de ventas: la clave de idempotencia es estable durante toda la vida del carrito (un reintento no
 * duplica la venta), y el registro enruta a venta simple o mixta (prendas + disfraces) según corresponda.
 */
class VentaPosViewModelTest {

    private lateinit var repo: VentaRepository
    private lateinit var disfrazRepo: DisfrazRepository
    private lateinit var pedido: PedidoDisfracesStore

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        disfrazRepo = mockk(relaxed = true)
        pedido = mockk(relaxed = true)
        // init -> cargar() consume repo.sucursales() con `is Fallo`; se stubbea a Fallo para salir temprano.
        coEvery { repo.sucursales() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = VentaPosViewModel(repo, disfrazRepo, pedido)

    private fun eventos(vm: VentaPosViewModel): MutableList<EventoPos> {
        val out = mutableListOf<EventoPos>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun registrar_ok_usa_la_clave_de_idempotencia_del_vm_y_emite_el_total() {
        val req = slot<RegistrarVentaRequest>()
        coEvery { repo.registrar(capture(req)) } returns RespuestaRed.Exito(VentaResponse(total = BigDecimal("50.00")))
        val vm = vm()
        val e = eventos(vm)
        vm.registrar(sucursalId = UUID.randomUUID(), clienteId = null, lineas = emptyList(), descuento = null)

        assertEquals(vm.claveIdempotencia, req.captured.claveIdempotencia) // misma clave estable del carrito
        val ev = e.filterIsInstance<EventoPos.Registrada>().firstOrNull()
        assertTrue(ev != null)
        assertEquals(BigDecimal("50.00"), ev!!.total)
    }

    @Test
    fun registrar_fallo_emite_error() {
        coEvery { repo.registrar(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.CONFLICTO, "sin stock"))
        val vm = vm()
        val e = eventos(vm)
        vm.registrar(UUID.randomUUID(), null, emptyList(), null)
        assertTrue(e.any { it is EventoPos.Error })
    }

    @Test
    fun registrar_mixto_ok_llama_vender_varios_limpia_el_pedido_y_avisa() {
        coEvery { disfrazRepo.venderVarios(any(), any(), any(), any()) } returns RespuestaRed.Exito(UUID.randomUUID())
        val vm = vm()
        val e = eventos(vm)
        vm.registrarMixto(UUID.randomUUID(), clienteId = null, lineas = emptyList(), items = emptyList())
        coVerify(exactly = 1) { disfrazRepo.venderVarios(any(), any(), any(), any()) }
        coVerify { pedido.limpiar() } // el carrito de disfraces se vacia tras vender
        assertTrue(e.any { it is EventoPos.Registrada })
    }
}
