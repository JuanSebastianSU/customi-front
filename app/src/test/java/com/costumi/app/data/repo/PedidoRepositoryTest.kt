package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.MisCarritosApi
import com.costumi.apiclient.apis.CarritoControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.EditarCantidadRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.util.UUID

/**
 * El repositorio de pedidos valida los ids ANTES de tocar la red: un id mal formado devuelve [RespuestaRed.Fallo]
 * y **no** llama al API. Importa porque estos ids vienen de argumentos de navegación/deep-links: sin la
 * guarda, un id corrupto reventaría en `UUID.fromString` o mandaría basura al backend.
 */
class PedidoRepositoryTest {

    private val gson = Serializer.gsonBuilder.create()
    private val idOk = UUID.randomUUID().toString()
    private val idMalo = "no-es-uuid"

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private val carritoApi = mockk<CarritoControllerApi>(relaxed = true)
    private fun repo() = PedidoRepository(
        carritoApi,
        mockk<MarketplaceControllerApi>(relaxed = true),
        mockk<MisCarritosApi>(relaxed = true),
        gson,
        dispatchers,
    )

    @Test
    fun agregar_al_carrito_con_ids_invalidos_no_llama_al_api() = runTest {
        val r = repo().agregarAlCarrito(
            empresaId = idMalo, sucursalId = idMalo, prendaId = idMalo,
            tipo = AgregarItemRequest.Tipo.entries.first(), cantidad = 1,
            fechaRetiro = null, fechaDevolucion = null,
        )
        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { carritoApi.agregarItem(any()) }
    }

    @Test
    fun agregar_al_carrito_con_ids_validos_si_llama_al_api() = runTest {
        coEvery { carritoApi.agregarItem(any()) } returns Response.success(CarritoResponse())
        val r = repo().agregarAlCarrito(
            empresaId = idOk, sucursalId = idOk, prendaId = idOk,
            tipo = AgregarItemRequest.Tipo.entries.first(), cantidad = 1,
            fechaRetiro = null, fechaDevolucion = null,
        )
        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) { carritoApi.agregarItem(any()) }
    }

    @Test
    fun checkout_venta_con_sucursal_invalida_es_fallo() = runTest {
        val r = repo().checkoutVenta(empresaId = idOk, sucursalId = idMalo)
        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { carritoApi.checkout(any()) }
    }

    @Test
    fun quitar_del_carrito_con_linea_invalida_es_fallo() = runTest {
        val r = repo().quitarDelCarrito(
            lineaId = idMalo, sucursalId = idOk,
            tipo = CarritoControllerApi.TipoQuitarItem.entries.first(), empresaId = idOk,
        )
        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { carritoApi.quitarItem(any(), any(), any(), any()) }
    }

    @Test
    fun editar_cantidad_con_linea_invalida_es_fallo() = runTest {
        val r = repo().editarCantidad(
            lineaId = idMalo, cantidad = 2, sucursalId = idOk,
            tipo = EditarCantidadRequest.Tipo.entries.first(), empresaId = idOk,
        )
        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { carritoApi.editarCantidad(any(), any()) }
    }

    @Test
    fun carrito_pendiente_con_sucursal_invalida_es_fallo() = runTest {
        val r = repo().carritoPendiente(
            sucursalId = idMalo,
            tipo = CarritoControllerApi.TipoPendiente.entries.first(), empresaId = idOk,
        )
        assertTrue(r is RespuestaRed.Fallo)
    }
}
