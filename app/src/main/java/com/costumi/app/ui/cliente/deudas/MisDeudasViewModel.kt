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

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.mias()) {
                is RespuestaRed.Exito -> {
                    _saldoTotal.value = r.data.mapNotNull { it.saldo }
                        .fold(BigDecimal.ZERO) { acc, s -> acc + s }
                    if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                }
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }
}
