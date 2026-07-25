package com.costumi.app.ui.gestion.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.repo.ClientesRepository
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.EstadoDeCuentaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FiltrosClientes(
    val buscar: String = "",
    val incluirArchivados: Boolean = false,
    val filtro: String? = null,
)

sealed interface EventoCliente {
    data class Info(val mensaje: String) : EventoCliente
    data class Error(val mensaje: String) : EventoCliente
    data class EstadoCuenta(val cliente: ClienteResponse, val estado: EstadoDeCuentaResponse) : EventoCliente
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val repo: ClientesRepository,
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosClientes())

    val clientes = _filtros
        .flatMapLatest { repo.clientes(it.buscar, it.incluirArchivados, it.filtro) }
        .cachedIn(viewModelScope)

    private val _eventos = MutableSharedFlow<EventoCliente>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    fun buscar(texto: String) {
        _filtros.value = _filtros.value.copy(buscar = texto)
    }

    fun verArchivados(incluir: Boolean) {
        _filtros.value = _filtros.value.copy(incluirArchivados = incluir)
    }

    /** Filtra la cartera: PENDIENTES / VENCIDAS / MULTAS / SALDOS (o null = todos). */
    fun filtrar(filtro: String?) {
        _filtros.value = _filtros.value.copy(filtro = filtro)
    }

    /** Carga el estado de cuenta del cliente (desglose de deuda) y lo emite para mostrarlo. */
    fun verEstadoCuenta(cliente: ClienteResponse) {
        val id = cliente.id ?: return
        viewModelScope.launch {
            when (val r = repo.estadoCuenta(id)) {
                is RespuestaRed.Exito -> _eventos.tryEmit(EventoCliente.EstadoCuenta(cliente, r.data))
                is RespuestaRed.Fallo -> _eventos.tryEmit(EventoCliente.Error(r.error.mensaje))
            }
        }
    }

    fun alternarArchivado(cliente: ClienteResponse) {
        val id = cliente.id ?: return
        val archivado = cliente.archivada == true
        viewModelScope.launch {
            val r = if (archivado) repo.activar(id) else repo.archivar(id)
            emitir(r, if (archivado) "Cliente activado." else "Cliente archivado.")
        }
    }

    fun alternarListaNegra(cliente: ClienteResponse) {
        val id = cliente.id ?: return
        val enNegra = cliente.enListaNegra == true
        viewModelScope.launch {
            val r = repo.cambiarListaNegra(id, !enNegra)
            emitir(r, if (enNegra) "Cliente quitado de lista negra." else "Cliente puesto en lista negra.")
        }
    }

    private fun emitir(r: RespuestaRed<ClienteResponse>, exito: String) {
        _eventos.tryEmit(
            when (r) {
                is RespuestaRed.Exito -> EventoCliente.Info(exito)
                is RespuestaRed.Fallo -> EventoCliente.Error(r.error.mensaje)
            },
        )
    }
}
