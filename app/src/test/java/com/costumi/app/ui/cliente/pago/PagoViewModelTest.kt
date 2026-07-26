package com.costumi.app.ui.cliente.pago

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.app.data.repo.PagoClienteRepository
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.apis.CarritoControllerApi
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.CheckoutRentaResponse
import com.costumi.apiclient.models.CheckoutResponse
import com.costumi.apiclient.models.LineaDeCarritoResponse
import io.mockk.coEvery
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
import java.math.BigDecimal
import java.util.UUID

/**
 * Pago del cliente (checkout diferido): el total sale del carrito y la orden se materializa SOLO al
 * confirmar. Cubre el pago con tarjeta **simulado** de esta app (aprueba sin cobrar de verdad) y los bordes
 * del checkout de renta (0 / 1 / varias rentas), donde el código de retiro solo aplica con una sola orden.
 */
class PagoViewModelTest {

    private lateinit var pedidoRepo: PedidoRepository
    private lateinit var cuentaRepo: CuentaRepository
    private lateinit var pagoRepo: PagoClienteRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        pedidoRepo = mockk(relaxed = true)
        cuentaRepo = mockk(relaxed = true)
        pagoRepo = mockk(relaxed = true)
        // init llama a cargar() -> carritoPendiente; el código de retiro se busca en miHistorial.
        coEvery { pedidoRepo.carritoPendiente(any(), any(), any()) } returns RespuestaRed.Exito(
            CarritoResponse(lineas = listOf(LineaDeCarritoResponse()), total = BigDecimal("50.00")),
        )
        coEvery { cuentaRepo.miHistorial() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(tipo: String): PagoViewModel {
        val handle = SavedStateHandle(
            mapOf("tipo" to tipo, "empresaId" to UUID.randomUUID().toString(), "sucursalId" to UUID.randomUUID().toString()),
        )
        return PagoViewModel(pedidoRepo, cuentaRepo, pagoRepo, handle)
    }

    private fun eventos(vm: PagoViewModel): MutableList<EventoPago> {
        val out = mutableListOf<EventoPago>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun carrito_vacio_deja_la_pantalla_en_error() {
        coEvery { pedidoRepo.carritoPendiente(any(), any(), any()) } returns RespuestaRed.Exito(
            CarritoResponse(lineas = emptyList()),
        )
        val vm = vm("VENTA")
        assertTrue(vm.estado.value is com.costumi.app.core.UiState.Error)
    }

    @Test
    fun tarjeta_simulada_en_venta_aprueba_el_pago() {
        coEvery { pedidoRepo.checkoutVenta(any(), any()) } returns RespuestaRed.Exito(
            CheckoutResponse(ventaId = UUID.randomUUID()),
        )
        val vm = vm("VENTA")
        val e = eventos(vm)
        vm.pagarConTarjetaSimulado()
        assertTrue(e.any { it is EventoPago.TarjetaAprobada })
    }

    @Test
    fun tarjeta_simulada_con_venta_fallida_avisa_error() {
        coEvery { pedidoRepo.checkoutVenta(any(), any()) } returns RespuestaRed.Fallo(
            ErrorApi(TipoError.CONFLICTO, "sin stock"),
        )
        val vm = vm("VENTA")
        val e = eventos(vm)
        vm.pagarConTarjetaSimulado()
        assertTrue(e.any { it is EventoPago.Error && it.mensaje.contains("stock") })
    }

    @Test
    fun tarjeta_simulada_con_varias_rentas_aprueba_sin_codigo_unico() {
        // El checkout de renta puede crear varias rentas: entonces no hay un único código (se ven en Mis Pedidos).
        coEvery { pedidoRepo.checkoutRenta(any(), any()) } returns RespuestaRed.Exito(
            CheckoutRentaResponse(rentaIds = listOf(UUID.randomUUID(), UUID.randomUUID())),
        )
        val vm = vm("RENTA")
        val e = eventos(vm)
        vm.pagarConTarjetaSimulado()
        assertTrue(e.any { it is EventoPago.TarjetaAprobada && it.codigo == null })
    }

    @Test
    fun renta_sin_ninguna_renta_creada_es_error() {
        coEvery { pedidoRepo.checkoutRenta(any(), any()) } returns RespuestaRed.Exito(
            CheckoutRentaResponse(rentaIds = emptyList()),
        )
        val vm = vm("RENTA")
        val e = eventos(vm)
        vm.pagarConTarjetaSimulado()
        assertTrue(e.any { it is EventoPago.Error })
    }

    @Test
    fun pagar_en_tienda_en_venta_reserva_la_orden() {
        coEvery { pedidoRepo.checkoutVenta(any(), any()) } returns RespuestaRed.Exito(
            CheckoutResponse(ventaId = UUID.randomUUID()),
        )
        val vm = vm("VENTA")
        val e = eventos(vm)
        vm.pagarEnTienda()
        assertTrue(e.any { it is EventoPago.Reservado })
    }
}
