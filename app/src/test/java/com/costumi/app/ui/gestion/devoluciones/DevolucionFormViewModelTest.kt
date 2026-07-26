package com.costumi.app.ui.gestion.devoluciones

import androidx.lifecycle.SavedStateHandle
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.data.repo.ConfiguracionRepository
import com.costumi.app.data.repo.DevolucionRepository
import com.costumi.apiclient.models.ConfiguracionResponse
import com.costumi.apiclient.models.PrendaResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * La cuenta del dinero al devolver una renta: liquidación del depósito (depósito − cargos) y el recargo por
 * retraso según la política de la tienda. Es la MISMA cuenta que hace el servidor; se muestra antes de
 * confirmar, así que si se desvía, el empleado le dice al cliente un número equivocado en el mostrador.
 */
class DevolucionFormViewModelTest {

    // Claves de navegación (coinciden con DevolucionFormFragment.ARG_*); se ponen literales para no cargar el Fragment.
    private val argFechaDev = "fechaDevolucion"
    private val argPrendaIds = "prendaIds"

    @Before fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun despues() = Dispatchers.resetMain()

    // ---- Liquidación (lógica pura del data class) ----

    private fun liquidacion(deposito: String, danos: String, retraso: String, multasApagadas: Boolean = false) =
        Liquidacion(
            deposito = BigDecimal(deposito), danos = BigDecimal(danos), retraso = BigDecimal(retraso),
            diasRetraso = 0, retrasoManual = false, multasApagadas = multasApagadas,
        )

    @Test
    fun cargos_es_danos_mas_retraso_y_saldo_es_deposito_menos_cargos() {
        val l = liquidacion(deposito = "50.00", danos = "30.00", retraso = "5.00")
        assertEquals(BigDecimal("35.00"), l.cargos)
        assertEquals(BigDecimal("15.00"), l.saldo) // 50 - 35, le sobra al cliente
    }

    @Test
    fun saldo_negativo_cuando_los_cargos_superan_el_deposito() {
        val l = liquidacion(deposito = "50.00", danos = "70.00", retraso = "0.00")
        assertEquals(BigDecimal("-20.00"), l.saldo) // el cliente queda debiendo
    }

    @Test
    fun con_multas_apagadas_no_hay_cargos_y_se_devuelve_todo_el_deposito() {
        val l = liquidacion(deposito = "50.00", danos = "70.00", retraso = "10.00", multasApagadas = true)
        assertEquals(BigDecimal.ZERO, l.cargos)
        assertEquals(BigDecimal("50.00"), l.saldo)
    }

    // ---- Recargo por retraso (lógica del VM contra la política de la tienda) ----

    private fun vm(
        recargoPorDia: String?,
        modo: ConfiguracionResponse.ModoRecargoRetraso?,
        pactada: String? = "2026-01-10",
    ): DevolucionFormViewModel {
        val handle = SavedStateHandle(
            buildMap<String, Any> {
                pactada?.let { put(argFechaDev, it) }
                put(argPrendaIds, arrayListOf<String>())
            },
        )
        val config = ConfiguracionResponse(
            multasActivo = true,
            recargoPorRetrasoPorDia = recargoPorDia?.let { BigDecimal(it) },
            modoRecargoRetraso = modo,
        )
        val configRepo = mockk<ConfiguracionRepository> { coEvery { obtener() } returns RespuestaRed.Exito(config) }
        val devRepo = mockk<DevolucionRepository> {
            coEvery { prendas() } returns RespuestaRed.Exito(emptyList<PrendaResponse>())
        }
        return DevolucionFormViewModel(devRepo, configRepo, handle)
    }

    @Test
    fun dias_de_retraso_cuenta_desde_la_fecha_pactada_y_no_es_negativo() {
        val v = vm(recargoPorDia = "2.00", modo = ConfiguracionResponse.ModoRecargoRetraso.ACUMULATIVA)
        assertEquals(3, v.diasDeRetraso(LocalDate.of(2026, 1, 13)))
        assertEquals(0, v.diasDeRetraso(LocalDate.of(2026, 1, 10))) // en fecha
        assertEquals(0, v.diasDeRetraso(LocalDate.of(2026, 1, 5)))  // antes: no cuenta hacia atrás
    }

    @Test
    fun recargo_acumulativo_multiplica_por_dia() {
        val v = vm(recargoPorDia = "2.00", modo = ConfiguracionResponse.ModoRecargoRetraso.ACUMULATIVA)
        assertEquals(BigDecimal("6.00"), v.recargoPorRetraso(LocalDate.of(2026, 1, 13))) // 2 * 3 dias
        assertEquals(BigDecimal.ZERO, v.recargoPorRetraso(LocalDate.of(2026, 1, 10)))     // en fecha
    }

    @Test
    fun recargo_fijo_se_cobra_una_sola_vez() {
        val v = vm(recargoPorDia = "2.00", modo = ConfiguracionResponse.ModoRecargoRetraso.FIJA)
        assertEquals(BigDecimal("2.00"), v.recargoPorRetraso(LocalDate.of(2026, 1, 13))) // fijo, sin importar 3 dias
    }

    @Test
    fun sin_recargo_configurado_no_cobra_retraso() {
        val v = vm(recargoPorDia = null, modo = ConfiguracionResponse.ModoRecargoRetraso.ACUMULATIVA)
        assertEquals(BigDecimal.ZERO, v.recargoPorRetraso(LocalDate.of(2026, 1, 13)))
    }
}
