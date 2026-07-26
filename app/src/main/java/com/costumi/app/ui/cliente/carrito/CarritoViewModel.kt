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

/** Evento de una sola vez del carrito. */
sealed interface EventoCheckout {
    /**
     * Ir a la pantalla de pago. NO se crea nada todavía: la orden y el código de retiro se materializan
     * recién al confirmar el pago allí. Así, si el cliente entra a pagar y vuelve, el carrito sigue intacto.
     */
    data class IrAPago(
        val tipo: String, // "RENTA" o "VENTA"
        val empresaId: String,
        val sucursalId: String,
    ) : EventoCheckout

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

    /**
     * "Finalizar" solo lleva a la pantalla de pago; NO hace checkout todavía. El pedido y su código se
     * crean allí al confirmar el pago. Antes, esto llamaba al checkout aquí y creaba la renta/venta apenas
     * el cliente entraba a pagar: si se iba sin pagar, el carrito quedaba vacío y aparecía un pedido "por
     * retirar" sin pagar. Ahora el carrito sobrevive intacto hasta que realmente se paga.
     */
    fun irAPago() {
        if (empresaId.isBlank() || sucursalId.isBlank()) {
            _eventos.tryEmit(EventoCheckout.Error("No se pudo identificar la tienda."))
            return
        }
        _eventos.tryEmit(EventoCheckout.IrAPago(if (esRenta) "RENTA" else "VENTA", empresaId, sucursalId))
    }
}
