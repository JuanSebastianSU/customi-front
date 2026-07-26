package com.costumi.app.ui.gestion.caja

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CajaRepository
import com.costumi.apiclient.models.TurnoResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

/**
 * Turnos de caja: el turno ABIERTO se muestra arriba (el backend no ordena), y abrir un turno recarga la
 * lista. Que el turno vigente quede arriba importa: es el que el cajero usa.
 */
class CajaViewModelTest {

    private lateinit var repo: CajaRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
        coEvery { repo.turnos() } returns RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "x"))
    }

    @After fun despues() = Dispatchers.resetMain()

    private fun eventos(vm: CajaViewModel): MutableList<EventoCaja> {
        val out = mutableListOf<EventoCaja>()
        CoroutineScope(Dispatchers.Main).launch { vm.eventos.collect { out.add(it) } }
        return out
    }

    @Test
    fun sin_turnos_es_empty() {
        coEvery { repo.turnos() } returns RespuestaRed.Exito(emptyList())
        val vm = CajaViewModel(repo)
        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun ordena_el_turno_abierto_arriba() {
        val cerrado = TurnoResponse(id = UUID.randomUUID(), estado = "CERRADO")
        val abierto = TurnoResponse(id = UUID.randomUUID(), estado = "ABIERTO")
        coEvery { repo.turnos() } returns RespuestaRed.Exito(listOf(cerrado, abierto)) // abierto llega segundo
        val vm = CajaViewModel(repo)
        val visibles = (vm.estado.value as UiState.Success).data
        assertEquals("ABIERTO", visibles.first().estado) // pero se muestra primero
    }

    @Test
    fun error_de_red_es_error() {
        coEvery { repo.turnos() } returns RespuestaRed.Fallo(ErrorApi(TipoError.SERVIDOR, "boom"))
        val vm = CajaViewModel(repo)
        assertTrue(vm.estado.value is UiState.Error)
    }

    @Test
    fun abrir_ok_emite_info() {
        coEvery { repo.abrir(any(), any()) } returns RespuestaRed.Exito(TurnoResponse(id = UUID.randomUUID(), estado = "ABIERTO"))
        val vm = CajaViewModel(repo)
        val e = eventos(vm)
        vm.abrir(UUID.randomUUID(), BigDecimal("100.00"))
        assertTrue(e.any { it is EventoCaja.Info })
    }

    @Test
    fun abrir_fallo_emite_error() {
        coEvery { repo.abrir(any(), any()) } returns RespuestaRed.Fallo(ErrorApi(TipoError.CONFLICTO, "ya hay un turno abierto"))
        val vm = CajaViewModel(repo)
        val e = eventos(vm)
        vm.abrir(UUID.randomUUID(), BigDecimal("100.00"))
        assertTrue(e.any { it is EventoCaja.Error })
    }
}
