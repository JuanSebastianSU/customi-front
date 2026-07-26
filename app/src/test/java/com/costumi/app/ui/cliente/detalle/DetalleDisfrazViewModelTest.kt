package com.costumi.app.ui.cliente.detalle

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.FavoritosRepository
import com.costumi.app.data.repo.MarketplaceRepository
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.SucursalVitrinaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Armado de un disfraz para el cliente: guarda la elección por slot y valida antes de agregar al carrito
 * (sucursal de retiro obligatoria, fechas obligatorias en renta). Sin estas guardas se mandarían pedidos
 * incompletos al backend.
 */
class DetalleDisfrazViewModelTest {

    private val empresaId = UUID.randomUUID().toString()
    private val disfrazId = UUID.randomUUID().toString()

    private lateinit var repo: MarketplaceRepository
    private lateinit var pedidos: PedidoRepository
    private lateinit var favoritos: FavoritosRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        pedidos = mockk(relaxed = true)
        favoritos = mockk(relaxed = true)
        // init: cargar() (detalle) + cargarSucursales() + esFavorito(). Se stubbea lo mínimo.
        coEvery { repo.disfrazDetalle(any(), any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.NO_ENCONTRADO, "x"))
        every { favoritos.esFavorito(any()) } returns flowOf(false)
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm(sucursales: List<SucursalVitrinaResponse> = emptyList()): DetalleDisfrazViewModel {
        coEvery { repo.sucursales(any()) } returns RespuestaRed.Exito(sucursales)
        val handle = SavedStateHandle(
            mapOf("empresaId" to empresaId, "disfrazId" to disfrazId, "nombre" to "Pirata"),
        )
        return DetalleDisfrazViewModel(repo, pedidos, favoritos, handle)
    }

    private fun sucursal() = SucursalVitrinaResponse(id = UUID.randomUUID())

    private fun eventos(vm: DetalleDisfrazViewModel): MutableList<EventoDisfraz> {
        val out = mutableListOf<EventoDisfraz>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    private fun hayError(e: List<EventoDisfraz>, frag: String) =
        e.any { it is EventoDisfraz.Error && it.mensaje.contains(frag, ignoreCase = true) }

    @Test
    fun seleccionar_guarda_la_eleccion_por_slot() {
        val v = vm()
        val prenda = UUID.randomUUID()
        v.seleccionar(orden = 2, prendaId = prenda)
        assertEquals(prenda, v.seleccionDe(2))
        assertEquals(null, v.seleccionDe(99)) // slot sin elegir
    }

    @Test
    fun agregar_sin_sucursal_avisa_y_no_llama_al_backend() {
        val v = vm(sucursales = emptyList()) // no hay sucursal -> ninguna seleccionada
        val e = eventos(v)
        v.agregarAlCarrito(AgregarItemRequest.Tipo.VENTA, cantidad = 1, retiro = null, devolucion = null)
        assertTrue(hayError(e, "sucursal"))
        coVerify(exactly = 0) { pedidos.agregarDisfrazAlCarrito(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun agregar_renta_sin_fechas_avisa() {
        val v = vm(sucursales = listOf(sucursal())) // sucursal seleccionada por defecto
        val e = eventos(v)
        v.agregarAlCarrito(AgregarItemRequest.Tipo.RENTA, cantidad = 1, retiro = null, devolucion = null)
        assertTrue(hayError(e, "fechas"))
        coVerify(exactly = 0) { pedidos.agregarDisfrazAlCarrito(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun agregar_venta_con_sucursal_llama_al_backend() {
        coEvery {
            pedidos.agregarDisfrazAlCarrito(any(), any(), any(), any(), any(), any(), any(), any())
        } returns RespuestaRed.Exito(CarritoResponse())
        val v = vm(sucursales = listOf(sucursal()))
        val e = eventos(v)
        v.agregarAlCarrito(AgregarItemRequest.Tipo.VENTA, cantidad = 1, retiro = null, devolucion = null)
        coVerify(exactly = 1) { pedidos.agregarDisfrazAlCarrito(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertTrue(e.any { it is EventoDisfraz.Agregado })
    }
}
