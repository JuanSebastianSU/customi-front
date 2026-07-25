package com.costumi.app.ui.cliente.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.local.entity.EmpresaEntity
import com.costumi.app.data.repo.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Explorar tiendas: la UI observa Room (cache-first) y este VM sincroniza de la red. Muestra la
 * caché al instante si existe; si esta vacia, Loading mientras carga y Error (con reintentar) si falla.
 */
@HiltViewModel
class ExplorarViewModel @Inject constructor(
    private val repo: MarketplaceRepository,
) : ViewModel() {

    private val cargando = MutableStateFlow(true)
    private val errorRefresco = MutableStateFlow<String?>(null)

    /** Texto que escribio el usuario; vacio = mostrar la cache completa. */
    private val texto = MutableStateFlow("")

    /** Resultados de la ultima busqueda en el servidor; null mientras no se este buscando. */
    private val resultados = MutableStateFlow<List<EmpresaEntity>?>(null)

    val estado: StateFlow<UiState<List<EmpresaEntity>>> =
        combine(repo.observarEmpresas(), resultados, cargando, errorRefresco) { cache, hallados, cargando, error ->
            // Buscando: manda lo que respondio el servidor (puede traer tiendas que no estan cacheadas).
            val lista = hallados ?: cache
            when {
                lista.isNotEmpty() -> UiState.Success(lista)
                cargando -> UiState.Loading
                error != null -> UiState.Error(error) { refrescar() }
                hallados != null -> UiState.Empty
                else -> UiState.Empty
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /**
     * El usuario escribio en la caja de busqueda. Se consulta al servidor (no la cache) porque el punto
     * de buscar es encontrar tiendas que todavia no tiene guardadas.
     */
    fun buscar(nuevoTexto: String) {
        texto.value = nuevoTexto
        val limpio = nuevoTexto.trim()
        if (limpio.isBlank()) {
            resultados.value = null
            return
        }
        viewModelScope.launch {
            cargando.value = true
            when (val r = repo.buscarEmpresas(limpio)) {
                is RespuestaRed.Exito -> {
                    // Si el usuario siguio escribiendo, esta respuesta ya no corresponde.
                    if (texto.value.trim() == limpio) resultados.value = r.data
                    errorRefresco.value = null
                }
                is RespuestaRed.Fallo -> errorRefresco.value = r.error.mensaje
            }
            cargando.value = false
        }
    }

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            cargando.value = true
            errorRefresco.value = null
            when (val r = repo.refrescarEmpresas(buscar = null)) {
                is RespuestaRed.Fallo -> errorRefresco.value = r.error.mensaje
                is RespuestaRed.Exito -> Unit
            }
            cargando.value = false
        }
    }
}
