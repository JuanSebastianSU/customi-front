package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.ErrorApi
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.core.TipoError
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.ActividadDeEmpleadoControllerApi
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.EmpleadoControllerApi
import com.costumi.apiclient.apis.PermisosEmpleadoControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.ActividadResponse
import com.costumi.apiclient.models.AsignarSucursalesRequest
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.CapacidadDto
import com.costumi.apiclient.models.EmpleadoDetalleResponse
import com.costumi.apiclient.models.EmpleadoResponse
import com.costumi.apiclient.models.EstablecerPermisoRequest
import com.costumi.apiclient.models.InvitacionResponse
import com.costumi.apiclient.models.InvitarEmpleadoRequest
import com.costumi.apiclient.models.SucursalResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Gestión de personal (RF-8): listar, alta, cambiar rol, baja/reactivación y asignar sucursales. */
@Singleton
class EmpleadoRepository @Inject constructor(
    private val empleadoApi: EmpleadoControllerApi,
    private val permisosApi: PermisosEmpleadoControllerApi,
    private val actividadApi: ActividadDeEmpleadoControllerApi,
    private val authApi: AuthControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun empleados(buscar: String? = null): RespuestaRed<List<EmpleadoDetalleResponse>> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) { empleadoApi.listar10(buscar = buscar?.ifBlank { null }, tamano = TAMANO) }
                .mapear { it.contenido.orEmpty() }
        }

    /** Alta = invitación (Fase B): invita por email; el enlace de aceptación viene en la respuesta. */
    suspend fun invitar(
        email: String,
        rol: InvitarEmpleadoRequest.Rol,
        sucursalIds: List<UUID> = emptyList(),
    ): RespuestaRed<InvitacionResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                empleadoApi.invitar(InvitarEmpleadoRequest(email = email, rol = rol, sucursalIds = sucursalIds))
            }
        }

    suspend fun cambiarRol(id: UUID, rol: CambiarRolRequest.Rol): RespuestaRed<EmpleadoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empleadoApi.cambiarRol(id, CambiarRolRequest(rol)) } }

    suspend fun activar(id: UUID): RespuestaRed<EmpleadoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empleadoApi.activar2(id) } }

    suspend fun desactivar(id: UUID): RespuestaRed<EmpleadoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empleadoApi.desactivar(id) } }

    suspend fun asignarSucursales(id: UUID, sucursalIds: List<UUID>): RespuestaRed<List<UUID>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { empleadoApi.asignarSucursales(id, AsignarSucursalesRequest(sucursalIds)) } }

    /** Matriz de capacidades del empleado (catálogo completo con `concedido`), Fase B paso 5. */
    suspend fun permisos(id: UUID): RespuestaRed<List<CapacidadDto>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { permisosApi.matriz(id) } }

    suspend fun establecerPermiso(
        id: UUID,
        capacidad: EstablecerPermisoRequest.Capacidad,
        concedido: Boolean,
    ): RespuestaRed<Unit> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { permisosApi.establecer(id, EstablecerPermisoRequest(capacidad, concedido)) }
    }

    /** Actividad del empleado (ventas y total vendido). */
    suspend fun actividad(id: UUID): RespuestaRed<ActividadResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { actividadApi.actividad(id) } }

    /** Sucursales activas de la empresa, para resolver nombres y asignar. */
    suspend fun sucursales(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> {
                val empresaId = me.data.empresaId
                if (empresaId.isNullOrBlank()) {
                    RespuestaRed.Fallo(ErrorApi(TipoError.DESCONOCIDO, "Tu usuario no tiene empresa asignada."))
                } else {
                    when (val r = ejecutarLlamada(gson) { sucursalApi.listar9(UUID.fromString(empresaId)) }) {
                        is RespuestaRed.Fallo -> r
                        is RespuestaRed.Exito -> RespuestaRed.Exito(r.data.filter { it.archivada != true })
                    }
                }
            }
        }
    }

    private companion object {
        /** Estas listas se muestran completas en pantalla; se pide una pagina grande y se busca en el servidor. */
        const val TAMANO = 100
    }
}
