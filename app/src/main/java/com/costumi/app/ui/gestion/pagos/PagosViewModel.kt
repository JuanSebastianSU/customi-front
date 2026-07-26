package com.costumi.app.ui.gestion.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.TipoConcepto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Selector de operaciones a cobrar: alterna entre Ventas y Rentas, lista paginada. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PagosViewModel @Inject constructor(
    private val repo: PagoRepository,
) : ViewModel() {

    private val _tipo = MutableStateFlow(TipoConcepto.VENTA)
    val tipo = _tipo.asStateFlow()

    /** Codigo de retiro que escribio el usuario; null = sin filtrar. */
    private val _buscar = MutableStateFlow<String?>(null)

    /** Estado por el que filtrar (en MAYÚSCULAS como el backend); null = todos. */
    private val _estado = MutableStateFlow<String?>(null)
    val estado = _estado.asStateFlow()

    val operaciones = combine(_tipo, _buscar, _estado) { tipo, buscar, estado -> Triple(tipo, buscar, estado) }
        .flatMapLatest { (tipo, buscar, estado) ->
            repo.operaciones(tipo, buscar).map { pd ->
                if (estado == null) pd else pd.filter { it.estado?.uppercase() == estado }
            }
        }
        .cachedIn(viewModelScope)

    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    fun cambiarTipo(tipo: TipoConcepto) {
        if (_tipo.value != tipo) { _tipo.value = tipo; _estado.value = null }
    }

    fun filtrarEstado(estado: String?) {
        _estado.value = estado
    }

    /** Estados posibles según el tipo activo, para pintar los chips de filtro. */
    fun estadosDe(tipo: TipoConcepto): List<String> = if (tipo == TipoConcepto.VENTA) {
        listOf("CONFIRMADA", "PARCIALMENTE_DEVUELTA", "DEVUELTA")
    } else {
        listOf("RESERVADA", "ACTIVA", "DEVUELTA", "CERRADA", "CANCELADA")
    }
}
