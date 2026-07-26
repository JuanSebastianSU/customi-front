package com.costumi.app.ui.cliente.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.data.local.entity.FavoritoDisfrazEntity
import com.costumi.app.data.repo.FavoritosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Mis guardados": lista de disfraces favoritos del cliente (persistencia local). */
@HiltViewModel
class FavoritosViewModel @Inject constructor(
    private val repo: FavoritosRepository,
) : ViewModel() {

    val favoritos: StateFlow<List<FavoritoDisfrazEntity>> =
        repo.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Al abrir "Mis guardados" se baja la lista de la cuenta (sync entre dispositivos).
        viewModelScope.launch { repo.sincronizar() }
    }

    fun quitar(f: FavoritoDisfrazEntity) {
        viewModelScope.launch { repo.alternar(f, esFavorito = true) }
    }
}
