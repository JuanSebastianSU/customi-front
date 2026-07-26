package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.PedidoDao
import com.costumi.app.data.local.entity.PedidoEntity
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.apis.ReembolsoControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.HistorialItem
import com.costumi.apiclient.models.LineaDeHistorial
import com.costumi.apiclient.models.RespuestaPaginadaHistorialItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.UUID

/**
 * Tests de la caché cache-first del historial "Mis pedidos" (Room, `PLAN_ROOM_OFFLINE.md` A4):
 * - con red OK, [CuentaRepository.refrescarHistorial] **escribe en Room** conservando el orden y
 *   descartando pedidos sin `operacionId`.
 * - con red caída, devuelve el error y **NO toca la caché**.
 * - [CuentaRepository.observarHistorial] reconstruye el `HistorialItem` completo **con sus líneas** desde JSON.
 */
class CuentaRepositoryTest {

    private val gson = Serializer.gsonBuilder.create()

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(api: ClienteControllerApi, dao: PedidoDao) = CuentaRepository(
        api,
        mockk<EmpresaControllerApi>(relaxed = true),
        mockk<ReembolsoControllerApi>(relaxed = true),
        dao,
        gson,
        dispatchers,
    )

    private fun pedido(operacionId: UUID?, tienda: String) = HistorialItem(
        operacionId = operacionId,
        empresaNombre = tienda,
        lineas = listOf(LineaDeHistorial(nombre = "Capa", cantidad = 1)),
    )

    @Test
    fun refrescar_con_red_ok_escribe_en_room_con_orden_y_descarta_sin_operacionId() = runTest {
        val api = mockk<ClienteControllerApi>()
        val dao = mockk<PedidoDao>(relaxed = true)
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        coEvery { api.miHistorial(any(), any(), any()) } returns Response.success(
            RespuestaPaginadaHistorialItem(contenido = listOf(pedido(a, "Tienda A"), pedido(null, "Sin id"), pedido(b, "Tienda B"))),
        )

        val r = repo(api, dao).refrescarHistorial()

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) {
            dao.reemplazar(
                match {
                    it.size == 2 && // el pedido sin operacionId (índice 1) se descartó
                        it[0].operacionId == a.toString() && it[0].orden == 0 &&
                        // orden conserva el índice original del servidor: el hueco (2) es inofensivo para ORDER BY
                        it[1].operacionId == b.toString() && it[1].orden == 2 &&
                        it[0].json.contains("Tienda A")
                },
            )
        }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error_y_no_toca_room() = runTest {
        val api = mockk<ClienteControllerApi>()
        val dao = mockk<PedidoDao>(relaxed = true)
        coEvery { api.miHistorial(any(), any(), any()) } throws IOException("sin red")

        val r = repo(api, dao).refrescarHistorial()

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.reemplazar(any()) }
    }

    @Test
    fun observar_reconstruye_el_pedido_completo_con_sus_lineas() = runTest {
        val api = mockk<ClienteControllerApi>()
        val dao = mockk<PedidoDao>(relaxed = true)
        val id = UUID.randomUUID()
        val json = gson.toJson(pedido(id, "Tienda A"))
        every { dao.observarTodos() } returns flowOf(listOf(PedidoEntity(operacionId = id.toString(), orden = 0, json = json)))

        val pedidos = repo(api, dao).observarHistorial().first()

        assertEquals(1, pedidos.size)
        assertEquals("Tienda A", pedidos[0].empresaNombre)
        assertEquals(1, pedidos[0].lineas?.size)
        assertEquals("Capa", pedidos[0].lineas?.first()?.nombre)
    }
}
