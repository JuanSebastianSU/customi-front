package com.costumi.app.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstrae los dispatchers de corrutinas para poder inyectarlos (y sustituirlos por unos de prueba
 * en tests). Regla del proyecto: TODO I/O corre fuera del hilo principal (nunca runBlocking en main).
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/** Implementacion por defecto sobre los dispatchers reales. */
class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}
