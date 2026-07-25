package com.costumi.app.data.remote.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canal para avisar a la UI que la sesion caduco (refresh reusado/revocado): el router (Fase 2)
 * observa [sesionExpirada] y navega a Login. Se emite desde el Authenticator, fuera del ciclo de vida.
 */
@Singleton
class EventosDeSesion @Inject constructor() {

    private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

    fun notificarSesionExpirada() {
        _sesionExpirada.tryEmit(Unit)
    }
}
