package com.costumi.app.ui.cliente.carrito

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.TipoError
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.apis.CarritoControllerApi
import com.costumi.apiclient.models.CarritoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Evento de una sola vez del checkout. */
sealed interface EventoCheckout {
    /** El pedido se creó y hay un único concepto que pagar: se va a la pantalla de pago. */
    data class IrAPago(
        val tipo: String, // "RENTA" o "VENTA"
        val conceptoId: String,
        val empresaId: String,
        val sucursalId: String,
    ) : EventoCheckout

    /** El pedido quedó reservado (varias rentas): se paga en la tienda. */
    data class Exito(val mensaje: String) : EventoCheckout
    data class Error(val mensaje: String) : EventoCheckout
}

@HiltViewModel
class CarritoViewModel @Inject constructor(
    private val repo: PedidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val empresaId: String = savedStateHandle[CarritoFragment.ARG_EMPRESA_ID] ?: ""
    private val sucursalId: String = savedStateHandle[CarritoFragment.ARG_SUCURSAL_ID] ?: ""
    val esRenta: Boolean = savedStateHandle.get<String>(CarritoFragment.ARG_TIPO) == "RENTA"

    private val _estado = MutableStateFlow<UiState<CarritoResponse>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    /** Sucursal de retiro ("Sucursal Norte · Cra 45 #10-20") para la cabecera; null hasta resolverla. */
    private val _retiro = MutableStateFlow<String?>(null)
    val retiro = _retiro.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoCheckout>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
        cargarRetiro()
    }

    /** Resuelve el nombre y dirección de la sucursal de retiro (hoy el carrito solo trae ids). */
    private fun cargarRetiro() {
        if (empresaId.isBlank() || sucursalId.isBlank()) return
        viewModelScope.launch {
            val sucursales = (repo.sucursales(empresaId) as? RespuestaRed.Exito)?.data.orEmpty()
            val suc = sucursales.firstOrNull { it.id?.toString() == sucursalId } ?: return@launch
            val nombre = suc.nombre.orEmpty()
            val direccion = suc.direccion?.takeIf { it.isNotBlank() }
            _retiro.value = "Retiras en $nombre" + if (direccion != null) "  ·  $direccion" else ""
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            val tipo = if (esRenta) {
                CarritoControllerApi.TipoPendiente.RENTA
            } else {
                CarritoControllerApi.TipoPendiente.VENTA
            }
            when (val r = repo.carritoPendiente(sucursalId, tipo, empresaId)) {
                is RespuestaRed.Exito -> {
                    val lineas = r.data.lineas.orEmpty()
                    _estado.value = if (lineas.isEmpty()) UiState.Empty else UiState.Success(r.data)
                }
                // No tener carrito todavia no es un error: es un carrito vacio.
                is RespuestaRed.Fallo -> _estado.value = if (r.error.tipo == TipoError.NO_ENCONTRADO) {
                    UiState.Empty
                } else {
                    UiState.Error(r.error.mensaje) { cargar() }
                }
            }
        }
    }

    /** Quita una linea del carrito y recarga (el backend devuelve el carrito ya sin ella). */
    fun quitar(linea: com.costumi.apiclient.models.LineaDeCarritoResponse) {
        val lineaId = linea.id ?: return
        viewModelScope.launch {
            val tipo = if (esRenta) {
                CarritoControllerApi.TipoQuitarItem.RENTA
            } else {
                CarritoControllerApi.TipoQuitarItem.VENTA
            }
            when (val r = repo.quitarDelCarrito(lineaId.toString(), sucursalId, tipo, empresaId)) {
                is RespuestaRed.Exito -> cargar()
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCheckout.Error(r.error.mensaje))
            }
        }
    }

    /** Cambia la cantidad de una línea (A10). Mínimo 1; recarga el carrito con los totales recalculados. */
    fun cambiarCantidad(linea: com.costumi.apiclient.models.LineaDeCarritoResponse, cantidad: Int) {
        val lineaId = linea.id ?: return
        if (cantidad < 1) return
        viewModelScope.launch {
            val tipo = if (esRenta) {
                com.costumi.apiclient.models.EditarCantidadRequest.Tipo.RENTA
            } else {
                com.costumi.apiclient.models.EditarCantidadRequest.Tipo.VENTA
            }
            when (val r = repo.editarCantidad(lineaId.toString(), cantidad, sucursalId, tipo, empresaId)) {
                is RespuestaRed.Exito -> cargar()
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCheckout.Error(r.error.mensaje))
            }
        }
    }

    fun finalizar() {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val resultado = if (esRenta) {
                when (val r = repo.checkoutRenta(empresaId, sucursalId)) {
                    is RespuestaRed.Exito -> {
                        val rentas = r.data.rentaIds.orEmpty()
                        when {
                            rentas.size == 1 -> EventoCheckout.IrAPago(
                                "RENTA", rentas.first().toString(), empresaId, sucursalId,
                            )
                            // Varias rentas (varios periodos): se reservan y se pagan en la tienda.
                            rentas.isNotEmpty() -> EventoCheckout.Exito(
                                "¡Rentas reservadas! Pasa por la tienda a pagar y retirar con tu codigo.",
                            )
                            else -> EventoCheckout.Error("No se creo ninguna renta.")
                        }
                    }
                    is RespuestaRed.Fallo -> EventoCheckout.Error(r.error.mensaje)
                }
            } else {
                when (val r = repo.checkoutVenta(empresaId, sucursalId)) {
                    is RespuestaRed.Exito -> {
                        val ventaId = r.data.ventaId
                        if (ventaId != null) {
                            EventoCheckout.IrAPago("VENTA", ventaId.toString(), empresaId, sucursalId)
                        } else {
                            EventoCheckout.Error("No se creo la venta.")
                        }
                    }
                    is RespuestaRed.Fallo -> EventoCheckout.Error(r.error.mensaje)
                }
            }
            _eventos.tryEmit(resultado)
            _procesando.value = false
        }
    }
}
