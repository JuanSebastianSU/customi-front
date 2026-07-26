package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.DeudaDao
import com.costumi.app.data.local.entity.DeudaEntity
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.data.remote.MisDeudasApi
import com.costumi.apiclient.infrastructure.Serializer
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Tests de la caché cache-first de multas/saldos (Room, `PLAN_ROOM_OFFLINE.md` A5):
 * - con red OK, [MisDeudasRepository.refrescarDeudas] **escribe en Room** conservando el orden y descartando
 *   las deudas sin `rentaId` (sin clave no se pueden cachear).
 * - con red caída, devuelve el error y **NO toca la caché**.
 * - [MisDeudasRepository.observarDeudas] reconstruye el DTO completo (saldo `BigDecimal`, fechas `LocalDate`).
 */
class MisDeudasRepositoryTest {

    private val gson = Serializer.gsonBuilder.create()

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(api: MisDeudasApi, dao: DeudaDao) = MisDeudasRepository(api, dao, gson, dispatchers)

    private fun deuda(rentaId: UUID?, tienda: String, saldo: String?) = MiDeudaDto(
        empresaId = UUID.randomUUID(), empresaNombre = tienda, rentaId = rentaId, codigoRetiro = "R-1",
        estado = "PENDIENTE", fechaRetiro = LocalDate.of(2026, 1, 5), fechaDevolucion = LocalDate.of(2026, 1, 9),
        importe = BigDecimal("40.00"), cargoPorDanos = null, cargoPorRetraso = null, deposito = null,
        multa = null, pagado = BigDecimal.ZERO, saldo = saldo?.let { BigDecimal(it) },
    )

    @Test
    fun refrescar_con_red_ok_escribe_en_room_con_orden_y_descarta_sin_rentaId() = runTest {
        val api = mockk<MisDeudasApi>()
        val dao = mockk<DeudaDao>(relaxed = true)
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        coEvery { api.mias() } returns Response.success(
            listOf(deuda(a, "Tienda A", "12.50"), deuda(b, "Tienda B", "0"), deuda(null, "Sin renta", "5")),
        )

        val r = repo(api, dao).refrescarDeudas()

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) {
            dao.reemplazar(
                match {
                    it.size == 2 && // la deuda sin rentaId se descartó
                        it[0].rentaId == a.toString() && it[0].orden == 0 &&
                        it[1].rentaId == b.toString() && it[1].orden == 1 &&
                        it[0].json.contains("Tienda A")
                },
            )
        }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error_y_no_toca_room() = runTest {
        val api = mockk<MisDeudasApi>()
        val dao = mockk<DeudaDao>(relaxed = true)
        coEvery { api.mias() } throws IOException("sin red")

        val r = repo(api, dao).refrescarDeudas()

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.reemplazar(any()) }
    }

    @Test
    fun observar_reconstruye_saldo_y_fechas_desde_el_json() = runTest {
        val api = mockk<MisDeudasApi>()
        val dao = mockk<DeudaDao>(relaxed = true)
        val id = UUID.randomUUID()
        val json = gson.toJson(deuda(id, "Tienda A", "12.50"))
        every { dao.observarTodas() } returns flowOf(listOf(DeudaEntity(rentaId = id.toString(), orden = 0, json = json)))

        val deudas = repo(api, dao).observarDeudas().first()

        assertEquals(1, deudas.size)
        assertEquals(BigDecimal("12.50"), deudas[0].saldo)
        assertEquals(LocalDate.of(2026, 1, 5), deudas[0].fechaRetiro)
        assertEquals("Tienda A", deudas[0].empresaNombre)
    }
}
