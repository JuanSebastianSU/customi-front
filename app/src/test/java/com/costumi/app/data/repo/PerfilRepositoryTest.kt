package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.local.dao.PerfilDao
import com.costumi.app.data.local.entity.PerfilEntity
import com.costumi.app.data.remote.FotoPerfilApi
import com.costumi.apiclient.apis.PerfilControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.PerfilResponse
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

/**
 * Tests de la caché del perfil (Room, `PLAN_ROOM_OFFLINE.md` A6):
 * - [PerfilRepository.refrescarPerfil] con red OK **escribe en Room**; con red caída devuelve error y no toca Room.
 * - [PerfilRepository.observarPerfil] reconstruye el `PerfilResponse` desde el JSON guardado.
 */
class PerfilRepositoryTest {

    private val gson = Serializer.gsonBuilder.create()

    private val dispatchers = object : DispatcherProvider {
        private val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        override val main = d
        override val io = d
        override val default = d
    }

    private fun repo(api: PerfilControllerApi, dao: PerfilDao) =
        PerfilRepository(api, mockk<FotoPerfilApi>(relaxed = true), dao, gson, dispatchers)

    private val perfil = PerfilResponse(email = "juan@x.com", nombre = "Juan", telefono = "099")

    @Test
    fun refrescar_con_red_ok_escribe_en_room() = runTest {
        val api = mockk<PerfilControllerApi>()
        val dao = mockk<PerfilDao>(relaxed = true)
        coEvery { api.ver() } returns Response.success(perfil)

        val r = repo(api, dao).refrescarPerfil()

        assertTrue(r is RespuestaRed.Exito)
        coVerify(exactly = 1) { dao.guardar(match { it.json.contains("juan@x.com") && it.json.contains("Juan") }) }
    }

    @Test
    fun refrescar_con_red_caida_devuelve_error_y_no_toca_room() = runTest {
        val api = mockk<PerfilControllerApi>()
        val dao = mockk<PerfilDao>(relaxed = true)
        coEvery { api.ver() } throws IOException("sin red")

        val r = repo(api, dao).refrescarPerfil()

        assertTrue(r is RespuestaRed.Fallo)
        coVerify(exactly = 0) { dao.guardar(any()) }
    }

    @Test
    fun observar_reconstruye_el_perfil_desde_el_json() = runTest {
        val api = mockk<PerfilControllerApi>()
        val dao = mockk<PerfilDao>(relaxed = true)
        every { dao.observar() } returns flowOf(PerfilEntity(json = gson.toJson(perfil)))

        val p = repo(api, dao).observarPerfil().first()

        assertEquals("juan@x.com", p?.email)
        assertEquals("Juan", p?.nombre)
    }
}
