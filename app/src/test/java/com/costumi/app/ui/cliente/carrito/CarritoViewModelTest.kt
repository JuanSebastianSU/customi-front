package com.costumi.app.ui.cliente.carrito

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.LineaDeCarritoResponse
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Carrito del cliente. Reglas que importan: un carrito inexistente (404) es "vacío", no un error; y
 * "Finalizar" (irAPago) NO crea la orden todavía (checkout diferido), solo navega al pago con los ids.
 */
class CarritoViewModelTest {

    private lateinit var repo: PedidoRepository
    private val empresaId = UUID.randomUUID().toString()
    private val sucursalId = UUID.randomUUID().toString()

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(
        tipo: String = "VENTA",
        empresa: String = empresaId,
        sucursal: String = sucursalId,
        carrito: RespuestaRed<CarritoResponse> = RespuestaRed.Fallo(ErrorApi(TipoError.NO_ENCONTRADO, "vacio")),
    ): CarritoViewModel {
        coEvery { repo.carritoPendiente(any(), any(), any()) } returns carrito
        val handle = SavedStateHandle(mapOf("empresaId" to empresa, "sucursalId" to sucursal, "tipo" to tipo))
        return CarritoViewModel(repo, handle)
    }

    private fun eventos(vm: CarritoViewModel): MutableList<EventoCheckout> {
        val out = mutableListOf<EventoCheckout>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun carrito_con_lineas_es_success() {
        val v = vm(carrito = RespuestaRed.Exito(CarritoResponse(lineas = listOf(LineaDeCarritoResponse()))))
        assertTrue(v.estado.value is UiState.Success)
    }

    @Test
    fun carrito_sin_lineas_es_vacio() {
        val v = vm(carrito = RespuestaRed.Exito(CarritoResponse(lineas = emptyList())))
        assertTrue(v.estado.value is UiState.Empty)
    }

    @Test
    fun carrito_inexistente_404_es_vacio_no_error() {
        val v = vm(carrito = RespuestaRed.Fallo(ErrorApi(TipoError.NO_ENCONTRADO, "no hay")))
        assertTrue(v.estado.value is UiState.Empty)
    }

    @Test
    fun otro_error_si_es_error() {
        val v = vm(carrito = RespuestaRed.Fallo(ErrorApi(TipoError.SERVIDOR, "boom")))
        assertTrue(v.estado.value is UiState.Error)
    }

    @Test
    fun ir_a_pago_navega_con_el_tipo_correcto_sin_crear_la_orden() {
        val v = vm(tipo = "RENTA")
        val e = eventos(v)
        v.irAPago()
        val ev = e.filterIsInstance<EventoCheckout.IrAPago>().firstOrNull()
        assertTrue(ev != null)
        assertEquals("RENTA", ev!!.tipo)
        assertEquals(empresaId, ev.empresaId)
        // checkout diferido: irAPago NO llama a ningun checkout.
        coVerify(exactly = 0) { repo.checkoutVenta(any(), any()) }
        coVerify(exactly = 0) { repo.checkoutRenta(any(), any()) }
    }

    @Test
    fun ir_a_pago_sin_ids_avisa_error() {
        val v = vm(empresa = "", sucursal = "")
        val e = eventos(v)
        v.irAPago()
        assertTrue(e.any { it is EventoCheckout.Error })
    }

    @Test
    fun cambiar_cantidad_a_menos_de_uno_no_llama_al_backend() {
        val v = vm()
        v.cambiarCantidad(LineaDeCarritoResponse(id = UUID.randomUUID()), cantidad = 0)
        coVerify(exactly = 0) { repo.editarCantidad(any(), any(), any(), any(), any()) }
    }
}
