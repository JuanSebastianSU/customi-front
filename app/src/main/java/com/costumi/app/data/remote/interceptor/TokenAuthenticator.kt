package com.costumi.app.data.remote.interceptor

import com.costumi.app.BuildConfig
import com.costumi.app.data.remote.session.EventosDeSesion
import com.costumi.app.data.remote.session.SesionLocal
import com.costumi.apiclient.models.RefreshRequest
import com.costumi.apiclient.models.TokenResponse
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maneja el 401 en endpoints protegidos con REFRESH ROTATIVO y deteccion de reuso (regla C2):
 *  - Refresca UNA sola vez por cadena (corta si ya se reintento).
 *  - El refresh rota: se guarda el par NUEVO y se reintenta la request original con el access nuevo.
 *  - Si el propio /auth/refresh falla (reuso/revocado → el backend revoco la familia), NO reintenta:
 *    borra la sesion local y avisa para mandar a Login.
 *  - Sincroniza: si otro hilo ya refresco mientras esperabamos, se reintenta con el token nuevo sin
 *    volver a refrescar.
 *
 * El refresh se hace con un OkHttpClient propio y "desnudo" (sin este Authenticator ni el
 * AuthInterceptor) para evitar recursion.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sesion: SesionLocal,
    private val eventos: EventosDeSesion,
    private val gson: Gson,
) : Authenticator {

    private val clienteRefresh by lazy { OkHttpClient.Builder().build() }
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Nunca refrescar en endpoints publicos (p. ej. un 401 de login por credenciales malas).
        if (AuthInterceptor.esPublico(response.request.url.encodedPath)) return null
        // Si ya reintentamos, cortar para no entrar en loop.
        if (contarRespuestas(response) >= 2) return null

        val accessQueFallo = response.request.header("Authorization")
            ?.removePrefix("Bearer ")?.trim()

        synchronized(lock) {
            val accessActual = sesion.accessToken
            // Otro hilo ya refresco mientras esperabamos el lock → reintentar con el nuevo.
            if (!accessActual.isNullOrBlank() && accessActual != accessQueFallo) {
                return reintentarCon(response, accessActual)
            }

            val refresh = sesion.refreshToken
            if (refresh.isNullOrBlank()) {
                cerrarSesion()
                return null
            }

            val nuevos = intentarRefrescar(refresh)
            if (nuevos == null || nuevos.accessToken.isNullOrBlank()) {
                // Refresh rechazado o error → sesion invalida.
                cerrarSesion()
                return null
            }

            sesion.guardar(nuevos)
            return reintentarCon(response, nuevos.accessToken!!)
        }
    }

    private fun reintentarCon(response: Response, accessToken: String): Request =
        response.request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun intentarRefrescar(refreshToken: String): TokenResponse? = try {
        val cuerpo = gson.toJson(RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url(BuildConfig.BASE_URL + "api/v1/auth/refresh")
            .post(cuerpo)
            .build()
        clienteRefresh.newCall(peticion).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            gson.fromJson(json, TokenResponse::class.java)
        }
    } catch (_: Exception) {
        null
    }

    private fun cerrarSesion() {
        sesion.limpiar()
        eventos.notificarSesionExpirada()
    }

    /** Cuenta cuantas respuestas hay en la cadena (para no reintentar mas de una vez). */
    private fun contarRespuestas(response: Response): Int {
        var actual: Response? = response
        var cuenta = 1
        while (actual?.priorResponse != null) {
            cuenta++
            actual = actual.priorResponse
        }
        return cuenta
    }
}
