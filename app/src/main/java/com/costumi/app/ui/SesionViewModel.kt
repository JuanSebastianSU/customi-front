package com.costumi.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Cierre de sesion, compartido por las pantallas "home" de cada modo. */
@HiltViewModel
class SesionViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _cerrada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cerrada = _cerrada.asSharedFlow()

    fun cerrarSesion() {
        viewModelScope.launch {
            repo.logout()
            _cerrada.tryEmit(Unit)
        }
    }
}
