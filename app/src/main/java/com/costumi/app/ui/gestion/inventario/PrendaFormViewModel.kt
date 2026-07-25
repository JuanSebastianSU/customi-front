package com.costumi.app.ui.gestion.inventario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.InventarioRepository
import com.costumi.app.data.repo.TaxonomiaRepository
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CrearPrendaRequest
import com.costumi.apiclient.models.EditarPrendaRequest
import com.costumi.apiclient.models.EtiquetaSeleccionadaDto
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

/** Eventos del formulario de prenda. */
sealed interface EventoForm {
    /** Guardada: si {@code avisoFoto} != null, la prenda se guardó pero la foto no se pudo subir. */
    data class Guardada(val avisoFoto: String? = null) : EventoForm
    data class Error(val mensaje: String) : EventoForm
}

/**
 * Alta/edición de una prenda. En edición, el backend no permite cambiar categoría ni tipo
 * (EditarPrendaRequest solo lleva nombre/precios) — la UI los muestra de solo lectura.
 */
@HiltViewModel
class PrendaFormViewModel @Inject constructor(
    private val repo: InventarioRepository,
    private val taxonomia: TaxonomiaRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    val prendaId: String? = estado[PrendaFormFragment.ARG_ID]
    val esEdicion: Boolean = prendaId != null

    private val _categorias = MutableStateFlow<UiState<List<CategoriaResponse>>>(UiState.Loading)
    val categorias = _categorias.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoForm>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Un tipo de etiqueta activo con sus valores disponibles, para clasificar la prenda (RF-2.7). */
    data class TipoConValores(val tipo: TipoEtiquetaResponse, val valores: List<ValorEtiquetaResponse>)

    private val _etiquetas = MutableStateFlow<List<TipoConValores>>(emptyList())
    val etiquetasDisponibles = _etiquetas.asStateFlow()

    init {
        cargarCategorias()
        cargarEtiquetas()
    }

    /**
     * Carga los tipos de etiqueta activos con sus valores. Los tipos sin valores no se muestran
     * (no hay nada que elegir). La UI filtra además por la categoría elegida (RF-2.7).
     */
    private fun cargarEtiquetas() {
        viewModelScope.launch {
            val tipos = (taxonomia.tiposEtiqueta() as? RespuestaRed.Exito)?.data
                ?.filter { it.archivada != true }
                ?: return@launch
            val conValores = coroutineScope {
                tipos.map { tipo ->
                    async {
                        val valores = (tipo.id?.let { taxonomia.valores(it) } as? RespuestaRed.Exito)
                            ?.data?.filter { it.archivada != true }.orEmpty()
                        TipoConValores(tipo, valores)
                    }
                }.map { it.await() }
            }
            _etiquetas.value = conValores.filter { it.valores.isNotEmpty() }
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            _categorias.value = UiState.Loading
            _categorias.value = when (val r = repo.categorias()) {
                is RespuestaRed.Exito -> UiState.Success(r.data.filter { it.archivada != true })
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargarCategorias() }
            }
        }
    }

    /**
     * Guarda la prenda (alta o edición) y, si se eligió una foto, la sube DESPUÉS con el id resultante
     * (el endpoint de foto es por-id). La foto es best-effort: si falla, la prenda queda guardada igual
     * y se avisa (avisoFoto), sin bloquear el flujo.
     */
    fun guardar(
        nombre: String,
        categoriaId: UUID?,
        tipo: CrearPrendaRequest.TipoArticulo?,
        precioRenta: BigDecimal?,
        precioVenta: BigDecimal?,
        costo: BigDecimal?,
        deposito: BigDecimal?,
        valorReposicion: BigDecimal?,
        valorDano: BigDecimal?,
        etiquetas: List<EtiquetaSeleccionadaDto> = emptyList(),
        fotoBytes: ByteArray? = null,
        fotoMime: String? = null,
        fotoNombre: String? = null,
    ) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            val r = if (esEdicion) {
                repo.editarPrenda(
                    UUID.fromString(prendaId),
                    EditarPrendaRequest(
                        nombre = nombre,
                        precioRenta = precioRenta,
                        precioVenta = precioVenta,
                        costoAdquisicion = costo,
                        depositoSugerido = deposito,
                        valorReposicion = valorReposicion,
                        valorDano = valorDano,
                        etiquetas = etiquetas,
                    ),
                )
            } else {
                repo.crearPrenda(
                    CrearPrendaRequest(
                        categoriaId = categoriaId!!,
                        tipoArticulo = tipo!!,
                        nombre = nombre,
                        precioRenta = precioRenta,
                        precioVenta = precioVenta,
                        costoAdquisicion = costo,
                        depositoSugerido = deposito,
                        valorReposicion = valorReposicion,
                        valorDano = valorDano,
                        etiquetas = etiquetas,
                    ),
                )
            }
            if (r is RespuestaRed.Fallo) {
                _guardando.value = false
                _eventos.tryEmit(EventoForm.Error(r.error.mensaje))
                return@launch
            }
            val id = if (esEdicion) UUID.fromString(prendaId) else (r as RespuestaRed.Exito).data.id
            var avisoFoto: String? = null
            if (fotoBytes != null && id != null) {
                val f = repo.subirFoto(id, fotoBytes, fotoMime ?: "image/jpeg", fotoNombre ?: "foto.jpg")
                if (f is RespuestaRed.Fallo) avisoFoto = "Prenda guardada, pero la foto no se subio: ${f.error.mensaje}"
            }
            _guardando.value = false
            _eventos.tryEmit(EventoForm.Guardada(avisoFoto))
        }
    }
}
