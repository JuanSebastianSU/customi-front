package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.DisfrazVitrinaDao
import com.costumi.app.data.local.dao.EmpresaDao
import com.costumi.app.data.local.dao.PrendaVitrinaDao
import com.costumi.app.data.local.entity.DisfrazVitrinaEntity
import com.costumi.app.data.local.entity.PrendaVitrinaEntity
import com.costumi.apiclient.apis.DisfrazControllerApi
import com.costumi.apiclient.apis.DisfrazMarketplaceControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.EtiquetaVitrinaDto
import com.costumi.apiclient.models.PrendaVitrinaResponse
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID

/**
 * Tests de la caché cache-first del catálogo de prendas (Room, `PLAN_ROOM_OFFLINE.md` A1). Cubre lo que
 * puede romperse ahí:
 * - con red OK, [MarketplaceRepository.refrescarCatalogo] **escribe en Room** con el `empresaId` del
 *   contexto (el DTO no lo trae) y preserva precios y etiquetas.
 * - con red caída, devuelve el error y **NO toca la caché**.
 * - [MarketplaceRepository.observarCatalogo] reconstruye `BigDecimal` y las etiquetas desde la entidad.
 * No necesita dispositivo: DAO y APIs son dobles (mockk).
 */
class MarketplaceRepositoryTest {

    private val empresaId = UUID.randomUUID().toString()
    private val prendaId = UUID.randomUUID()
    private val gson = Serializer.gsonBuilder.create()

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(
        api: MarketplaceControllerApi = mockk(),
        prendaDao: PrendaVitrinaDao = mockk(relaxed = true),
        disfrazApi: DisfrazMarketplaceControllerApi = mockk(),
        disfrazDao: DisfrazVitrinaDao = mockk(relaxed = true),
    ) = MarketplaceRepository(
        api,
        disfrazApi,
        mockk<DisfrazControllerApi>(relaxed = true),
        mockk<EmpresaDao>(relaxed = true),
        prendaDao,
        disfrazDao,
        gson,
        dispatchers,
    )

    private fun prendaResponse() = PrendaVitrinaResponse(
        id = prendaId,
        nombre = "Capa",
        tipoArticulo = "PRENDA",
        precioRenta = BigDecimal("10.50"),
        precioVenta = null,
        categoria = "Superheroes",
        fotoUrl = null,
        etiquetas = listOf(EtiquetaVitrinaDto(tipo = "Color", valor = "Rojo")),
    )

