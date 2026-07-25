package com.costumi.app.ui.gestion.pagos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ArticuloConcepto
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.apiclient.models.ComprobanteResponse
import com.costumi.apiclient.models.IntentoDePagoRequest
import com.costumi.apiclient.models.Porcion
import com.costumi.apiclient.models.RegistrarCobroMixtoRequest
import com.costumi.apiclient.models.RegistrarPagoRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

sealed interface EventoPago {
    data class Registrado(val mensaje: String) : EventoPago
    data class Comprobante(val bytes: ByteArray) : EventoPago
    data class Checkout(val url: String) : EventoPago
    data class Error(val mensaje: String) : EventoPago
}

/** Gestiona los cobros de una operación (venta o renta): comprobante, saldo, cobrar y comprobante PDF. */
@HiltViewModel
class PagoConceptoViewModel @Inject constructor(
    private val repo: PagoRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val conceptoId: UUID = UUID.fromString(estado[PagoConceptoFragment.ARG_CONCEPTO_ID]!!)
    private val tipoConcepto = RegistrarPagoRequest.TipoConcepto.valueOf(estado[PagoConceptoFragment.ARG_TIPO]!!)
    val tipoMixto = RegistrarCobroMixtoRequest.TipoConcepto.valueOf(estado[PagoConceptoFragment.ARG_TIPO]!!)
    private val tipoIntento = IntentoDePagoRequest.TipoConcepto.valueOf(estado[PagoConceptoFragment.ARG_TIPO]!!)
    val esRenta = tipoConcepto == RegistrarPagoRequest.TipoConcepto.RENTA
    private var sucursalId: UUID? =
        estado.get<String>(PagoConceptoFragment.ARG_SUCURSAL_ID)?.let { UUID.fromString(it) }
    val total: BigDecimal =
        estado.get<String>(PagoConceptoFragment.ARG_TOTAL)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val titulo: String = estado.get<String>(PagoConceptoFragment.ARG_TITULO) ?: "Operacion"

    private val _estado = MutableStateFlow<UiState<ComprobanteResponse>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPago>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private val _articulos = MutableStateFlow<List<ArticuloConcepto>>(emptyList())
    val articulos = _articulos.asStateFlow()

    /** Saldo pendiente por cobrar = total del concepto + multa de la renta − lo neto ya cobrado. */
    fun pendiente(comprobante: ComprobanteResponse): BigDecimal =
        (total + (comprobante.multa ?: BigDecimal.ZERO) - (comprobante.saldoNeto ?: BigDecimal.ZERO))
            .max(BigDecimal.ZERO)

    init {
        cargar()
        cargarArticulos()
    }

    /** Trae los artículos (con foto) de la operación para que el cobro muestre QUÉ se está cobrando. */
    private fun cargarArticulos() {
        viewModelScope.launch {
            val tipo = if (esRenta) TipoConcepto.RENTA else TipoConcepto.VENTA
            (repo.articulos(tipo, conceptoId) as? RespuestaRed.Exito)?.let { _articulos.value = it.data }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.comprobante(conceptoId)) {
                is RespuestaRed.Exito -> UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    fun cobrar(
        monto: BigDecimal,
        metodo: RegistrarPagoRequest.Metodo,
        tipoPago: RegistrarPagoRequest.TipoPago,
        referencia: String?,
    ) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val sucursal = resolverSucursal()
            if (sucursal == null) {
                _eventos.tryEmit(EventoPago.Error("No se pudo determinar la sucursal."))
            } else {
                val req = RegistrarPagoRequest(
                    sucursalId = sucursal,
                    tipoConcepto = tipoConcepto,
                    conceptoId = conceptoId,
                    monto = monto,
                    metodo = metodo,
                    tipoPago = tipoPago,
                    referencia = referencia?.ifBlank { null },
                    claveIdempotencia = UUID.randomUUID().toString(),
                )
                when (val r = repo.registrar(req)) {
                    is RespuestaRed.Exito ->
                        _eventos.tryEmit(EventoPago.Registrado("Pago registrado por ${r.data.monto}."))
                    is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPago.Error(r.error.mensaje))
                }
                cargar()
            }
            _procesando.value = false
        }
    }

    fun cobrarMixto(porciones: List<Porcion>, efectivoRecibido: BigDecimal?) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val sucursal = resolverSucursal()
            if (sucursal == null) {
                _eventos.tryEmit(EventoPago.Error("No se pudo determinar la sucursal."))
            } else {
                val req = RegistrarCobroMixtoRequest(
                    sucursalId = sucursal,
                    tipoConcepto = tipoMixto,
                    conceptoId = conceptoId,
                    porciones = porciones,
                    efectivoRecibido = efectivoRecibido,
                    claveIdempotencia = UUID.randomUUID().toString(),
                )
                when (val r = repo.registrarMixto(req)) {
                    is RespuestaRed.Exito -> {
                        val vuelto = r.data.vuelto?.takeIf { it.signum() > 0 }
                        val msg = "Cobro mixto por ${r.data.total}" + (vuelto?.let { " · vuelto $it" } ?: "")
                        _eventos.tryEmit(EventoPago.Registrado(msg))
                    }
                    is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPago.Error(r.error.mensaje))
                }
                cargar()
            }
            _procesando.value = false
        }
    }

    /** Inicia el pago en línea del saldo pendiente y entrega la URL del checkout alojado (RF-6.11). */
    fun pagarEnLinea(monto: BigDecimal) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val sucursal = resolverSucursal()
            if (sucursal == null) {
                _eventos.tryEmit(EventoPago.Error("No se pudo determinar la sucursal."))
            } else {
                val req = IntentoDePagoRequest(
                    sucursalId = sucursal,
                    tipoConcepto = tipoIntento,
                    conceptoId = conceptoId,
                    monto = monto,
                )
                when (val r = repo.intento(req)) {
                    is RespuestaRed.Exito -> {
                        val url = r.data.urlCheckout
                        if (url.isNullOrBlank()) _eventos.tryEmit(EventoPago.Error("La pasarela no devolvio un checkout."))
                        else _eventos.tryEmit(EventoPago.Checkout(url))
                    }
                    is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPago.Error(r.error.mensaje))
                }
            }
            _procesando.value = false
        }
    }

    fun descargarComprobante() {
        viewModelScope.launch {
            when (val r = repo.comprobantePdf(conceptoId)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoPago.Comprobante(r.data))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPago.Error(r.error.mensaje))
            }
        }
    }

    private suspend fun resolverSucursal(): UUID? {
        sucursalId?.let { return it }
        return when (val r = repo.sucursalPorDefecto()) {
            is RespuestaRed.Exito -> r.data.also { sucursalId = it }
            is RespuestaRed.Fallo -> null
        }
    }
}
