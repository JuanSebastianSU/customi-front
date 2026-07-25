package com.costumi.app.data.repo

import com.costumi.app.core.DispatcherProvider
import com.costumi.app.core.RespuestaRed
import com.costumi.app.data.remote.ejecutarLlamada
import com.costumi.apiclient.apis.PerfilControllerApi
import com.costumi.apiclient.models.ActualizarPerfilRequest
import com.costumi.apiclient.models.CambiarContrasenaRequest
import com.costumi.apiclient.models.PerfilResponse
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La propia cuenta (RF-14): ver y editar los datos, y cambiar la contrasena. Siempre opera sobre el
 * usuario del token, asi que no recibe ningun id.
 */
@Singleton
class PerfilRepository @Inject constructor(
    private val api: PerfilControllerApi,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun perfil(): RespuestaRed<PerfilResponse> =
        withContext(dispatchers.io) { ejecutarLlamada(gson) { api.ver() } }

    /** Vacio borra el dato: nombre y telefono son opcionales. */
    suspend fun actualizar(nombre: String?, telefono: String?): RespuestaRed<PerfilResponse> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                api.actualizar(
                    ActualizarPerfilRequest(
                        nombre = nombre?.trim().orEmpty(),
                        telefono = telefono?.trim().orEmpty(),
                    ),
                )
            }
        }

    /** El backend exige la contrasena actual aunque la sesion este abierta. */
    suspend fun cambiarContrasena(actual: String, nueva: String): RespuestaRed<Unit> =
        withContext(dispatchers.io) {
            ejecutarLlamada(gson) {
                api.cambiarContrasena(CambiarContrasenaRequest(contrasenaActual = actual, contrasenaNueva = nueva))
            }
        }
}
