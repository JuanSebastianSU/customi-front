package com.costumi.app.ui.gestion.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costumi.app.R
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.UiState
import com.costumi.app.data.repo.InventarioRepository
import com.costumi.app.data.repo.MiEmpresaRepository
import com.costumi.app.data.repo.ReembolsoRepository
import com.costumi.app.data.repo.ResumenDashboard
import com.costumi.app.data.repo.ReporteRepository
import com.costumi.app.ui.common.Tono
import com.costumi.apiclient.models.SolicitudDeReembolsoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Una cosa que requiere atencion hoy, con a donde ir a resolverla. El panel solo muestra las que
 * existen: si no hay ninguna, dice que esta todo al dia (y eso tambien es informacion).
 */
data class AlertaPanel(
    val titulo: String,
    val detalle: String,
    val tono: Tono,
    /** Destino de navegacion (id del nav_gestion) al que lleva tocarla. */
    val destino: Int,
)

/** Dashboard de Gestion: lo urgente primero, luego ingresos e inventario. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: ReporteRepository,
    private val inventario: InventarioRepository,
    private val miEmpresa: MiEmpresaRepository,
    private val reembolsos: ReembolsoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<UiState<ResumenDashboard>>(UiState.Loading)
    val estado = _estado.asStateFlow()

    /** Nombre de la tienda: encabeza el panel para saber en que empresa se esta operando (RF-15.1). */
    private val _tienda = MutableStateFlow<String?>(null)
    val tienda = _tienda.asStateFlow()

    /** Lo que requiere atencion hoy, ya ordenado por gravedad. */
    private val _alertas = MutableStateFlow<List<AlertaPanel>>(emptyList())
    val alertas = _alertas.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            (miEmpresa.mia() as? RespuestaRed.Exito)?.let { _tienda.value = it.data.nombre }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Loading
            _estado.value = when (val r = repo.dashboard()) {
                is RespuestaRed.Exito -> UiState.Success(r.data)
                is RespuestaRed.Fallo -> UiState.Error(r.error.mensaje) { cargar() }
            }
        }
        cargarAlertas()
    }

    /**
     * Las tres alertas se piden en paralelo y son best-effort: si una falla, el panel muestra las
     * otras en vez de quedarse en blanco. Se recalcula al volver al panel, porque se pudo reponer
     * stock o cerrar una renta en otra pantalla.
     */
    fun cargarAlertas() {
        viewModelScope.launch {
            val vencidas = async { (repo.rentasVencidas() as? RespuestaRed.Exito)?.data.orEmpty() }
            val stockBajo = async { (inventario.stockBajo() as? RespuestaRed.Exito)?.data.orEmpty() }
            val pendientes = async {
                (reembolsos.bandeja() as? RespuestaRed.Exito)?.data.orEmpty()
                    .filter { it.estado == SolicitudDeReembolsoResponse.Estado.PENDIENTE }
            }

            val lista = mutableListOf<AlertaPanel>()
            val rentas = vencidas.await()
            if (rentas.isNotEmpty()) {
                val peor = rentas.maxOfOrNull { it.diasVencida ?: 0L } ?: 0L
                lista.add(
                    AlertaPanel(
                        titulo = if (rentas.size == 1) "1 renta vencida" else "${rentas.size} rentas vencidas",
                        detalle = if (peor <= 0) "Vencieron hoy" else "La mas atrasada, hace $peor dias",
                        tono = Tono.ERROR,
                        destino = R.id.rentasFragment,
                    ),
                )
            }
            val grupos = stockBajo.await()
            if (grupos.isNotEmpty()) {
                lista.add(
                    AlertaPanel(
                        titulo = if (grupos.size == 1) "1 variante con stock bajo"
                        else "${grupos.size} variantes con stock bajo",
                        detalle = "Reponer antes de que se agoten",
                        tono = Tono.ALERTA,
                        destino = R.id.inventarioFragment,
                    ),
                )
            }
            val reembolsosPendientes = pendientes.await()
            if (reembolsosPendientes.isNotEmpty()) {
                lista.add(
                    AlertaPanel(
                        titulo = if (reembolsosPendientes.size == 1) "1 reembolso por responder"
                        else "${reembolsosPendientes.size} reembolsos por responder",
                        detalle = "El cliente esta esperando la decision",
                        tono = Tono.ALERTA,
                        destino = R.id.reembolsosFragment,
                    ),
                )
            }
            _alertas.value = lista
        }
    }
}
