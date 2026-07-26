package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.MiEmpresaDao
import com.costumi.app.data.local.entity.MiEmpresaEntity
import com.costumi.app.data.remote.FotoEmpresaApi
import com.costumi.app.data.remote.MiEmpresaApi
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.EmpresaResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Tests de la caché de la propia tienda (Room, `PLAN_ROOM_OFFLINE.md` A6). Antes era caché en memoria; lo
 * que puede romperse al pasarla a Room:
 * - [MiEmpresaRepository.mia] es **cache-first**: si hay tienda en Room la devuelve **sin llamar a la red**.
 * - con Room vacío, pide a la red y **guarda** (round-trip JSON preserva los campos).
 * - una caída de red no rompe (devuelve el error).
 */
class MiEmpresaRepositoryTest {

    private val gson = Serializer.gsonBuilder.create()

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(api: MiEmpresaApi, dao: MiEmpresaDao) = MiEmpresaRepository(
        api,
        mockk<EmpresaControllerApi>(relaxed = true),
        mockk<FotoEmpresaApi>(relaxed = true),
        dao,
        gson,
        dispatchers,
    )

    private val tienda = EmpresaResponse(nombre = "Fiesta & Fantasia", ciudad = "Cuenca", logoUrl = "http://x/logo.png")

    @Test
    fun mia_con_cache_en_room_no_llama_a_la_red() = runTest {
        val api = mockk<MiEmpresaApi>() // sin stub: si se llamara, el test fallaría
        val dao = mockk<MiEmpresaDao>(relaxed = true)
        coEvery { dao.leer() } returns MiEmpresaEntity(json = gson.toJson(tienda))

        val r = repo(api, dao).mia()

        assertTrue(r is RespuestaRed.Exito)
        assertEquals("Fiesta & Fantasia", (r as RespuestaRed.Exito).data.nombre)
        coVerify(exactly = 0) { api.mia() }
    }

    @Test
    fun mia_sin_cache_pide_a_la_red_y_guarda_en_room() = runTest {
        val api = mockk<MiEmpresaApi>()
        val dao = mockk<MiEmpresaDao>(relaxed = true)
        coEvery { dao.leer() } returns null
        coEvery { api.mia() } returns Response.success(tienda)

        val r = repo(api, dao).mia()

        assertTrue(r is RespuestaRed.Exito)
        // Gson escapa el '&' a &, así que se verifica con substrings sin caracteres especiales.
        coVerify(exactly = 1) { dao.guardar(match { it.json.contains("Fiesta") && it.json.contains("logo.png") }) }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error() = runTest {
        val api = mockk<MiEmpresaApi>()
        val dao = mockk<MiEmpresaDao>(relaxed = true)
        coEvery { api.mia() } throws IOException("sin red")

        val r = repo(api, dao).refrescar()

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.guardar(any()) }
    }
}
