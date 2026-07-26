package com.costumi.app.ui.auth

import com.costumi.app.core.ModoApp

/** Eventos de una sola vez que emiten los ViewModels de auth (navegar, error, info). */
sealed interface EventoAuth {
    data class Navegar(val modo: ModoApp) : EventoAuth
    data class Error(val mensaje: String) : EventoAuth
    data class Info(val mensaje: String) : EventoAuth

    /** Preview de una invitación de trabajo lista para aceptar (empresa/rol/email). */
    data class InvitacionLista(
        val vista: com.costumi.apiclient.models.InvitacionVistaResponse,
        val token: String,
    ) : EventoAuth
}
