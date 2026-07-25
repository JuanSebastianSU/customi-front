package com.costumi.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: el contenedor de dependencias vive atado al ciclo de la aplicacion. */
@HiltAndroidApp
class CostumiApp : Application()
