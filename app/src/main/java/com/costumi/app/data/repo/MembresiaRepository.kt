package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.apis.MembresiaControllerApi
import com.costumi.apiclient.models.CambiarContextoRequest
import com.costumi.apiclient.models.MembresiaEstadoResponse
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

    // Nota: NO hay "selector multi-tienda" a propósito. El modelo Fase B es exclusivo: una persona trabaja
    // en UNA sola tienda a la vez (regla de seguridad #2; aceptar una 2ª invitación se rechaza en el backend
    // hasta desvincularse). `/auth/me` ya expone la única membresía activa. Por eso `GET /auth/me/membresias`
    // (listar todas) no se usa: no existe un caso de "cambiar de tienda".

    /** El propio empleado se desvincula de su tienda: queda solo-cliente. */
    suspend fun desvincularme(): RespuestaRed<MembresiaEstadoResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.desvincularme() } }
}
