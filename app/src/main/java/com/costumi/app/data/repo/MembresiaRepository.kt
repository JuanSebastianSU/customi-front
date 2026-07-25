package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.MembresiaControllerApi
import com.costumi.apiclient.models.CambiarContextoRequest
import com.costumi.apiclient.models.MembresiaEstadoResponse
import com.costumi.apiclient.models.MembresiaResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contexto de sesión (Fase B, H1): alterna entre «Comprando» y «Trabajando» y desvinculación del propio
 * usuario. El cambio de contexto emite un token nuevo (con la empresa+rol del modo elegido) que se persiste
 * en [SesionLocal]; el llamador re-resuelve el rol y re-navega al shell correspondiente.
 */
@Singleton
class MembresiaRepository @Inject constructor(
    private val api: MembresiaControllerApi,
    private val sesion: SesionLocal,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    /** Cambia el contexto (COMPRA/TRABAJO) y guarda el token nuevo. */
    suspend fun cambiarContexto(modo: CambiarContextoRequest.Modo): RespuestaRed<Unit> =
        withContext(dispatchers.io) {
            when (val r = ejecutarLlamada(gson) { api.cambiarContexto(CambiarContextoRequest(modo)) }) {
                is RespuestaRed.Fallo -> r
                is RespuestaRed.Exito -> {
                    sesion.guardar(r.data)
                    RespuestaRed.Exito(Unit)
                }
            }
        }

    /** Las tiendas del usuario con su rol y estado (a lo sumo una ACTIVA). */
    suspend fun misMembresias(): RespuestaRed<List<MembresiaResponse>> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.mias1() } }

    /** El propio empleado se desvincula de su tienda: queda solo-cliente. */
    suspend fun desvincularme(): RespuestaRed<MembresiaEstadoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.desvincularme() } }
}
