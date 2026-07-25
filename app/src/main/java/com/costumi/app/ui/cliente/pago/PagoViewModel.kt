package com.costumi.app.ui.cliente.pago

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.app.data.repo.PagoClienteRepository
import com.costumi.apiclient.models.IntentoDePagoDeClienteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

/** Datos del pedido recién creado, para mostrar el desglose y decidir el pago. */
data class PagoUi(
    val tipoTexto: String,
    val tienda: String?,
    val total: BigDecimal?,
    val codigoRetiro: String?,
)

/**
 * Pantalla de pago: tras crear la renta/venta, muestra el total y el código de retiro y deja elegir
 * pagar con tarjeta (en línea el total) o en la tienda (efectivo). El total sale del historial del cliente.
 */
@HiltViewModel
class PagoViewModel @Inject constructor(
    private val cuentaRepo: CuentaRepository,
    private val pagoRepo: PagoClienteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tipo: String = savedStateHandle[PagoFragment.ARG_TIPO] ?: "VENTA"
    private val conceptoId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[PagoFragment.ARG_CONCEPTO_ID] ?: "") }.getOrNull()
    private val empresaId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[PagoFragment.ARG_EMPRESA_ID] ?: "") }.getOrNull()
    private val sucursalId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[PagoFragment.ARG_SUCURSAL_ID] ?: "") }.getOrNull()

    private val _estado = MutableStateFlow<UiState<PagoUi>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPago>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private var total: BigDecimal? = null

    init {
        cargar()
    }

    fun cargar() {
        val id = conceptoId
        if (id == null) {
            _estado.value = UiState.Error("Pedido no valido.") {}
            return
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = cuentaRepo.miHistorial()) {
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
                is RespuestaRed.Exito -> {
                    val item = r.data.firstOrNull { it.operacionId == id }
                    if (item == null) {
                        _estado.value = UiState.Error("No encontramos tu pedido.") { cargar() }
                        return@launch
                    }
                    total = item.monto
                    val tipoTexto = if (tipo == "RENTA") "Renta" else "Compra"
                    _estado.value = UiState.Success(
                        PagoUi(tipoTexto, item.empresaNombre, item.monto, item.codigoRetiro),
                    )
                }
            }
        }
    }

    fun pagarConTarjeta() {
        val e = empresaId
        val s = sucursalId
        val c = conceptoId
        val monto = total
        if (e == null || s == null || c == null || monto == null) {
            emitir(EventoPago.Error("No se puede iniciar el pago de este pedido."))
            return
        }
        val concepto = if (tipo == "RENTA") {
            IntentoDePagoDeClienteRequest.TipoConcepto.RENTA
        } else {
            IntentoDePagoDeClienteRequest.TipoConcepto.VENTA
        }
        viewModelScope.launch {
            _cargando.value = true
            val r = pagoRepo.intento(e, s, concepto, c, monto)
            _cargando.value = false
            when (r) {
                is RespuestaRed.Fallo -> emitir(EventoPago.Error(r.error.mensaje))
                is RespuestaRed.Exito -> {
                    val url = r.data.urlCheckout
                    if (url.isNullOrBlank()) emitir(EventoPago.Error("La pasarela no devolvio un enlace de pago."))
                    else emitir(EventoPago.AbrirCheckout(url))
                }
            }
        }
    }

    fun pagarEnTienda() {
        emitir(EventoPago.PagoEnTienda)
    }

    private fun emitir(evento: EventoPago) {
        _eventos.tryEmit(evento)
    }
}
