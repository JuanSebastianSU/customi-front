package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.ItemDisfrazDto
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface EventoPedido {
    data class Exito(val mensaje: String) : EventoPedido
    data class Error(val mensaje: String) : EventoPedido
}

/** Arma un pedido de varios disfraces (carrito) y lo confirma como una sola renta o venta. */
@HiltViewModel
class PedidoDisfracesViewModel @Inject constructor(
    private val repo: DisfrazRepository,
    private val pedido: PedidoDisfracesStore,
) : ViewModel() {

    val items = pedido.items

    private val _clientes = MutableStateFlow<List<ClienteResponse>>(emptyList())
    val clientes = _clientes.asStateFlow()
    private val _sucursales = MutableStateFlow<List<SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()
    private val _disfraces = MutableStateFlow<List<DisfrazResponse>>(emptyList())
    val disfraces = _disfraces.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()
    private val _eventos = MutableSharedFlow<EventoPedido>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    var modoRenta: Boolean = true
    var clienteId: UUID? = null
    var sucursalId: UUID? = null
    var retiro: LocalDate? = null
    var devolucion: LocalDate? = null

    init {
        pedido.limpiar() // cada vez que se abre la pantalla, el pedido empieza vacío
        cargarSelectores()
    }

    private fun cargarSelectores() {
        viewModelScope.launch {
            (repo.clientesParaSelector() as? RespuestaRed.Exito)?.data?.let { _clientes.value = it }
            (repo.sucursalesParaSelector() as? RespuestaRed.Exito)?.data?.let { sucs ->
                _sucursales.value = sucs
                sucursalId = sucs.firstOrNull()?.id
            }
            (repo.disfraces() as? RespuestaRed.Exito)?.data?.let { ds ->
                _disfraces.value = ds.filter { it.activo == true }
            }
        }
    }

    fun quitar(indice: Int) = pedido.quitar(indice)

    fun confirmar() {
        val actuales = pedido.items.value
        if (actuales.isEmpty()) return emitir(EventoPedido.Error("Agrega al menos un disfraz al pedido."))
        val cli = clienteId ?: return emitir(EventoPedido.Error("Elige el cliente."))
        val suc = sucursalId ?: return emitir(EventoPedido.Error("Elige la sucursal."))
        if (modoRenta && (retiro == null || devolucion == null)) {
            return emitir(EventoPedido.Error("Elige las fechas de renta."))
        }
        val dto = actuales.map {
            ItemDisfrazDto(disfrazId = it.disfrazId, cantidad = it.cantidad, selecciones = it.selecciones)
        }
        viewModelScope.launch {
            _cargando.value = true
            val r = if (modoRenta) repo.rentarVarios(cli, suc, retiro!!, devolucion!!, dto)
            else repo.venderVarios(cli, suc, dto)
            _cargando.value = false
            when (r) {
                is RespuestaRed.Exito -> {
                    pedido.limpiar()
                    emitir(
                        EventoPedido.Exito(
                            if (modoRenta) "Pedido rentado. Cobra desde Rentas."
                            else "Pedido vendido. Cobra desde Ventas.",
                        ),
                    )
                }
                is RespuestaRed.Fallo -> emitir(EventoPedido.Error(r.error.mensaje))
            }
        }
    }

    private fun emitir(e: EventoPedido) {
        _eventos.tryEmit(e)
    }
}
