package com.costumi.app.ui.gestion.auditoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.AuditoriaRepository
import com.costumi.apiclient.models.AuditoriaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Auditoria: trail de eventos de la empresa, solo lectura. */
@HiltViewModel
class AuditoriaViewModel @Inject constructor(
    private val repo: AuditoriaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<AuditoriaResponse>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    /** Categorías presentes (por la primera palabra de la acción: VENTA, RENTA, PRENDA...), para los chips. */
    private val _categorias = MutableStateFlow<List<String>>(emptyList())
    val categorias = _categorias.asStateFlow()

    /** Texto de busqueda vigente; null = sin filtrar. */
    private var buscar: String? = null

    /** Categoría de acción elegida (null = todas). El registro NO es paginado, así que se filtra en la app. */
    private var categoria: String? = null

    /** Lo último traído, sin filtrar por categoría (el buscar sí va al servidor). */
    private var todas: List<AuditoriaResponse> = emptyList()

    // El init va DESPUÉS de declarar el estado y los filtros: sus inicializadores deben correr antes de que
    // cargar()/publicar() los usen (si no, un cargar() síncrono los encontraría sin inicializar).
    init {
        cargar()
    }

    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    /** Filtra por categoría de acción sin volver a la red. */
    fun filtrar(cat: String?) {
        categoria = cat
        publicar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            when (val r = repo.registros(buscar)) {
                is RespuestaRed.Exito -> {
                    todas = r.data.sortedByDescending { it.fecha }
                    // Las categorías salen de los datos: no se hardcodean, así aparecen todas las que haya.
                    _categorias.value = todas.mapNotNull { categoriaDe(it.accion) }.distinct().sorted()
                    publicar()
                }
                is RespuestaRed.Fallo -> _estado.value = UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }

    private fun publicar() {
        val visibles = if (categoria == null) todas
        else todas.filter { categoriaDe(it.accion) == categoria }
        _estado.value = if (visibles.isEmpty()) UiState.Empty else UiState.Success(visibles)
    }

    /** La categoría de un evento = la primera palabra de la acción ("VENTA_DEVUELTA" → "Ventas"). */
    private fun categoriaDe(accion: String?): String? =
        accion?.substringBefore('_')?.takeIf { it.isNotBlank() }
            ?.lowercase()?.replaceFirstChar { it.uppercase() }
}
