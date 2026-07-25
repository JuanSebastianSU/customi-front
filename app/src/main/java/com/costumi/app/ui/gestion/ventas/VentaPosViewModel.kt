package com.costumi.app.ui.gestion.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.VentaRepository
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.ItemDisfrazDto
import com.costumi.apiclient.models.LineaPrendaVentaDto
import com.costumi.apiclient.models.LineaVentaRequest
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.RegistrarVentaRequest
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class DatosPos(
    val sucursales: List<SucursalResponse>,
    val clientes: List<ClienteResponse>,
    val prendas: List<PrendaResponse>,
    val disfraces: List<DisfrazResponse>,
    // Para los filtros del buscador visual de artículos (mismos que armar disfraz).
    val categorias: List<com.costumi.apiclient.models.CategoriaResponse>,
    val etiquetas: List<com.costumi.app.data.repo.TipoConValores>,
)

sealed interface EventoPos {
    data class Registrada(val total: BigDecimal?) : EventoPos
    data class Error(val mensaje: String) : EventoPos
}

/** POS: carga sucursales/clientes/prendas y registra la venta (idempotente por clave estable). */
@HiltViewModel
class VentaPosViewModel @Inject constructor(
    private val repo: VentaRepository,
    private val disfrazRepo: DisfrazRepository,
    private val pedido: PedidoDisfracesStore,
) : ViewModel() {

    /** Clave de idempotencia estable para toda la vida del carrito (reintentos no duplican). */
    val claveIdempotencia: String = UUID.randomUUID().toString()

    private val _datos = MutableStateFlow<UiState<DatosPos>>(UiState.Loading)
    val datos = _datos.asStateFlow()

    private val _registrando = MutableStateFlow(false)
    val registrando = _registrando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoPos>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Disfraces agregados a esta venta (carrito compartido con el asignador). */
    val disfracesPedido = pedido.items

    init {
        // Cada "Nueva venta" empieza sin disfraces del pedido anterior.
        pedido.limpiar()
        cargar()
    }

    fun quitarDisfraz(indice: Int) = pedido.quitar(indice)

    /** Catálogo del inventario (con foto y stock) para el buscador visual de artículos del ticket. */
    suspend fun catalogo(): List<com.costumi.apiclient.models.PrendaDeCatalogoResponse> =
        (disfrazRepo.catalogoInventario() as? RespuestaRed.Exito)?.data.orEmpty()

    fun cargar() {
        viewModelScope.launch {
            _datos.value = UiState.Loading
            val suc = repo.sucursales()
            if (suc is RespuestaRed.Fallo) { _datos.value = error(suc); return@launch }
            val cli = repo.clientesParaSelector()
            if (cli is RespuestaRed.Fallo) { _datos.value = error(cli); return@launch }
            val pre = repo.prendasParaSelector()
            if (pre is RespuestaRed.Fallo) { _datos.value = error(pre); return@launch }
            // Los disfraces son opcionales: si fallan, se puede vender solo prendas.
            val dis = (disfrazRepo.disfraces() as? RespuestaRed.Exito)?.data.orEmpty()
                .filter { it.activo == true }
            // Categorías y etiquetas para los filtros del buscador de artículos (best-effort).
            val categorias = (disfrazRepo.categoriasParaPool() as? RespuestaRed.Exito)?.data.orEmpty()
            val etiquetas = (disfrazRepo.tiposConValores() as? RespuestaRed.Exito)?.data.orEmpty()
            _datos.value = UiState.Success(
                DatosPos(
                    (suc as RespuestaRed.Exito).data,
                    (cli as RespuestaRed.Exito).data,
                    (pre as RespuestaRed.Exito).data,
                    dis,
                    categorias,
                    etiquetas,
                ),
            )
        }
    }

    private fun error(f: RespuestaRed.Fallo) = UiState.Error(f.error.mensaje) { cargar() }

    fun registrar(
        sucursalId: UUID,
        clienteId: UUID?,
        lineas: List<LineaVentaRequest>,
        descuento: BigDecimal?,
    ) {
        if (_registrando.value) return
        viewModelScope.launch {
            _registrando.value = true
            val req = RegistrarVentaRequest(
                sucursalId = sucursalId,
                clienteId = clienteId,
                descuento = descuento,
                claveIdempotencia = claveIdempotencia,
                lineas = lineas,
            )
            val r = repo.registrar(req)
            _registrando.value = false
            _eventos.tryEmit(
                when (r) {
                    is RespuestaRed.Exito -> EventoPos.Registrada(r.data.total)
                    is RespuestaRed.Fallo -> EventoPos.Error(r.error.mensaje)
                },
            )
        }
    }

    /**
     * Registra un pedido MIXTO en una sola venta: prendas sueltas ({@code lineas}) y/o disfraces
     * ({@code items}). Se usa cuando la venta incluye al menos un disfraz. El cliente es opcional.
     */
    fun registrarMixto(
        sucursalId: UUID,
        clienteId: UUID?,
        lineas: List<LineaPrendaVentaDto>,
        items: List<ItemDisfrazDto>,
    ) {
        if (_registrando.value) return
        viewModelScope.launch {
            _registrando.value = true
            val r = disfrazRepo.venderVarios(clienteId, sucursalId, items, lineas)
            _registrando.value = false
            when (r) {
                is RespuestaRed.Exito -> {
                    pedido.limpiar()
                    _eventos.tryEmit(EventoPos.Registrada(null))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoPos.Error(r.error.mensaje))
            }
        }
    }

    /** Los disfraces (por id) del pedido en curso, para construir los {@code ItemDisfrazDto}. */
    fun itemsDisfrazDelPedido(): List<ItemDisfrazDto> = pedido.items.value.map {
        ItemDisfrazDto(disfrazId = it.disfrazId, cantidad = it.cantidad, selecciones = it.selecciones)
    }
}
