package com.costumi.app.ui.gestion.reembolsos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.ReembolsoRepository
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.apiclient.models.SolicitarReembolsoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Selector de operación (venta/renta) para registrar una solicitud de reembolso. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SolicitarReembolsoViewModel @Inject constructor(
    private val pagos: PagoRepository,
    private val reembolsos: ReembolsoRepository,
) : ViewModel() {

    private val _tipo = MutableStateFlow(TipoConcepto.VENTA)
    val tipo = _tipo.asStateFlow()

    val operaciones = _tipo.flatMapLatest { pagos.operaciones(it) }.cachedIn(viewModelScope)

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoReembolso>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun cambiarTipo(tipo: TipoConcepto) {
        if (_tipo.value != tipo) _tipo.value = tipo
    }

    fun solicitar(op: OperacionPago, monto: BigDecimal, motivo: String?) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val tipoConcepto = if (op.tipo == TipoConcepto.RENTA) {
                SolicitarReembolsoRequest.TipoConcepto.RENTA
            } else {
                SolicitarReembolsoRequest.TipoConcepto.VENTA
            }
            when (val r = reembolsos.solicitar(tipoConcepto, op.id, monto, motivo)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoReembolso.Info("Solicitud registrada."))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoReembolso.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
