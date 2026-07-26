package com.costumi.app.ui.gestion.config

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.ConfiguracionRepository
import com.costumi.apiclient.infrastructure.Serializer
import com.costumi.apiclient.models.ConfiguracionResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Configuración de la empresa: al importar un respaldo, valida que el archivo sea un JSON parseable ANTES de
 * mandarlo al backend; si no, avisa y no llama. Evita restaurar la tienda desde un archivo corrupto.
 */
class ConfiguracionViewModelTest {

    private val gson = Serializer.gsonBuilder.create()
    private lateinit var repo: ConfiguracionRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        // init -> cargar() -> obtener(); se stubbea para que el `when` no reciba un mock de RespuestaRed.
        coEvery { repo.obtener() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun vm() = ConfiguracionViewModel(repo, gson)

    private fun eventos(vm: ConfiguracionViewModel): MutableList<EventoConfig> {
        val out = mutableListOf<EventoConfig>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun importar_json_invalido_avisa_y_no_llama_al_backend() {
        val vm = vm()
        val e = eventos(vm)
        vm.importar("esto no es json {{{")
        assertTrue(e.any { it is EventoConfig.Error && it.mensaje.contains("respaldo valido") })
        coVerify(exactly = 0) { repo.importar(any()) }
    }

    @Test
    fun importar_json_valido_llama_al_backend() {
        coEvery { repo.importar(any()) } returns RespuestaRed.Exito(ConfiguracionResponse())
        val vm = vm()
        val e = eventos(vm)
        vm.importar("{}")
        coVerify(exactly = 1) { repo.importar(any()) }
        assertTrue(e.any { it is EventoConfig.Importada })
    }

    @Test
    fun exportar_emite_el_json_del_respaldo() {
        coEvery { repo.exportar() } returns RespuestaRed.Exito(ConfiguracionResponse(multasActivo = true))
        val vm = vm()
        val e = eventos(vm)
        vm.exportar()
        assertTrue(e.any { it is EventoConfig.Exportada && it.json.contains("multasActivo") })
    }

    @Test
    fun guardar_ok_emite_guardada() {
        coEvery { repo.actualizar(any()) } returns RespuestaRed.Exito(ConfiguracionResponse())
        val vm = vm()
        val e = eventos(vm)
        vm.guardar(com.costumi.apiclient.models.ConfiguracionRequest())
        assertTrue(e.any { it is EventoConfig.Guardada })
        assertTrue(vm.estado.value is com.costumi.app.core.UiState.Success)
    }
}
