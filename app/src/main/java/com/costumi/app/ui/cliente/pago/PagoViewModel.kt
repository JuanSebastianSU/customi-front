package com.costumi.app.ui.cliente.pago

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.CuentaRepository
import com.costumi.app.data.repo.PagoClienteRepository
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.apis.CarritoControllerApi
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

/**
 * Lo que el cliente ve ANTES de pagar: el total a pagar, tomado del carrito valorizado. Todavía no hay
 * orden ni código de retiro: eso se materializa recién al confirmar el pago.
 */
data class PagoUi(
    val tipoTexto: String,
    val total: BigDecimal?,
    val deposito: BigDecimal?,
)

/**
 * Pantalla de pago. Muestra el total del CARRITO (aún sin crear la orden) y deja elegir pagar con tarjeta
 * o en la tienda. La orden y su código de retiro se crean SOLO al confirmar el pago (checkout diferido):
 * así, si el cliente entra a pagar y se va sin confirmar, el carrito sigue intacto y no queda ningún
 * pedido "por retirar" sin pagar.
 */
@HiltViewModel
class PagoViewModel @Inject constructor(
    private val pedidoRepo: PedidoRepository,
    private val cuentaRepo: CuentaRepository,
    private val pagoRepo: PagoClienteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tipo: String = savedStateHandle[PagoFragment.ARG_TIPO] ?: "VENTA"
    private val esRenta = tipo == "RENTA"
    private val empresaId: String = savedStateHandle[PagoFragment.ARG_EMPRESA_ID] ?: ""
    private val sucursalId: String = savedStateHandle[PagoFragment.ARG_SUCURSAL_ID] ?: ""

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

    /** Total a pagar = total del carrito valorizado (misma lógica que el checkout). Aún no hay código. */
    fun cargar() {
        if (empresaId.isBlank() || sucursalId.isBlank()) {
            _estado.value = UiState.Error("Pedido no valido.") {}
            return
        }
        val tipoPendiente = if (esRenta) {
            CarritoControllerApi.TipoPendiente.RENTA
        } else {
            CarritoControllerApi.TipoPendiente.VENTA
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = pedidoRepo.carritoPendiente(sucursalId, tipoPendiente, empresaId)) {
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
                is RespuestaRed.Exito -> {
                    val carrito = r.data
                    if (carrito.lineas.orEmpty().isEmpty()) {
                        _estado.value = UiState.Error("Tu carrito esta vacio.") {}
                        return@launch
                    }
                    total = carrito.total
                    val tipoTexto = if (esRenta) "Renta" else "Compra"
                    _estado.value = UiState.Success(PagoUi(tipoTexto, carrito.total, carrito.totalDeposito))
                }
            }
        }
    }

    /**
     * Pago en la tienda (efectivo): AQUÍ se materializa la orden (checkout) y recién entonces existe el
     * código de retiro. Antes de este momento no hay nada creado.
     */
    fun pagarEnTienda() {
        if (_cargando.value) return
        viewModelScope.launch {
            _cargando.value = true
            val creada = crearOrden()
            _cargando.value = false
            when (creada) {
                is Orden.Fallo -> emitir(EventoPago.Error(creada.mensaje))
                is Orden.Ok -> {
                    // Con un único concepto podemos mostrar su código; con varias rentas se ven en Mis Pedidos.
                    val codigo = creada.conceptoUnico?.let { buscarCodigo(it) }
                    emitir(EventoPago.Reservado(codigo))
                }
            }
        }
    }

    /**
     * Pago con tarjeta: materializa la orden (checkout) y abre la pasarela por el concepto creado. Si el
     * checkout creó varias rentas (varios periodos), no hay un único concepto que cobrar con tarjeta: quedan
     * reservadas para pagar en la tienda.
     */
    fun pagarConTarjeta() {
        if (_cargando.value) return
        val monto = total
        if (monto == null) {
            emitir(EventoPago.Error("No se puede iniciar el pago de este pedido."))
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            val creada = crearOrden()
            val conceptoId = (creada as? Orden.Ok)?.conceptoUnico
            when {
                creada is Orden.Fallo -> { _cargando.value = false; emitir(EventoPago.Error(creada.mensaje)) }
                conceptoId == null -> { _cargando.value = false; emitir(EventoPago.Reservado(null)) }
                else -> {
                    val concepto = if (esRenta) {
                        IntentoDePagoDeClienteRequest.TipoConcepto.RENTA
                    } else {
                        IntentoDePagoDeClienteRequest.TipoConcepto.VENTA
                    }
                    val e = UUID.fromString(empresaId)
                    val s = UUID.fromString(sucursalId)
                    val r = pagoRepo.intento(e, s, concepto, conceptoId, monto)
                    _cargando.value = false
                    when (r) {
                        is RespuestaRed.Fallo -> emitir(EventoPago.Error(r.error.mensaje))
                        is RespuestaRed.Exito -> {
                            val url = r.data.urlCheckout
                            if (url.isNullOrBlank()) {
                                emitir(EventoPago.Error("La pasarela no devolvio un enlace de pago."))
                            } else {
                                emitir(EventoPago.AbrirCheckout(url))
                            }
                        }
                    }
                }
            }
        }
    }

    /** Resultado de materializar la orden (checkout). */
    private sealed interface Orden {
        /** [conceptoUnico] = id de la venta o de la única renta creada; null si el checkout creó varias rentas. */
        data class Ok(val conceptoUnico: UUID?) : Orden
        data class Fallo(val mensaje: String) : Orden
    }

    /** Ejecuta el checkout (crea renta/venta y consume el carrito). Solo se llama al confirmar el pago. */
    private suspend fun crearOrden(): Orden {
        return if (esRenta) {
            when (val r = pedidoRepo.checkoutRenta(empresaId, sucursalId)) {
                is RespuestaRed.Fallo -> Orden.Fallo(r.error.mensaje)
                is RespuestaRed.Exito -> {
                    val rentas = r.data.rentaIds.orEmpty()
                    when {
                        rentas.isEmpty() -> Orden.Fallo("No se creo ninguna renta.")
                        rentas.size == 1 -> Orden.Ok(rentas.first())
                        else -> Orden.Ok(null) // varias rentas: se pagan en la tienda
                    }
                }
            }
        } else {
            when (val r = pedidoRepo.checkoutVenta(empresaId, sucursalId)) {
                is RespuestaRed.Fallo -> Orden.Fallo(r.error.mensaje)
                is RespuestaRed.Exito -> r.data.ventaId?.let { Orden.Ok(it) } ?: Orden.Fallo("No se creo la venta.")
            }
        }
    }

    /** Código de retiro de una orden recién creada (lo deriva el backend en el historial del cliente). */
    private suspend fun buscarCodigo(conceptoId: UUID): String? =
        (cuentaRepo.miHistorial() as? RespuestaRed.Exito)?.data
            ?.firstOrNull { it.operacionId == conceptoId }?.codigoRetiro

    private fun emitir(evento: EventoPago) {
        _eventos.tryEmit(evento)
    }
}
