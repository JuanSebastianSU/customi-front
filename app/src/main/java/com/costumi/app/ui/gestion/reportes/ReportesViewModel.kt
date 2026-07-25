package com.costumi.app.ui.gestion.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.ReporteCompleto
import com.costumi.app.data.repo.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EventoReporte {
    /** Un exporte descargado: bytes + nombre de archivo + si es PDF (abrir) o no (compartir). */
    data class Archivo(val bytes: ByteArray, val nombre: String, val esPdf: Boolean) : EventoReporte
    data class Error(val mensaje: String) : EventoReporte
}

/** Tablero de Reportes (rol DUENO/ENCARGADO): métricas, rankings y exportes CSV/PDF. */
@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val repo: ReporteRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<ReporteCompleto>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    private val _exportando = MutableStateFlow(false)
    val exportando = _exportando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoReporte>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    // Selectores de filtro y de "ventas por etiqueta".
    private val _sucursales = MutableStateFlow<List<com.costumi.apiclient.models.SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()
    private val _tipos = MutableStateFlow<List<com.costumi.apiclient.models.TipoEtiquetaResponse>>(emptyList())
    val tipos = _tipos.asStateFlow()
    private val _porEtiqueta = MutableStateFlow<List<com.costumi.apiclient.models.ValorEtiquetaRanking>>(emptyList())
    val porEtiqueta = _porEtiqueta.asStateFlow()

    private var sucursalId: java.util.UUID? = null
    private var desde: java.time.LocalDate? = null
    private var hasta: java.time.LocalDate? = null
    private var ultimoTipoEtiqueta: java.util.UUID? = null

    init {
        cargar()
        cargarSelectores()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.reportes(sucursalId, desde, hasta)) {
                is RespuestaRed.Exito -> UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    private fun cargarSelectores() {
        viewModelScope.launch {
            (repo.sucursalesReporte() as? RespuestaRed.Exito)?.data?.let { _sucursales.value = it }
            (repo.tiposEtiqueta() as? RespuestaRed.Exito)?.data?.let { _tipos.value = it }
        }
    }

    /** Aplica el filtro de sucursal y/o rango de fechas y recarga (donde el endpoint lo soporta). */
    fun filtrar(sucursalId: java.util.UUID?, desde: java.time.LocalDate?, hasta: java.time.LocalDate?) {
        this.sucursalId = sucursalId
        this.desde = desde
        this.hasta = hasta
        cargar()
        // Si ya había una etiqueta seleccionada, recárgala con la nueva sucursal.
        ultimoTipoEtiqueta?.let { cargarPorEtiqueta(it) }
    }

    fun cargarPorEtiqueta(tipoId: java.util.UUID) {
        ultimoTipoEtiqueta = tipoId
        viewModelScope.launch {
            _porEtiqueta.value = (repo.ventasPorEtiqueta(tipoId, sucursalId) as? RespuestaRed.Exito)?.data.orEmpty()
        }
    }

    fun exportarInventario(pdf: Boolean) =
        exportar(pdf, "inventario-tablero") { repo.exportarInventario(pdf) }

    fun exportarRentasVencidas(pdf: Boolean) =
        exportar(pdf, "rentas-vencidas") { repo.exportarRentasVencidas(pdf, sucursalId) }

    private fun exportar(pdf: Boolean, base: String, accion: suspend () -> RespuestaRed<ByteArray>) {
        if (_exportando.value) return
        viewModelScope.launch {
            _exportando.value = true
            when (val r = accion()) {
                is RespuestaRed.Exito ->
                    _eventos.tryEmit(EventoReporte.Archivo(r.data, "$base.${if (pdf) "pdf" else "csv"}", pdf))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoReporte.Error(r.error.mensaje))
            }
            _exportando.value = false
        }
    }
}
