package com.costumi.app.data.remote.interceptor

import com.costumi.app.data.remote.session.SesionLocal
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adjunta `Authorization: Bearer <accessToken>` a cada request protegida. No lo agrega a los
 * endpoints publicos de auth (login/registro/olvide/restablecer/refresh), que no requieren token.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sesion: SesionLocal,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (esPublico(original.url.encodedPath)) {
            return chain.proceed(original)
        }
        val token = sesion.accessToken
        val builder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        // Multi-sucursal (Fase B): acota ventas/rentas/caja a la sucursal activa; sin ella = todas.
        sesion.sucursalActivaId?.takeIf { it.isNotBlank() }?.let { builder.header("X-Sucursal-Id", it) }
        return chain.proceed(builder.build())
    }

    companion object {
        private val PUBLICOS = listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/registro",
            "/api/v1/auth/olvide",
            "/api/v1/auth/restablecer",
            "/api/v1/auth/refresh",
        )

        /** Endpoints que no llevan token ni deben disparar el refresh del Authenticator. */
        fun esPublico(encodedPath: String): Boolean = PUBLICOS.any { encodedPath.endsWith(it) }
    }
}
