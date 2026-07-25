package com.costumi.app.di

import com.costumi.app.core.DefaultDispatcherProvider
import com.costumi.app.core.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Wiring transversal (dispatchers de corrutinas y otras utilidades sin estado). */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
