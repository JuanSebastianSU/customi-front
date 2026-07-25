package com.costumi.app.ui.cliente.carrito

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.remote.CarritoAbiertoDto
import com.costumi.app.data.repo.PedidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Los carritos que el cliente dejo abiertos, en cualquier tienda (RF-16.2/16.3).
 *
 * Antes no habia forma de volver a un carrito: la pantalla del carrito exige empresa, sucursal y tipo, y
 * solo se llegaba a ella justo despues de agregar un articulo. Al cambiar de tienda habia que agregar
 * algo otra vez para reencontrarlo.
 */
@HiltViewModel
class MisCarritosViewModel @Inject constructor(
    private val repo: PedidoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<List<CarritoAbiertoDto>>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.misCarritos()) {
                is RespuestaRed.Exito ->
                    if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
    }
}
