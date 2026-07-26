package com.costumi.app.ui.gestion.devoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.DevolucionRepository
import com.costumi.apiclient.models.DevolucionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Una devolución con el cliente y el código de la renta ya resueltos (para mostrar "de quién es"). */
data class DevolucionUi(
    val dev: DevolucionResponse,
    val codigoRetiro: String?,
    val clienteNombre: String?,
)

/** Historial de devoluciones liquidadas de la empresa (solo lectura). */
@HiltViewModel
class DevolucionesViewModel @Inject constructor(
    private val repo: DevolucionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<DevolucionUi>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    /** Texto de busqueda vigente; null = sin filtrar. */
    private var buscar: String? = null

    // El init va DESPUÉS de declarar el estado y `buscar`: su inicializador debe correr antes de que
    // cargar() lo use (si no, un cargar() síncrono lo encontraría sin inicializar).
    init {
        cargar()
    }

    /** El usuario escribio en la caja de busqueda: se guarda y se recarga la lista. */
    fun buscar(texto: String) {
        buscar = texto.trim().ifBlank { null }
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.historial(buscar)) {
                is RespuestaRed.Exito -> {
                    if (r.data.isEmpty()) {
                        UiState.Empty
                    } else {
                        val info = repo.infoRentas() // rentaId → (código, cliente)
                        // Recencia: más reciente primero por cuándo se REGISTRÓ la devolución, no por id.
                        val items = r.data.sortedByDescending { it.registradaEn }.map { d ->
                            val ri = d.rentaId?.toString()?.let { info[it] }
                            DevolucionUi(d, codigoRetiro = ri?.first, clienteNombre = ri?.second)
                        }
                        UiState.Success(items)
                    }
                }
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }
}
