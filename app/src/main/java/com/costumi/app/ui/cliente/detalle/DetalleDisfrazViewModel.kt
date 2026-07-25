package com.costumi.app.ui.cliente.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.app.data.repo.FavoritosRepository
import com.costumi.app.data.repo.MarketplaceRepository
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.FacetaDto
import com.costumi.apiclient.models.OpcionDto
import com.costumi.apiclient.models.SeleccionSlotDto
import com.costumi.apiclient.models.SlotDto
import com.costumi.apiclient.models.SlotOpcionesResponse
import com.costumi.apiclient.models.SucursalVitrinaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Datos de un slot para armar el disfraz: sus opciones ("ruleta") y la seleccion por defecto. */
data class SlotUi(
    val orden: Int,
    val nombre: String,
    val personalizable: Boolean,
    val opcional: Boolean,
    val opciones: List<OpcionDto>,
    val facetas: List<FacetaDto>,
    val seleccionInicial: UUID?,
)

/** Estado del detalle del disfraz listo para armar. */
data class DetalleUi(
    val disfraz: DisfrazResponse,
    val disponible: Boolean,
    val slots: List<SlotUi>,
)

/**
 * Detalle de un disfraz para el cliente: carga sus slots y las opciones de cada uno (la "ruleta"),
 * lleva la seleccion por slot y crea la renta/venta a nombre del cliente antes de ir al pago.
 */
