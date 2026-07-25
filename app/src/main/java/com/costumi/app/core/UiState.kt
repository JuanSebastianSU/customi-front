package com.costumi.app.core

/**
 * Estado de UI unico que toda pantalla renderiza (ver ORDEN_CONSTRUCCION §B). El ViewModel expone
 * un StateFlow<UiState<...>> y la vista dibuja segun el estado: cargando, con datos, vacio o error.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val mensaje: String, val reintentar: (() -> Unit)? = null) : UiState<Nothing>
}
