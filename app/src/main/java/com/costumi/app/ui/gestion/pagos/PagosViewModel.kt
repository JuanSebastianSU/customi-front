package com.costumi.app.ui.gestion.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.data.repo.OperacionPago
import com.costumi.app.data.repo.PagoRepository
import com.costumi.app.data.repo.TipoConcepto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    val operaciones = kotlinx.coroutines.flow.combine(_tipo, _buscar) { tipo, buscar -> tipo to buscar }
        .flatMapLatest { (tipo, buscar) -> repo.operaciones(tipo, buscar) }
        .cachedIn(viewModelScope)

    fun buscar(texto: String) {
        _buscar.value = texto.trim().ifBlank { null }
    }

    fun cambiarTipo(tipo: TipoConcepto) {
        if (_tipo.value != tipo) _tipo.value = tipo
    }
}
