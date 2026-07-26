package com.costumi.app.ui.gestion.reembolsos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ArticuloConcepto
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.ReembolsoRepository
import com.costumi.app.data.repo.RentaRepository
import com.costumi.app.data.repo.VentaRepository
import com.costumi.app.data.repo.TipoConcepto
import com.costumi.app.core.mapear
import com.costumi.apiclient.models.DevolverVentaRequest
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoReembolso {
    data class Info(val mensaje: String) : EventoReembolso
    data class Error(val mensaje: String) : EventoReembolso
    data class Detalle(val solicitud: SolicitudDeReembolsoResponse, val articulos: List<ArticuloConcepto>) : EventoReembolso
}

/** Bandeja de solicitudes de reembolso: listar y decidir (aprobar/rechazar). */
@HiltViewModel
class ReembolsosViewModel @Inject constructor(
    private val repo: ReembolsoRepository,
    private val pagoRepo: PagoRepository,
    private val ventaRepo: VentaRepository,
    private val rentaRepo: RentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<SolicitudDeReembolsoResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoReembolso>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Texto de busqueda vigente; null = sin filtrar. */
    private var buscar: String? = null

    /** Pestaña activa. La bandeja NO es paginada (lista completa), así que se filtra en la app. */
    enum class Filtro { PENDIENTES, RESUELTAS, TODAS }
    private var filtro = Filtro.PENDIENTES

    /** Lo último que trajo el backend, sin filtrar; se re-filtra sin volver a pedir. */
    private var todas: List<SolicitudDeReembolsoResponse> = emptyList()

    // El init va DESPUÉS de declarar el estado y los filtros: sus inicializadores deben correr antes de que
    // cargar()/publicar() los usen (si no, un cargar() síncrono los encontraría sin inicializar).
    init {
        cargar()
    }

    /** El usuario escribio en la caja de busqueda: se guarda y se recarga la lista. */
    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    /** Cambia de pestaña sin ir a la red. */
    fun filtrar(f: Filtro) {
        filtro = f
        publicar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = repo.bandeja(buscar)) {
                is RespuestaRed.Exito -> { todas = r.data; publicar() }
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    /** Aplica la pestaña sobre lo ya traído y lo emite (pendientes primero, más recientes arriba). */
    private fun publicar() {
        val visibles = todas.filter { s ->
            when (filtro) {
                Filtro.PENDIENTES -> s.estado?.value == "PENDIENTE"
                Filtro.RESUELTAS -> s.estado?.value != "PENDIENTE"
                Filtro.TODAS -> true
            }
        }.sortedWith(
            compareByDescending<SolicitudDeReembolsoResponse> { it.estado?.value == "PENDIENTE" }
                .thenByDescending { it.creadaEn },
        )
        _estado.value = if (visibles.isEmpty()) UiState.Empty else UiState.Success(visibles)
    }

    /** Carga los artículos (con foto) de la operación reembolsada para mostrarlos en un desglose. */
    fun verDetalle(s: SolicitudDeReembolsoResponse) {
        val conceptoId = s.conceptoId ?: return
        val tipo = when (s.tipoConcepto?.value?.uppercase()) {
            "RENTA" -> TipoConcepto.RENTA
            "VENTA" -> TipoConcepto.VENTA
            else -> null
        } ?: return
        viewModelScope.launch {
            val arts = (pagoRepo.articulos(tipo, conceptoId) as? RespuestaRed.Exito)?.data.orEmpty()
            _eventos.tryEmit(EventoReembolso.Detalle(s, arts))
        }
    }

    fun aprobar(id: UUID, motivo: String) = decidir(aprobar = true, id, motivo)
    fun rechazar(id: UUID, motivo: String) = decidir(aprobar = false, id, motivo)

    /**
     * Registra la devolucion de la venta/renta de la solicitud, que es la precondicion para aprobar el
     * reembolso (el backend responde 409 "el item aun no fue devuelto" si no esta).
     *
     * Antes esto obligaba a salir a Ventas o Rentas, encontrar la operacion y devolverla ahi, sin que
     * nada lo indicara: desde la bandeja solo se podia rechazar. Se devuelve **todo lo pendiente** (asi
     * lo interpreta el backend cuando no se detallan lineas); para devolver solo algunas unidades sigue
     * estando el dialogo de Ventas.
     */
    fun registrarDevolucion(s: SolicitudDeReembolsoResponse) {
        val concepto = s.conceptoId ?: return
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val r = if (s.tipoConcepto?.value == "RENTA") {
                rentaRepo.devolver(concepto).mapear { }
            } else {
                ventaRepo.devolver(concepto, DevolverVentaRequest(lineas = emptyList())).mapear { }
            }
            when (r) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoReembolso.Info("Devolucion registrada. Ya se puede aprobar el reembolso."))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoReembolso.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }

    private fun decidir(aprobar: Boolean, id: UUID, motivo: String) {
        if (_procesando.value) return
        viewModelScope.launch {
            _procesando.value = true
            val r = if (aprobar) repo.aprobar(id, motivo) else repo.rechazar(id, motivo)
            when (r) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoReembolso.Info(if (aprobar) "Reembolso aprobado." else "Solicitud rechazada."))
                    cargar()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoReembolso.Error(r.error.mensaje))
            }
            _procesando.value = false
        }
    }
}
