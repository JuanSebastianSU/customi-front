package com.costumi.app.ui.gestion.inventario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.GruposStockRepository
import com.costumi.apiclient.models.AjusteDeStockRequest
import com.costumi.apiclient.models.CrearGrupoDeStockRequest
import com.costumi.apiclient.models.GrupoDeStockResponse
import com.costumi.apiclient.models.MoverUnidadesRequest
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TransferirStockRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoStock {
    data class Info(val mensaje: String) : EventoStock
    data class Error(val mensaje: String) : EventoStock
}

@HiltViewModel
class GruposStockViewModel @Inject constructor(
    private val repo: GruposStockRepository,
    private val disfrazRepo: DisfrazRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val prendaId: UUID = UUID.fromString(estado[GruposStockFragment.ARG_PRENDA_ID]!!)
    val prendaNombre: String = estado[GruposStockFragment.ARG_PRENDA_NOMBRE] ?: "Prenda"

    /** Nombres de los valores de etiqueta (talla/color), para leer la variante en cristiano. */
    private var nombreValor: Map<UUID, String> = emptyMap()

    private val _grupos = MutableStateFlow<UiState<List<GrupoDeStockResponse>>>(UiState.Loading)
    val grupos = _grupos.asStateFlow()

    private val _sucursales = MutableStateFlow<List<SucursalResponse>>(emptyList())
    val sucursales = _sucursales.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoStock>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        viewModelScope.launch {
            (repo.sucursales() as? RespuestaRed.Exito)?.let { _sucursales.value = it.data }
            // Best-effort: si falla, la variante cae a "Variante" en vez de romperse.
            (disfrazRepo.tiposConValores() as? RespuestaRed.Exito)?.let { r ->
                nombreValor = r.data.flatMap { it.valores }.mapNotNull { v -> v.id?.let { it to v.valor.orEmpty() } }.toMap()
            }
            cargarGrupos()
        }
    }

    fun nombreSucursal(id: UUID?): String =
        _sucursales.value.firstOrNull { it.id == id }?.nombre ?: "Sucursal"

    /** La variante en cristiano: "Rojo · M", o "Stock general" si no tiene combinación. */
    fun describirVariante(g: GrupoDeStockResponse): String {
        val combinacion = g.combinacion.orEmpty()
        if (combinacion.isEmpty()) return "Stock general"
        return combinacion.joinToString("  ·  ") { nombreValor[it.valorEtiquetaId] ?: "Variante" }
    }

    fun cargarGrupos() {
        viewModelScope.launch {
            _grupos.value = UiState.Loading
            _grupos.value = when (val r = repo.grupos(prendaId)) {
                is RespuestaRed.Exito -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargarGrupos() }
            }
        }
    }

    fun crearGrupo(sucursalId: UUID, cantidadInicial: Int) = ejecutar("Variante creada.") {
        repo.crearGrupo(prendaId, CrearGrupoDeStockRequest(sucursalId, null, cantidadInicial))
    }

    fun entrada(grupoId: UUID, cantidad: Int) = ejecutar("Entrada registrada.") {
        repo.entrada(grupoId, cantidad)
    }

    fun ajuste(grupoId: UUID, estado: AjusteDeStockRequest.Estado, delta: Int, motivo: String?) =
        ejecutar("Ajuste aplicado.") {
            repo.ajuste(grupoId, AjusteDeStockRequest(estado, delta, motivo))
        }

    fun mover(grupoId: UUID, desde: MoverUnidadesRequest.Desde, hacia: MoverUnidadesRequest.Hacia, cantidad: Int) =
        ejecutar("Unidades movidas.") {
            repo.mover(grupoId, MoverUnidadesRequest(desde, hacia, cantidad))
        }

    fun transferir(grupoId: UUID, sucursalDestinoId: UUID, cantidad: Int) =
        ejecutar("Stock transferido.") {
            repo.transferir(grupoId, TransferirStockRequest(sucursalDestinoId, cantidad))
        }

    fun eliminar(grupoId: UUID) {
        viewModelScope.launch {
            when (val r = repo.eliminar(grupoId)) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoStock.Info("Grupo eliminado."))
                    cargarGrupos()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoStock.Error(r.error.mensaje))
            }
        }
    }

    private fun ejecutar(exito: String, accion: suspend () -> RespuestaRed<GrupoDeStockResponse>) {
        viewModelScope.launch {
            when (val r = accion()) {
                is RespuestaRed.Exito -> {
                    _eventos.tryEmit(EventoStock.Info(exito))
                    cargarGrupos()
                }
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoStock.Error(r.error.mensaje))
            }
        }
    }
}
