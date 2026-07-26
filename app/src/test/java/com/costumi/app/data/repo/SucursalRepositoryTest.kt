package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.SucursalDao
import com.costumi.app.data.remote.FotoSucursalApi
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.UsuarioActualResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.UUID

/**
 * Tests de la caché cache-first de sucursales (Room), lo más importante que puede romperse ahí:
 * - con red OK, [SucursalRepository.refrescarSucursales] **escribe en Room** lo que devolvió la API.
 * - con red caída, devuelve el error y **NO toca la caché** (no la borra por una falla de red).
 * Sigue el §7 de `PLAN_ROOM_OFFLINE.md`. No necesita dispositivo: DAO y APIs son dobles (mockk).
 */
class SucursalRepositoryTest {

    private val empresaId = UUID.randomUUID()
    private val sucursalId = UUID.randomUUID()
    private val gson = Serializer.gsonBuilder.create()

    // Todo corre en el mismo dispatcher de test: sin hilos reales, determinista.
    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(sucursalApi: SucursalControllerApi, authApi: AuthControllerApi, dao: SucursalDao) =
        SucursalRepository(sucursalApi, mockk<FotoSucursalApi>(relaxed = true), authApi, dao, gson, dispatchers)

    @Test
    fun refrescar_con_red_ok_escribe_en_room() = runTest {
        val authApi = mockk<AuthControllerApi>()
        val sucursalApi = mockk<SucursalControllerApi>()
        val dao = mockk<SucursalDao>(relaxed = true)
        coEvery { authApi.me() } returns Response.success(UsuarioActualResponse(empresaId = empresaId.toString(), rol = "DUENO"))
        coEvery { sucursalApi.listar9(empresaId) } returns Response.success(
            listOf(SucursalResponse(id = sucursalId, nombre = "Centro", archivada = false)),
        )

        val r = repo(sucursalApi, authApi, dao).refrescarSucursales()

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) { dao.reemplazar(match { it.size == 1 && it[0].id == sucursalId.toString() }) }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error_y_no_toca_room() = runTest {
        val authApi = mockk<AuthControllerApi>()
        val sucursalApi = mockk<SucursalControllerApi>()
        val dao = mockk<SucursalDao>(relaxed = true)
        coEvery { authApi.me() } returns Response.success(UsuarioActualResponse(empresaId = empresaId.toString(), rol = "DUENO"))
        coEvery { sucursalApi.listar9(empresaId) } throws IOException("sin red")

        val r = repo(sucursalApi, authApi, dao).refrescarSucursales()

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.reemplazar(any()) }
    }
}
