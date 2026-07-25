package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.ui.cliente.detalle.SlotUi
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SlotDto
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

sealed interface EventoAsignar {
    data class Exito(val mensaje: String) : EventoAsignar
    data class Error(val mensaje: String) : EventoAsignar
}

/** Estado del disfraz listo para asignar (vender/rentar) a un cliente desde mostrador. */
data class AsignarUi(
    val disfraz: DisfrazResponse,
    val disponible: Boolean,
    val slots: List<SlotUi>,
    val clientes: List<ClienteResponse>,
    val sucursales: List<SucursalResponse>,
)

/** Vender/rentar un disfraz a un cliente desde mostrador: arma las piezas y crea la renta/venta. */
@HiltViewModel
class DisfrazAsignarViewModel @Inject constructor(
    private val repo: DisfrazRepository,
    private val pedido: PedidoDisfracesStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val nombre: String = savedStateHandle[DisfrazAsignarFragment.ARG_NOMBRE] ?: "Disfraz"

    /** En modo pedido, "confirmar" agrega este disfraz al carrito en vez de rentar/vender directo. */
    val esPedido: Boolean = savedStateHandle[DisfrazAsignarFragment.ARG_PEDIDO] ?: false

    private val disfrazId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[DisfrazAsignarFragment.ARG_DISFRAZ_ID] ?: "") }.getOrNull()

    private val _estado = MutableStateFlow<UiState<AsignarUi>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoAsignar>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private val selecciones = mutableMapOf<Int, UUID?>()
    var clienteId: UUID? = null
    var sucursalId: UUID? = null
    var cantidad: Int = 1

    init {
        cargar()
    }

    fun cargar() {
        val d = disfrazId
        if (d == null) {
            _estado.value = UiState.Error("Disfraz no valido.") {}
            return
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading
            val detalle = repo.detalleAsistido(d)
            if (detalle is RespuestaRed.Fallo) { _estado.value = UiState.Error(detalle.error.mensaje) { cargar() }; return@launch }
            val (empresaId, det) = (detalle as RespuestaRed.Exito).data
            val disfraz = det.disfraz
            if (disfraz == null) { _estado.value = UiState.Error("Disfraz no encontrado.") { cargar() }; return@launch }

            val slotsUi = mutableListOf<SlotUi>()
            selecciones.clear()
            for (slot in disfraz.slots.orEmpty().sortedBy { it.orden ?: 0 }) {
                val orden = slot.orden ?: continue
                val opciones = (repo.opcionesDeSlotAsistido(empresaId, d, orden) as? RespuestaRed.Exito)?.data?.opciones.orEmpty()
                // La preselección cae en la primera opción CON stock: las agotadas no se pueden elegir.
                val defecto = opciones.firstOrNull { (it.unidadesDisponibles ?: 0) > 0 }?.prendaId
                selecciones[orden] = defecto
                slotsUi.add(
                    SlotUi(
                        orden = orden,
                        nombre = slot.nombre ?: "Pieza",
                        personalizable = slot.ejePrenda == SlotDto.EjePrenda.PERSONALIZABLE,
                        opcional = slot.opcional ?: false,
                        opciones = opciones,
                        facetas = emptyList(),
                        seleccionInicial = defecto,
                    ),
                )
            }

            val clientes = (repo.clientesParaSelector() as? RespuestaRed.Exito)?.data.orEmpty()
            val sucursales = (repo.sucursalesParaSelector() as? RespuestaRed.Exito)?.data.orEmpty()
            sucursalId = sucursales.firstOrNull()?.id
            _estado.value = UiState.Success(
                AsignarUi(disfraz, det.disponible ?: false, slotsUi, clientes, sucursales),
            )
        }
    }

    fun seleccionar(orden: Int, prendaId: UUID?) {
        selecciones[orden] = prendaId
    }

    /** Agrega este disfraz configurado (piezas + cantidad) al pedido en curso. */
    fun agregarAlPedido() {
        val d = disfrazId ?: return
        pedido.agregar(PedidoDisfracesStore.Item(d, nombre, cantidad.coerceAtLeast(1), seleccionesDto()))
        emitir(EventoAsignar.Exito("Agregado al pedido."))
    }

    private fun seleccionesDto(): List<SeleccionSlotDto> =
        selecciones.mapNotNull { (orden, prendaId) -> prendaId?.let { SeleccionSlotDto(it, orden) } }

    fun rentar(retiro: LocalDate?, devolucion: LocalDate?) {
        val d = disfrazId ?: return
        val cli = clienteId ?: return emitir(EventoAsignar.Error("Elige el cliente."))
        val suc = sucursalId ?: return emitir(EventoAsignar.Error("Elige la sucursal."))
        if (retiro == null || devolucion == null) return emitir(EventoAsignar.Error("Elige las fechas de renta."))
        viewModelScope.launch {
            _cargando.value = true
            val r = repo.rentarParaCliente(d, cli, suc, retiro, devolucion, cantidad.coerceAtLeast(1), seleccionesDto())
            _cargando.value = false
            when (r) {
                is RespuestaRed.Exito -> emitir(EventoAsignar.Exito("Renta creada. Cobra desde la pantalla de Rentas."))
                is RespuestaRed.Fallo -> emitir(EventoAsignar.Error(r.error.mensaje))
            }
        }
    }

    fun vender() {
        val d = disfrazId ?: return
        val cli = clienteId ?: return emitir(EventoAsignar.Error("Elige el cliente."))
        val suc = sucursalId ?: return emitir(EventoAsignar.Error("Elige la sucursal."))
        viewModelScope.launch {
            _cargando.value = true
            val r = repo.venderParaCliente(d, cli, suc, cantidad.coerceAtLeast(1), seleccionesDto())
            _cargando.value = false
            when (r) {
                is RespuestaRed.Exito -> emitir(EventoAsignar.Exito("Venta creada. Cobra desde la pantalla de Ventas."))
                is RespuestaRed.Fallo -> emitir(EventoAsignar.Error(r.error.mensaje))
            }
        }
    }

    private fun emitir(evento: EventoAsignar) {
        _eventos.tryEmit(evento)
    }
}
