package com.costumi.app.data.remote.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.costumi.apiclient.models.TokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacen cifrado de la sesion (access + refresh token) con EncryptedSharedPreferences.
 * Los tokens NUNCA se guardan en texto plano. El refresh rota: al refrescar se sobrescribe el par.
 */
@Singleton
class SesionLocal @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ARCHIVO,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Synchronized
    fun guardar(tokens: TokenResponse) {
        prefs.edit()
            .putString(K_ACCESS, tokens.accessToken)
            .putString(K_REFRESH, tokens.refreshToken)
            .putString(K_TYPE, tokens.tokenType ?: "Bearer")
            .apply()
    }

    val accessToken: String?
        @Synchronized get() = prefs.getString(K_ACCESS, null)

    val refreshToken: String?
        @Synchronized get() = prefs.getString(K_REFRESH, null)

    fun haySesion(): Boolean = !accessToken.isNullOrBlank()

    @Synchronized
    fun limpiar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val ARCHIVO = "costumi_sesion"
        const val K_ACCESS = "access_token"
        const val K_REFRESH = "refresh_token"
        const val K_TYPE = "token_type"
    }
}
