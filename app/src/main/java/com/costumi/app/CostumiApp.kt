package com.costumi.app

import android.app.Application
import com.costumi.app.push.Notificaciones
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: el contenedor de dependencias vive atado al ciclo de la aplicacion. */
@HiltAndroidApp
class CostumiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Canal de avisos: debe existir antes de que llegue la primera push (Android 8+).
        Notificaciones.crearCanal(this)
    }
}
