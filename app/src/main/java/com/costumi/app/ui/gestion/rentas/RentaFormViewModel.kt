package com.costumi.app.ui.gestion.rentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.RentaRepository
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.CrearRentaRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.ItemDisfrazDto
import com.costumi.apiclient.models.LineaPrendaRentaDto
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class DatosRenta(
    val sucursales: List<SucursalResponse>,
    val clientes: List<ClienteResponse>,
    val prendas: List<PrendaResponse>,
    val disfraces: List<DisfrazResponse>,
    // Para los filtros del catalogo visual de prendas (los mismos que en el punto de venta).
    val categorias: List<com.costumi.apiclient.models.CategoriaResponse>,
    val etiquetas: List<com.costumi.app.data.repo.TipoConValores>,
)

sealed interface EventoRentaForm {
    data class Registrada(val importe: BigDecimal?) : EventoRentaForm
    data class Error(val mensaje: String) : EventoRentaForm
}

/** Alta de renta: carga sucursales/clientes/prendas y registra (idempotente por clave estable). */
@HiltViewModel
class RentaFormViewModel @Inject constructor(
    private val repo: RentaRepository,
    private val disfrazRepo: DisfrazRepository,
    private val pedido: PedidoDisfracesStore,
) : ViewModel() {

    val claveIdempotencia: String = UUID.randomUUID().toString()

    private val _datos = MutableStateFlow<UiState<DatosRenta>>(UiState.Loading)
    val datos = _datos.asStateFlow()

    private val _registrando = MutableStateFlow(false)
    val registrando = _registrando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoRentaForm>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Disfraces agregados a esta renta (carrito compartido con el asignador). */
    val disfracesPedido = pedido.items

    init {
        // Cada "Nueva renta" empieza sin disfraces del pedido anterior.
        pedido.limpiar()
        cargar()
    }

    fun quitarDisfraz(indice: Int) = pedido.quitar(indice)

    /** Catalogo del inventario (con foto y stock) para el selector visual de prendas. */
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
            // Los disfraces son opcionales: si fallan, se puede rentar solo prendas.
            val dis = (disfrazRepo.disfraces() as? RespuestaRed.Exito)?.data.orEmpty()
                .filter { it.activo == true }
            // Categorias y etiquetas para los filtros del catalogo (best-effort).
            val categorias = (disfrazRepo.categoriasParaPool() as? RespuestaRed.Exito)?.data.orEmpty()
            val etiquetas = (disfrazRepo.tiposConValores() as? RespuestaRed.Exito)?.data.orEmpty()
            _datos.value = UiState.Success(
                DatosRenta(
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
        clienteId: UUID,
        fechaRetiro: LocalDate,
        fechaDevolucion: LocalDate,
        lineas: List<com.costumi.apiclient.models.LineaRentaDto>,
        deposito: BigDecimal?,
    ) {
        if (_registrando.value) return
        viewModelScope.launch {
            _registrando.value = true
            val req = CrearRentaRequest(
                sucursalId = sucursalId,
                clienteId = clienteId,
                fechaRetiro = fechaRetiro,
                fechaDevolucion = fechaDevolucion,
                deposito = deposito,
                claveIdempotencia = claveIdempotencia,
                lineas = lineas,
                prendaId = null,
                precioPorDia = null,
            )
            val r = repo.crear(req)
            _registrando.value = false
            _eventos.tryEmit(
                when (r) {
                    is RespuestaRed.Exito -> EventoRentaForm.Registrada(r.data.importe)
                    is RespuestaRed.Fallo -> EventoRentaForm.Error(r.error.mensaje)
                },
            )
        }
    }

    /**
     * Registra un pedido MIXTO en una sola renta: prendas sueltas ({@code lineas}) y/o disfraces
     * ({@code items}) al mismo cliente. Se usa cuando la renta incluye al menos un disfraz.
     */
    fun registrarMixto(
        sucursalId: UUID,
        clienteId: UUID,
        fechaRetiro: LocalDate,
        fechaDevolucion: LocalDate,
        lineas: List<LineaPrendaRentaDto>,
        items: List<ItemDisfrazDto>,
    ) {
        if (_registrando.value) return
        viewModelScope.launch {
            _registrando.value = true
            val r = disfrazRepo.rentarVarios(clienteId, sucursalId, fechaRetiro, fechaDevolucion, items, lineas)
            _registrando.value = false
            when (r) {
                is RespuestaRed.Exito -> {
                    pedido.limpiar()
                    _eventos.tryEmit(EventoRentaForm.Registrada(null))
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoRentaForm.Error(r.error.mensaje))
            }
        }
    }

    /** Los disfraces (por id) del pedido en curso, para construir los {@code ItemDisfrazDto}. */
    fun itemsDisfrazDelPedido(): List<ItemDisfrazDto> = pedido.items.value.map {
        ItemDisfrazDto(disfrazId = it.disfrazId, cantidad = it.cantidad, selecciones = it.selecciones)
    }
}