    @Test
    fun refrescar_con_red_ok_escribe_en_room_con_empresa_precio_y_etiquetas() = runTest {
        val api = mockk<MarketplaceControllerApi>()
        val dao = mockk<PrendaVitrinaDao>(relaxed = true)
        coEvery { api.catalogo1(any(), any()) } returns Response.success(listOf(prendaResponse()))

        val r = repo(api, dao).refrescarCatalogo(empresaId)

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) {
            dao.reemplazarDeEmpresa(
                empresaId,
                match {
                    it.size == 1 &&
                        it[0].id == prendaId.toString() &&
                        it[0].empresaId == empresaId &&
                        it[0].precioRenta == "10.50" &&
                        it[0].etiquetasJson?.contains("Rojo") == true
                },
            )
        }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error_y_no_toca_room() = runTest {
        val api = mockk<MarketplaceControllerApi>()
        val dao = mockk<PrendaVitrinaDao>(relaxed = true)
        coEvery { api.catalogo1(any(), any()) } throws IOException("sin red")

        val r = repo(api, dao).refrescarCatalogo(empresaId)

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.reemplazarDeEmpresa(any(), any()) }
    }

    @Test
    fun observar_reconstruye_precio_y_etiquetas_desde_la_entidad() = runTest {
        val api = mockk<MarketplaceControllerApi>()
        val dao = mockk<PrendaVitrinaDao>(relaxed = true)
        val entidad = PrendaVitrinaEntity(
            id = prendaId.toString(),
            empresaId = empresaId,
            nombre = "Capa",
            tipoArticulo = "PRENDA",
            precioRenta = "10.50",
            precioVenta = null,
            categoria = "Superheroes",
            fotoUrl = null,
            etiquetasJson = gson.toJson(listOf(EtiquetaVitrinaDto(tipo = "Color", valor = "Rojo"))),
        )
        every { dao.observarDeEmpresa(empresaId) } returns flowOf(listOf(entidad))

        val prendas = repo(api, dao).observarCatalogo(empresaId).first()

        assertEquals(1, prendas.size)
        assertEquals(BigDecimal("10.50"), prendas[0].precioRenta)
        assertNull(prendas[0].precioVenta)
        assertEquals("Color", prendas[0].etiquetas?.first()?.tipo)
        assertEquals("Rojo", prendas[0].etiquetas?.first()?.valor)
    }

    // --- Disfraces (A2): mismo patrón cache-first; se cachea el conteo de piezas, no los slots ---

    private fun disfrazResponse() = DisfrazResponse(
        id = prendaId,
        empresaId = UUID.fromString(empresaId),
        nombre = "Pirata",
        categoria = "Aventura",
        tipo = DisfrazResponse.Tipo.AMBOS,
        precioRentaGeneral = BigDecimal("20.00"),
        precioVentaSugerido = BigDecimal("80.00"),
        fotoUrl = null,
        slots = listOf(
            com.costumi.apiclient.models.SlotDto(ejePrenda = com.costumi.apiclient.models.SlotDto.EjePrenda.FIJA),
            com.costumi.apiclient.models.SlotDto(ejePrenda = com.costumi.apiclient.models.SlotDto.EjePrenda.PERSONALIZABLE),
        ),
    )

    @Test
    fun refrescar_disfraces_con_red_ok_escribe_tipo_precio_y_conteo_de_piezas() = runTest {
        val disfrazApi = mockk<DisfrazMarketplaceControllerApi>()
        val disfrazDao = mockk<DisfrazVitrinaDao>(relaxed = true)
        coEvery { disfrazApi.listar18(any()) } returns Response.success(listOf(disfrazResponse()))

        val r = repo(disfrazApi = disfrazApi, disfrazDao = disfrazDao).refrescarDisfraces(empresaId)

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) {
            disfrazDao.reemplazarDeEmpresa(
                empresaId,
                match {
                    it.size == 1 &&
                        it[0].id == prendaId.toString() &&
                        it[0].empresaId == empresaId &&
                        it[0].tipo == "AMBOS" &&
                        it[0].precioRentaGeneral == "20.00" &&
                        it[0].piezas == 2 // se guarda el conteo, no los slots
                },
            )
        }
    }

    @Test
    fun refrescar_disfraces_con_red_caida_no_toca_room() = runTest {
        val disfrazApi = mockk<DisfrazMarketplaceControllerApi>()
        val disfrazDao = mockk<DisfrazVitrinaDao>(relaxed = true)
        coEvery { disfrazApi.listar18(any()) } throws IOException("sin red")

        val r = repo(disfrazApi = disfrazApi, disfrazDao = disfrazDao).refrescarDisfraces(empresaId)

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { disfrazDao.reemplazarDeEmpresa(any(), any()) }
    }

    @Test
    fun observar_disfraces_reconstruye_tipo_precio_y_las_piezas_desde_el_conteo() = runTest {
        val disfrazDao = mockk<DisfrazVitrinaDao>(relaxed = true)
        val entidad = DisfrazVitrinaEntity(
            id = prendaId.toString(),
            empresaId = empresaId,
            nombre = "Pirata",
            categoria = "Aventura",
            tipo = "AMBOS",
            precioRentaGeneral = "20.00",
            precioRentaSugerido = null,
            precioVentaGeneral = null,
            precioVentaSugerido = "80.00",
            fotoUrl = null,
            piezas = 2,
        )
        every { disfrazDao.observarDeEmpresa(empresaId) } returns flowOf(listOf(entidad))

        val disfraces = repo(disfrazDao = disfrazDao).observarDisfraces(empresaId).first()

        assertEquals(1, disfraces.size)
        assertEquals(DisfrazResponse.Tipo.AMBOS, disfraces[0].tipo)
        assertEquals(BigDecimal("20.00"), disfraces[0].precioRentaGeneral)
        // La vitrina lee `slots.size`: debe reflejar el conteo cacheado aunque no haya slots reales.
        assertEquals(2, disfraces[0].slots?.size)
    }
}
