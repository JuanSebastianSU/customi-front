package com.costumi.app.ui.cliente.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.repo.PedidoRepository
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.SucursalVitrinaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DetallePrendaViewModel @Inject constructor(
    private val repo: PedidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val empresaId: String = savedStateHandle[DetallePrendaFragment.ARG_EMPRESA_ID] ?: ""
    private val prendaId: String = savedStateHandle[DetallePrendaFragment.ARG_PRENDA_ID] ?: ""
    val nombre: String = savedStateHandle[DetallePrendaFragment.ARG_NOMBRE] ?: "Articulo"
    val categoria: String? = savedStateHandle[DetallePrendaFragment.ARG_CATEGORIA]
    val fotoUrl: String? = savedStateHandle[DetallePrendaFragment.ARG_FOTO_URL]

    /** Etiquetas ya formateadas ("Color: Negro · Talla: M") para que el cliente sepa qué es. */
    val etiquetas: String? = savedStateHandle[DetallePrendaFragment.ARG_ETIQUETAS]

    private val precioRenta = savedStateHandle.get<String>(DetallePrendaFragment.ARG_PRECIO_RENTA)
        ?.toBigDecimalOrNull()
    private val precioVenta = savedStateHandle.get<String>(DetallePrendaFragment.ARG_PRECIO_VENTA)
        ?.toBigDecimalOrNull()

    val permiteRenta: Boolean = precioRenta != null
    val permiteVenta: Boolean = precioVenta != null
    // En renta el precio es POR DÍA; se aclara en la etiqueta para no confundir al cliente.
    val precioRentaTexto: String? = precioRenta.comoPrecio()?.let { "$it / dia" }
    val precioVentaTexto: String? = precioVenta.comoPrecio()

    // Sucursales (puntos de retiro) de la tienda. El cliente elige dónde retirar si hay más de una.
    private val _sucursales = MutableStateFlow<List<SucursalVitrinaResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    private val _sucursalSeleccionada = MutableStateFlow<SucursalVitrinaResponse?>(null)
    val sucursalSeleccionada = _sucursalSeleccionada.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDetalle>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargarSucursales()
    }

    private fun cargarSucursales() {
        viewModelScope.launch {
            when (val r = repo.sucursales(empresaId)) {
                is RespuestaRed.Exito -> {
                    _sucursales.value = r.data
                    _sucursalSeleccionada.value = r.data.firstOrNull()
                }
                is RespuestaRed.Fallo -> Unit // al agregar se avisará si no hay sucursal
            }
        }
    }

    fun seleccionarSucursal(id: String) {
        _sucursalSeleccionada.value = _sucursales.value.firstOrNull { it.id?.toString() == id }
    }

    /**
     * Fechas elegidas. Viven en el ViewModel y no en el Fragment porque la vista se recrea al volver
     * atras: alli se perdian y la pantalla parecia haberlas "desmarcado".
     */
    var fechaRetiro: LocalDate? = null
    var fechaDevolucion: LocalDate? = null

    fun agregar(
        tipo: AgregarItemRequest.Tipo,
        cantidad: Int,
        fechaRetiro: LocalDate?,
        fechaDevolucion: LocalDate?,
    ) {
        if (_cargando.value) return
        if (tipo == AgregarItemRequest.Tipo.RENTA && (fechaRetiro == null || fechaDevolucion == null)) {
            _eventos.tryEmit(EventoDetalle.Error("Elige las fechas de retiro y devolucion."))
            return
        }
        val suc = _sucursalSeleccionada.value?.id?.toString()
        if (suc == null) {
            _eventos.tryEmit(EventoDetalle.Error("Esta tienda aun no puede recibir pedidos."))
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            val r = repo.agregarAlCarrito(
                empresaId = empresaId,
                sucursalId = suc,
                prendaId = prendaId,
                tipo = tipo,
                cantidad = cantidad.coerceAtLeast(1),
                fechaRetiro = fechaRetiro,
                fechaDevolucion = fechaDevolucion,
            )
            when (r) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoDetalle.Agregado(suc, tipo))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoDetalle.Error(r.error.mensaje))
            }
            _cargando.value = false
        }
    }
}