@HiltViewModel
class DetalleDisfrazViewModel @Inject constructor(
    private val repo: MarketplaceRepository,
    private val pedidos: PedidoRepository,
    private val favoritos: FavoritosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val nombre: String = savedStateHandle[DetalleDisfrazFragment.ARG_NOMBRE] ?: "Disfraz"
    private val empresaId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[DetalleDisfrazFragment.ARG_EMPRESA_ID] ?: "") }.getOrNull()
    private val disfrazId: UUID? =
        runCatching { UUID.fromString(savedStateHandle[DetalleDisfrazFragment.ARG_DISFRAZ_ID] ?: "") }.getOrNull()

    private val _estado = MutableStateFlow<UiState<DetalleUi>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDisfraz>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    private val _sucursales = MutableStateFlow<List<SucursalVitrinaResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    private val _sucursalSeleccionada = MutableStateFlow<SucursalVitrinaResponse?>(null)
    val sucursalSeleccionada = _sucursalSeleccionada.asStateFlow()

    /** Seleccion actual por slot (orden -> prendaId). Se inicializa con la primera opcion de cada slot. */
    private val selecciones = mutableMapOf<Int, UUID?>()

    /** true/false reactivo de si este disfraz está en "Mis guardados" (para pintar el corazón). */
    val esFavorito: kotlinx.coroutines.flow.StateFlow<Boolean> =
        (if (disfrazId != null) favoritos.esFavorito(disfrazId.toString()) else kotlinx.coroutines.flow.flowOf(false))
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), false)

    init {
        cargar()
        cargarSucursales()
    }

    /** Guarda o quita el disfraz de "Mis guardados" (persistencia local). */
    fun alternarFavorito() {
        val d = disfrazId ?: return
        val e = empresaId ?: return
        val disfraz = (_estado.value as? UiState.Success)?.data?.disfraz
        val snapshot = FavoritoDisfrazEntity(
            disfrazId = d.toString(),
            empresaId = e.toString(),
            nombre = nombre,
            fotoUrl = disfraz?.fotoUrl,
            precioRenta = (disfraz?.precioRentaGeneral ?: disfraz?.precioRentaSugerido).comoPrecio(),
            precioVenta = (disfraz?.precioVentaGeneral ?: disfraz?.precioVentaSugerido).comoPrecio(),
            guardadoEn = System.currentTimeMillis(),
        )
        viewModelScope.launch { favoritos.alternar(snapshot, esFavorito.value) }
    }

    fun cargar() {
        val e = empresaId
        val d = disfrazId
        if (e == null || d == null) {
            _estado.value = UiState.Error("Disfraz no valido.") {}
            return
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val det = repo.disfrazDetalle(e, d)) {
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(det.error.mensaje) { cargar() }
                is RespuestaRed.Exito -> {
                    val disfraz = det.data.disfraz
                    if (disfraz == null) {
                        _estado.value = UiState.Error("Disfraz no encontrado.") { cargar() }
                        return@launch
                    }
                    val slotsUi = mutableListOf<SlotUi>()
                    selecciones.clear()
                    for (slot in disfraz.slots.orEmpty().sortedBy { it.orden ?: 0 }) {
                        val orden = slot.orden ?: continue
                        val rueda = (repo.opcionesDeSlot(e, d, orden) as? RespuestaRed.Exito)?.data
                        val opciones = rueda?.opciones.orEmpty()
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
                                facetas = rueda?.facetas.orEmpty(),
                                seleccionInicial = defecto,
                            ),
                        )
                    }
                    _estado.value = UiState.Success(DetalleUi(disfraz, det.data.disponible ?: false, slotsUi))
                }
            }
        }
    }

    private fun cargarSucursales() {
        val e = empresaId ?: return
        viewModelScope.launch {
            (repo.sucursales(e) as? RespuestaRed.Exito)?.data?.let { lista ->
                _sucursales.value = lista
                _sucursalSeleccionada.value = lista.firstOrNull()
            }
        }
    }

    fun seleccionarSucursal(id: String) {
        _sucursalSeleccionada.value = _sucursales.value.firstOrNull { it.id?.toString() == id }
    }

    /**
     * Re-consulta la ruleta de un slot aplicando el filtro por facetas ({@code valores}). Devuelve las
     * opciones que quedan y las facetas con su conteo dinámico, o `null` si falla. La hoja de selección la
     * llama al togglear un chip; vacío = sin filtro.
     */
    suspend fun opcionesFiltradas(orden: Int, valores: List<UUID>): SlotOpcionesResponse? {
        val e = empresaId ?: return null
        val d = disfrazId ?: return null
        return (repo.opcionesDeSlot(e, d, orden, valores.ifEmpty { null }) as? RespuestaRed.Exito)?.data
    }

    /** La prenda elegida para ese slot, si ya hay una. La usa la lista de piezas para pintar la fila. */
    fun seleccionDe(orden: Int): UUID? = selecciones[orden]

    fun seleccionar(orden: Int, prendaId: UUID?) {
        selecciones[orden] = prendaId
    }

    private fun seleccionesDto(): List<SeleccionSlotDto> =
        selecciones.mapNotNull { (orden, prendaId) -> prendaId?.let { SeleccionSlotDto(it, orden) } }

    /**
     * Agrega el disfraz armado (con su elección por slot) al CARRITO del cliente. Así puede combinarlo con
     * otros artículos y ver el total antes de confirmar. En RENTA lleva su periodo; en VENTA no.
     */
    /**
     * Fechas elegidas. Viven en el ViewModel y no en el Fragment porque la vista se recrea al volver
     * atras: alli se perdian y la pantalla parecia haberlas "desmarcado".
     */
    var fechaRetiro: LocalDate? = null
    var fechaDevolucion: LocalDate? = null

    fun agregarAlCarrito(tipo: AgregarItemRequest.Tipo, cantidad: Int, retiro: LocalDate?, devolucion: LocalDate?) {
        val e = empresaId ?: return
        val d = disfrazId ?: return
        val suc = _sucursalSeleccionada.value?.id
        if (suc == null) {
            emitir(EventoDisfraz.Error("Elige una sucursal de retiro."))
            return
        }
        if (tipo == AgregarItemRequest.Tipo.RENTA && (retiro == null || devolucion == null)) {
            emitir(EventoDisfraz.Error("Elige las fechas de renta."))
            return
        }
        // Una pieza obligatoria sin ninguna opción con stock no permite armar el disfraz: se avisa claro.
        val sinOpcion = (_estado.value as? UiState.Success)?.data?.slots
            ?.firstOrNull { !it.opcional && selecciones[it.orden] == null }
        if (sinOpcion != null) {
            emitir(EventoDisfraz.Error("\"${sinOpcion.nombre}\" no tiene opciones con stock ahora."))
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            val esRenta = tipo == AgregarItemRequest.Tipo.RENTA
            val r = pedidos.agregarDisfrazAlCarrito(
                empresaId = e.toString(),
                sucursalId = suc.toString(),
                disfrazId = d.toString(),
                tipo = tipo,
                selecciones = seleccionesDto(),
                cantidad = cantidad,
                fechaRetiro = if (esRenta) retiro else null,
                fechaDevolucion = if (esRenta) devolucion else null,
            )
            _cargando.value = false
            when (r) {
                is RespuestaRed.Fallo -> emitir(EventoDisfraz.Error(r.error.mensaje))
                is RespuestaRed.Exito -> emitir(EventoDisfraz.Agregado(suc.toString(), e.toString(), tipo))
            }
        }
    }

    private fun emitir(evento: EventoDisfraz) {
        _eventos.tryEmit(evento)
    }
}
