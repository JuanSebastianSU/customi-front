package com.costumi.app.ui.gestion.devoluciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ConfiguracionRepository
import com.costumi.app.data.repo.DevolucionRepository
import com.costumi.apiclient.models.ConfiguracionResponse
import com.costumi.apiclient.models.RegistrarDevolucionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

sealed interface EventoDev {
    data class Registrada(val remanente: BigDecimal?, val multa: BigDecimal?) : EventoDev
    data class Error(val mensaje: String) : EventoDev
}

/** Lo que hay que saber de una prenda para revisarla: como se llama, como se ve y cuanto cuesta el dano. */
data class PrendaARevisar(val nombre: String, val fotoUrl: String?, val valorDano: BigDecimal?)

/**
 * Liquidacion del deposito tal como queda con lo marcado hasta ahora. Se recalcula en cada toque para
 * que el empleado (y el cliente que tiene enfrente) vean el numero antes de confirmar, no despues.
 */
data class Liquidacion(
    val deposito: BigDecimal,
    val danos: BigDecimal,
    val retraso: BigDecimal,
    /** Dias de atraso respecto a la fecha pactada; 0 si llego en fecha. */
    val diasRetraso: Long,
    /** El recargo por retraso lo puso el usuario a mano (si no, sale de la politica de la tienda). */
    val retrasoManual: Boolean,
    /** Los cargos no se cobran porque el modulo de multas esta apagado. */
    val multasApagadas: Boolean,
) {
    val cargos: BigDecimal get() = if (multasApagadas) BigDecimal.ZERO else danos + retraso

    /** Positivo: se le devuelve al cliente. Negativo: el cliente queda debiendo. */
    val saldo: BigDecimal get() = deposito - cargos
}

/** Arma el checklist de una renta (una fila por unidad) y registra la devolucion. */
@HiltViewModel
class DevolucionFormViewModel @Inject constructor(
    private val repo: DevolucionRepository,
    private val configuracion: ConfiguracionRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    /** IDs de prenda, uno por unidad amparada por la renta (fuente del checklist). */
    val prendaIds: List<String> = estado.get<ArrayList<String>>(DevolucionFormFragment.ARG_PRENDA_IDS).orEmpty()
    val depositoInicial: String? = estado[DevolucionFormFragment.ARG_DEPOSITO]
    val fechaDevolucionPactada: LocalDate? =
        estado.get<String>(DevolucionFormFragment.ARG_FECHA_DEV)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private val _prendas = MutableStateFlow<UiState<Map<String, PrendaARevisar>>>(UiState.Loading)
    val prendas = _prendas.asStateFlow()

    /** Politica de la tienda: si las multas estan activas y cuanto se cobra por dia de retraso. */
    private var config: ConfiguracionResponse? = null

    private val _registrando = MutableStateFlow(false)
    val registrando = _registrando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDev>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _prendas.value = UiState.Loading
            // La configuracion es best-effort: sin ella no se puede anticipar el recargo por retraso,
            // pero la devolucion se puede registrar igual (lo calcula el servidor).
            config = (configuracion.obtener() as? RespuestaRed.Exito)?.data
            _prendas.value = when (val r = repo.prendas()) {
                is RespuestaRed.Exito -> UiState.Success(
                    r.data.mapNotNull { p ->
                        p.id?.toString()?.let { it to PrendaARevisar(p.nombre.orEmpty(), p.fotoUrl, p.valorDano) }
                    }.toMap(),
                )
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    val multasActivas: Boolean get() = config?.multasActivo != false

    /** La tienda tiene configurado cuanto cobrar por retraso (si no, no se cobra nada por llegar tarde). */
    val tieneRecargoConfigurado: Boolean get() = (config?.recargoPorRetrasoPorDia ?: BigDecimal.ZERO).signum() > 0

    /** Dias que la renta se paso de la fecha pactada (0 si llego en fecha o no se sabe la pactada). */
    fun diasDeRetraso(fechaReal: LocalDate): Long {
        val pactada = fechaDevolucionPactada ?: return 0
        return ChronoUnit.DAYS.between(pactada, fechaReal).coerceAtLeast(0)
    }

    /**
     * Recargo por retraso segun la politica de la tienda: por dia (ACUMULATIVA) o una sola vez (FIJA).
     * Es la misma cuenta que hara el servidor; sirve para mostrarla antes de confirmar.
     */
    fun recargoPorRetraso(fechaReal: LocalDate): BigDecimal {
        val dias = diasDeRetraso(fechaReal)
        if (dias <= 0) return BigDecimal.ZERO
        val porDia = config?.recargoPorRetrasoPorDia ?: return BigDecimal.ZERO
        return if (config?.modoRecargoRetraso == ConfiguracionResponse.ModoRecargoRetraso.FIJA) {
            porDia
        } else {
            porDia * BigDecimal(dias)
        }
    }

    fun registrar(req: RegistrarDevolucionRequest) {
        if (_registrando.value) return
        viewModelScope.launch {
            _registrando.value = true
            when (val r = repo.registrar(req)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoDev.Registrada(r.data.remanente, r.data.multa))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoDev.Error(r.error.mensaje))
            }
            _registrando.value = false
        }
    }
}
