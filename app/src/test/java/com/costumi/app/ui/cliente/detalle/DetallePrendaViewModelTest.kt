package com.costumi.app.ui.cliente.detalle

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.SucursalVitrinaResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Detalle de una prenda para el cliente: deriva de los precios recibidos si permite renta/venta (y muestra
 * el precio de renta "por día"); y al agregar valida las fechas (en renta) y que haya sucursal de retiro.
 */
class DetallePrendaViewModelTest {

    private lateinit var repo: PedidoRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        // init -> cargarSucursales(); por defecto sin sucursales.
        coEvery { repo.sucursales(any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(
        precioRenta: String? = "10.00",
        precioVenta: String? = "80.00",
        conSucursal: Boolean = false,
    ): DetallePrendaViewModel {
        if (conSucursal) {
            coEvery { repo.sucursales(any()) } returns RespuestaRed.Exito(listOf(SucursalVitrinaResponse(id = UUID.randomUUID())))
        }
        val args = buildMap<String, Any> {
            put("empresaId", UUID.randomUUID().toString())
            put("prendaId", UUID.randomUUID().toString())
            precioRenta?.let { put("precioRenta", it) }
            precioVenta?.let { put("precioVenta", it) }
        }
        return DetallePrendaViewModel(repo, SavedStateHandle(args))
    }

    private fun eventos(vm: DetallePrendaViewModel): MutableList<EventoDetalle> {
        val out = mutableListOf<EventoDetalle>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun deriva_permisos_y_muestra_el_precio_de_renta_por_dia() {
        val v = vm(precioRenta = "10.00", precioVenta = null)
        assertTrue(v.permiteRenta)
        assertFalse(v.permiteVenta)
        assertTrue(v.precioRentaTexto?.contains("/ dia") == true)
    }

    @Test
    fun sin_precio_de_renta_no_permite_renta() {
        val v = vm(precioRenta = null, precioVenta = "80.00")
        assertFalse(v.permiteRenta)
        assertTrue(v.permiteVenta)
    }

    @Test
    fun agregar_renta_sin_fechas_avisa_y_no_llama() {
        val v = vm(conSucursal = true)
        val e = eventos(v)
        v.agregar(AgregarItemRequest.Tipo.RENTA, cantidad = 1, fechaRetiro = null, fechaDevolucion = null)
        assertTrue(e.any { it is EventoDetalle.Error && it.mensaje.contains("fechas") })
        coVerify(exactly = 0) { repo.agregarAlCarrito(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun agregar_sin_sucursal_avisa() {
        val v = vm(conSucursal = false) // sucursales fallo -> ninguna seleccionada
        val e = eventos(v)
        v.agregar(AgregarItemRequest.Tipo.VENTA, cantidad = 1, fechaRetiro = null, fechaDevolucion = null)
        assertTrue(e.any { it is EventoDetalle.Error && it.mensaje.contains("recibir pedidos") })
        coVerify(exactly = 0) { repo.agregarAlCarrito(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun agregar_venta_con_sucursal_llama_al_carrito() {
        coEvery { repo.agregarAlCarrito(any(), any(), any(), any(), any(), any(), any()) } returns RespuestaRed.Exito(CarritoResponse())
        val v = vm(conSucursal = true)
        val e = eventos(v)
        v.agregar(AgregarItemRequest.Tipo.VENTA, cantidad = 1, fechaRetiro = null, fechaDevolucion = null)
        coVerify(exactly = 1) { repo.agregarAlCarrito(any(), any(), any(), any(), any(), any(), any()) }
        assertTrue(e.any { it is EventoDetalle.Agregado })
    }
}
