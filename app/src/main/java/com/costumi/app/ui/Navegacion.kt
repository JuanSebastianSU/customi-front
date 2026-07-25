package com.costumi.app.ui

import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.costumi.app.R
import com.costumi.app.core.ModoApp

/** Router por rol: entra al "home" del modo, limpiando la pila (no se vuelve a auth con Atras). */
fun NavController.irAHome(modo: ModoApp) {
    val destino = when (modo) {
        ModoApp.CLIENTE -> R.id.clienteShellFragment
        ModoApp.GESTION -> R.id.gestionShellFragment
        ModoApp.SUPERADMIN -> R.id.superadminHomeFragment
    }
    navigate(destino, null, limpiarStack())
}

/** Vuelve a Login limpiando la pila (logout o sesion expirada). */
fun NavController.irALogin() {
    if (currentDestination?.id == R.id.loginFragment) return
    navigate(R.id.loginFragment, null, limpiarStack())
}

private fun limpiarStack() = navOptions {
    popUpTo(R.id.nav_principal) { inclusive = true }
    launchSingleTop = true
}
