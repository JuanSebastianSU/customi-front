package com.costumi.app.ui.cliente.deudas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.data.repo.MisDeudasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Multas y saldos del propio cliente (RF-7/11.5).
 *
 * El estado de cuenta que ya existia es por empresa y lo mira la tienda: el cliente no tenia forma de ver
 * que le cobraron ni por que.
 */
@HiltViewModel
class MisDeudasViewModel @Inject constructor(
    private val repo: MisDeudasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<MiDeudaDto>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    /** Total que el cliente debe ahora mismo, sumando todas las tiendas. */
    private val _saldoTotal = MutableStateFlow(BigDecimal.ZERO)
    val saldoTotal = _saldoTotal.asStateFlow()

    /** true tras el primer refresco: recién ahí una caché vacía significa "no debes nada" (Empty). */
    private var yaRefresco = false

    init {
        // Cache-first: la pantalla se pinta desde Room y se actualiza sola cuando el refresco escribe.
        // El saldo es informativo (N3): el importe se reconfirma con el servidor al momento de pagar.
        observar()
        cargar()
    }

    private fun observar() {
        viewModelScope.launch {
            repo.observarDeudas().collect { lista ->
                _saldoTotal.value = lista.mapNotNull { it.saldo }.fold(BigDecimal.ZERO) { acc, s -> acc + s }
                if (lista.isNotEmpty()) _estado.value = UiState.Success(lista)
                else if (yaRefresco) _estado.value = UiState.Empty
            }
        }
    }

    /** Refresca desde la red hacia Room. No tapa la caché con un spinner: solo Loading/Error si aún no hay datos. */
    fun cargar() {
        viewModelScope.launch {
            if (_estado.value !is UiState.Success) _estado.value = UiState.Loading
            when (val r = repo.refrescarDeudas()) {
                is RespuestaRed.Exito -> yaRefresco = true // el Flow de Room actualiza la lista
                is RespuestaRed.Fallo -> {
                    yaRefresco = true
                    if (_estado.value !is UiState.Success) _estado.value = UiState.Error(r.error.mensaje) { cargar() }
                }
            }
        }
    }
}
