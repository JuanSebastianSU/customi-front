package com.costumi.app.ui.gestion.caja

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CajaRepository
import com.costumi.apiclient.models.RegistrarMovimientoRequest
import com.costumi.apiclient.models.TurnoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

/** Detalle de un turno: corte por método, movimientos, registrar movimiento y cerrar (arqueo). */
@HiltViewModel
class TurnoDetalleViewModel @Inject constructor(
    private val repo: CajaRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val turnoId: UUID = UUID.fromString(estado[TurnoDetalleFragment.ARG_ID]!!)
    val sucursalNombre: String = estado.get<String>(TurnoDetalleFragment.ARG_SUCURSAL) ?: "Turno de caja"

    private val _estado = MutableStateFlow<UiState<TurnoResponse>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoCaja>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Efectivo esperado en caja (fondo + ingresos − egresos en efectivo), del corte del backend. */
    fun efectivoEsperado(turno: TurnoResponse): BigDecimal =
        turno.corte?.get("EFECTIVO") ?: BigDecimal.ZERO

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.turnos()) {
                is RespuestaRed.Exito -> {
                    val turno = r.data.firstOrNull { it.id == turnoId }
                    if (turno == null) UiState.Error("No se encontro el turno.") { cargar() }
                    else UiState.Success(turno)
                }
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun registrarMovimiento(req: RegistrarMovimientoRequest) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.registrarMovimiento(turnoId, req)) {
                is RespuestaRed.Exito -> { _estado.value = UiState.Success(r.data); _eventos.tryEmit(EventoCaja.Info("Movimiento registrado.")) }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCaja.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    fun cerrar(efectivoContado: BigDecimal) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            when (val r = repo.cerrar(turnoId, efectivoContado)) {
                is RespuestaRed.Exito -> {
                    _estado.value = UiState.Success(r.data)
                    val dif = r.data.diferenciaEfectivo ?: BigDecimal.ZERO
                    val nota = when {
                        dif.signum() == 0 -> "cuadre exacto"
                        dif.signum() > 0 -> "sobra ${dif}"
                        else -> "falta ${dif.abs()}"
                    }
                    _eventos.tryEmit(EventoCaja.Info("Turno cerrado ($nota)."))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCaja.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
