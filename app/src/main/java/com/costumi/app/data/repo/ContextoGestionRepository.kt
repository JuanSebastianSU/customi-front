package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.core.mapear
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.MisPermisosControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.models.SucursalResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contexto de la sesión de gestión (Fase B): las capacidades propias (para la navegación por permisos, A7) y
 * la sucursal activa multi-sucursal (A3). La sucursal activa se guarda en {@link SesionLocal} y el interceptor
 * la manda como {@code X-Sucursal-Id}.
 */
@Singleton
class ContextoGestionRepository @Inject constructor(
    private val misPermisosApi: MisPermisosControllerApi,
    private val sucursalApi: SucursalControllerApi,
    private val authApi: AuthControllerApi,
    private val sesion: SesionLocal,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Las secciones a las que el usuario tiene acceso, según sus capacidades concedidas (paso 5). */
    suspend fun misSecciones(): RespuestaRed<Set<String>> = withContext(dispatchers.io) {
        ejecutarLlamada(gson) { misPermisosApi.mias() }
            .mapear { caps -> caps.mapNotNull { it.seccion }.toSet() }
    }

    /** Sucursales (no archivadas) de mi empresa, para el selector de sucursal activa (DUEÑO/ENCARGADO). */
    suspend fun sucursales(): RespuestaRed<List<SucursalResponse>> = withContext(dispatchers.io) {
        when (val me = ejecutarLlamada(gson) { authApi.me() }) {
            is RespuestaRed.Fallo -> me
            is RespuestaRed.Exito -> {
                val empresaId = me.data.empresaId
                if (empresaId.isNullOrBlank()) {
                    RespuestaRed.Exito(emptyList())
                } else {
                    ejecutarLlamada(gson) { sucursalApi.listar9(UUID.fromString(empresaId)) }
                        .mapear { lista -> lista.filter { it.archivada != true } }
                }
            }
        }
    }

    var sucursalActivaId: String?
        get() = sesion.sucursalActivaId
        set(value) {
            sesion.sucursalActivaId = value
        }
}
