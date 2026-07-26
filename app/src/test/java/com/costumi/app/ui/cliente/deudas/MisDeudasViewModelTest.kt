package com.costumi.app.ui.cliente.deudas

import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.data.repo.MisDeudasRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

/**
 * Tests del aviso N4 "datos guardados" (`PLAN_ROOM_OFFLINE.md` B3): el ViewModel prende [sinConexion] SOLO
 * cuando muestra caché (Success) y el refresco falló **por falta de red**; no lo prende si el refresco va
 * bien, ni si no hay caché (ahí va error a pantalla completa).
 */
class MisDeudasViewModelTest {

    @Before fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun despues() = Dispatchers.resetMain()

    private fun unaDeuda() = MiDeudaDto(
        empresaId = UUID.randomUUID(), empresaNombre = "Tienda", rentaId = UUID.randomUUID(),
        codigoRetiro = "R-1", estado = "PENDIENTE", fechaRetiro = null, fechaDevolucion = null,
        importe = null, cargoPorDanos = null, cargoPorRetraso = null, deposito = null, multa = null,
        pagado = null, saldo = BigDecimal("10.00"),
    )

    private fun repoCon(
        cache: List<MiDeudaDto>,
        refresco: RespuestaRed<Int>,
    ): MisDeudasRepository = mockk {
        every { observarDeudas() } returns flowOf(cache)
        coEvery { refrescarDeudas() } returns refresco
    }

    @Test
    fun con_cache_y_red_caida_prende_el_aviso() {
        val repo = repoCon(
            cache = listOf(unaDeuda()),
            refresco = RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red")),
        )
        val vm = MisDeudasViewModel(repo)

        assertTrue(vm.sinConexion.value)
        assertTrue(vm.estado.value is UiState.Success) // la caché se sigue mostrando
    }

    @Test
    fun con_refresco_ok_no_prende_el_aviso() {
        val repo = repoCon(cache = listOf(unaDeuda()), refresco = RespuestaRed.Exito(1))
        val vm = MisDeudasViewModel(repo)

        assertFalse(vm.sinConexion.value)
    }

    @Test
    fun sin_cache_y_refresco_vacio_muestra_empty_no_se_queda_cargando() {
        // Regresión: Room no re-emite si la tabla ya estaba vacía; sin el conteo, quedaba en Loading para siempre.
        val repo = repoCon(cache = emptyList(), refresco = RespuestaRed.Exito(0))
        val vm = MisDeudasViewModel(repo)

        assertTrue(vm.estado.value is UiState.Empty)
    }

    @Test
    fun sin_cache_y_red_caida_no_prende_el_aviso_sino_error() {
        val repo = repoCon(
            cache = emptyList(),
            refresco = RespuestaRed.Fallo(ErrorApi(TipoError.SIN_CONEXION, "sin red")),
        )
        val vm = MisDeudasViewModel(repo)

        assertFalse(vm.sinConexion.value) // sin caché no hay "datos guardados" que avisar
        assertTrue(vm.estado.value is UiState.Error)
    }
}
