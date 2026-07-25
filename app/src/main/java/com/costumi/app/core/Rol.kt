package com.costumi.app.core

/** Los tres "modos" de la app segun el rol del token (RF-18): la misma app, distinta experiencia. */
enum class ModoApp { CLIENTE, GESTION, SUPERADMIN }

/**
 * Roles del backend. El token trae el `rol`; la app decide la navegacion con [modo].
 * DUENO y los roles de empleado comparten el modo Gestion (sus permisos los acota el backend).
 */
enum class Rol(val modo: ModoApp) {
    CLIENTE(ModoApp.CLIENTE),
    DUENO(ModoApp.GESTION),
    ENCARGADO(ModoApp.GESTION),
    MOSTRADOR(ModoApp.GESTION),
    BODEGA(ModoApp.GESTION),
    ATENCION(ModoApp.GESTION),
    SUPERADMIN(ModoApp.SUPERADMIN);

    companion object {
        fun desde(valor: String?): Rol? =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) }
    }
}
