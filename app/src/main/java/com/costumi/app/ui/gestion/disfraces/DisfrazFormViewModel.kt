package com.costumi.app.ui.gestion.disfraces

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DisfrazRepository
import com.costumi.app.data.repo.TipoConValores
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.costumi.apiclient.models.CrearDisfrazRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoDisfrazForm {
    /** Guardado: si {@code avisoFoto} != null, se guardó pero la foto no se pudo subir. */
    data class Guardado(val avisoFoto: String? = null) : EventoDisfrazForm
    data class Error(val mensaje: String) : EventoDisfrazForm
}

/** Datos que el formulario necesita: prendas (slot FIJA), categorías (pool) y el disfraz a editar. */
data class DatosDisfraz(
    val prendas: List<PrendaResponse>,
    /** Categorías de PRENDA (para filtrar el picker de elementos de un slot). */
    val categorias: List<CategoriaResponse>,
    /** Categorías de DISFRAZ (para la categoría propia del disfraz). */
    val categoriasDisfraz: List<CategoriaDeDisfrazResponse>,
    val etiquetas: List<TipoConValores>,
    val disfraz: DisfrazResponse?,
)

/** Alta/edición de disfraz: carga prendas + categorías (+ el disfraz si edita) y lo guarda. */
@HiltViewModel
class DisfrazFormViewModel @Inject constructor(
    private val repo: DisfrazRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    val disfrazId: String? = estado[DisfrazFormFragment.ARG_ID]
    val esEdicion: Boolean = disfrazId != null

    private val _datos = MutableStateFlow<UiState<DatosDisfraz>>(UiState.Loading)
    val datos = _datos.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoDisfrazForm>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _datos.value = UiState.Loading
            val prendas = repo.prendasParaSelector()
            if (prendas is RespuestaRed.Fallo) { _datos.value = errorReintentable(prendas); return@launch }
            val categorias = repo.categoriasParaPool()
            if (categorias is RespuestaRed.Fallo) { _datos.value = errorReintentable(categorias); return@launch }
            // Categorías de disfraz (propias). Si falla, no bloquea el alta: se sigue con lista vacía.
            val categoriasDisfraz = (repo.categoriasDeDisfraz() as? RespuestaRed.Exito)?.data
                ?.filter { it.archivada != true }.orEmpty()
            // Etiquetas para restringir pools; si falla, se sigue sin restricciones (no bloquea el alta).
            val etiquetas = (repo.tiposConValores() as? RespuestaRed.Exito)?.data.orEmpty()
            val disfraz = if (esEdicion) {
                when (val d = repo.disfraz(UUID.fromString(disfrazId))) {
                    is RespuestaRed.Fallo -> { _datos.value = errorReintentable(d); return@launch }
                    is RespuestaRed.Exito -> d.data
                }
            } else {
                null
            }
            _datos.value = UiState.Success(
                DatosDisfraz(
                    prendas = (prendas as RespuestaRed.Exito).data,
                    categorias = (categorias as RespuestaRed.Exito).data,
                    categoriasDisfraz = categoriasDisfraz,
                    etiquetas = etiquetas,
                    disfraz = disfraz,
                ),
            )
        }
    }

    private fun errorReintentable(fallo: RespuestaRed.Fallo): UiState.Error =
        UiState.Error(fallo.error.mensaje) { cargar() }

    /**
     * Catálogo de prendas (con stock) para elegir las opciones de una parte, filtrado por categoría y por
     * valores de etiqueta (`"tipoId:valorId"`; AND entre dimensiones, OR dentro).
     */
    suspend fun catalogo(
        categoriaId: UUID?,
        etiquetas: List<String>? = null,
    ): List<com.costumi.apiclient.models.PrendaDeCatalogoResponse> =
        (repo.catalogoInventario(categoriaId, etiquetas) as? RespuestaRed.Exito)?.data.orEmpty()

    /**
     * Guarda el disfraz y, si se eligió una foto, la sube DESPUÉS con el id resultante (el endpoint de
     * foto es por-id). La foto es best-effort: si falla, el disfraz queda guardado igual y se avisa.
     */
    fun guardar(
        req: CrearDisfrazRequest,
        fotoBytes: ByteArray? = null,
        fotoMime: String? = null,
        fotoNombre: String? = null,
    ) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            val r = if (esEdicion) repo.editar(UUID.fromString(disfrazId), req) else repo.crear(req)
            if (r is RespuestaRed.Fallo) {
                _guardando.value = false
                _eventos.tryEmit(EventoDisfrazForm.Error(r.error.mensaje))
                return@launch
            }
            val id = if (esEdicion) UUID.fromString(disfrazId) else (r as RespuestaRed.Exito).data.id
            var avisoFoto: String? = null
            if (fotoBytes != null && id != null) {
                val f = repo.subirFoto(id, fotoBytes, fotoMime ?: "image/jpeg", fotoNombre ?: "disfraz.jpg")
                if (f is RespuestaRed.Fallo) avisoFoto = "Disfraz guardado, pero la foto no se subio: ${f.error.mensaje}"
            }
            _guardando.value = false
            _eventos.tryEmit(EventoDisfrazForm.Guardado(avisoFoto))
        }
    }
}
