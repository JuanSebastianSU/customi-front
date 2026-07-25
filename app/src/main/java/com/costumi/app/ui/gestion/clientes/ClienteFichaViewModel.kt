package com.costumi.app.ui.gestion.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.ClientesRepository
import com.costumi.apiclient.models.CrearClienteRequest
import com.costumi.apiclient.models.EditarClienteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface EventoFicha {
    data object Guardado : EventoFicha
    data class Error(val mensaje: String) : EventoFicha
    /** Desglose de la deuda del cliente, para mostrarlo sin salir de su ficha. */
    data class EstadoCuenta(val estado: com.costumi.apiclient.models.EstadoDeCuentaResponse) : EventoFicha
}

@HiltViewModel
class ClienteFichaViewModel @Inject constructor(
    private val repo: ClientesRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    val clienteId: String? = estado[ClienteFichaFragment.ARG_ID]
    val esEdicion: Boolean = clienteId != null

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoFicha>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    /** Estado de cuenta del cliente que se está viendo (solo en edición). */
    fun verEstadoCuenta(id: UUID) {
        viewModelScope.launch {
            _eventos.tryEmit(
                when (val r = repo.estadoCuenta(id)) {
                    is RespuestaRed.Exito -> EventoFicha.EstadoCuenta(r.data)
                    is RespuestaRed.Fallo -> EventoFicha.Error(r.error.mensaje)
                },
            )
        }
    }

    fun guardar(
        nombre: String,
        telefono: String?,
        email: String?,
        documento: String?,
        direccion: String?,
    ) {
        if (_guardando.value) return
        viewModelScope.launch {
            _guardando.value = true
            val r = if (esEdicion) {
                // El correo es inmutable tras crear: EditarClienteRequest ya no lo incluye.
                repo.editar(
                    UUID.fromString(clienteId),
                    EditarClienteRequest(nombre, telefono, documento, direccion),
                )
            } else {
                repo.crear(CrearClienteRequest(nombre, telefono, email, documento, direccion))
            }
            _guardando.value = false
            _eventos.tryEmit(
                when (r) {
                    is RespuestaRed.Exito -> EventoFicha.Guardado
                    is RespuestaRed.Fallo -> EventoFicha.Error(r.error.mensaje)
                },
            )
        }
    }
}
