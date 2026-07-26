package com.costumi.app.di

import com.costumi.app.BuildConfig
import com.costumi.app.data.remote.ComprobantePagoApi
import com.costumi.app.data.remote.ContratoRentaApi
import com.costumi.app.data.remote.FotoPrendaApi
import com.costumi.app.data.remote.ReporteExportApi
import com.costumi.app.data.remote.interceptor.AuthInterceptor
import com.costumi.app.data.remote.interceptor.TokenAuthenticator
import com.costumi.apiclient.apis.AuthControllerApi
import com.costumi.apiclient.apis.ActividadDeEmpleadoControllerApi
import com.costumi.apiclient.apis.AuditoriaControllerApi
import com.costumi.apiclient.apis.CajaControllerApi
import com.costumi.apiclient.apis.CarritoControllerApi
import com.costumi.apiclient.apis.CategoriaControllerApi
import com.costumi.apiclient.apis.ClienteControllerApi
import com.costumi.apiclient.apis.ConfiguracionControllerApi
import com.costumi.apiclient.apis.ConteoDeDependenciasControllerApi
import com.costumi.apiclient.apis.DevolucionControllerApi
import com.costumi.apiclient.apis.GrupoDeStockControllerApi
import com.costumi.apiclient.apis.DisfrazControllerApi
import com.costumi.apiclient.apis.DisfrazMarketplaceControllerApi
import com.costumi.apiclient.apis.EmpleadoControllerApi
import com.costumi.apiclient.apis.EmpresaControllerApi
import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.apis.NotificacionControllerApi
import com.costumi.apiclient.apis.PlantillaNotificacionControllerApi
import com.costumi.apiclient.apis.PagoControllerApi
import com.costumi.apiclient.apis.PermisosEmpleadoControllerApi
import com.costumi.apiclient.apis.PrendaControllerApi
import com.costumi.apiclient.apis.ReembolsoControllerApi
import com.costumi.apiclient.apis.RentaControllerApi
import com.costumi.apiclient.apis.ReporteControllerApi
import com.costumi.apiclient.apis.SucursalControllerApi
import com.costumi.apiclient.apis.TipoEtiquetaControllerApi
import com.costumi.apiclient.apis.VentaControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Nucleo de red: OkHttp (token + refresh rotativo + timeouts + logging en debug) y Retrofit
 * apuntando al backend en Railway. El Gson reusa los adapters de fecha del cliente generado.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Serializer.gsonBuilder.create()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // --- Servicios del cliente generado (se agregan mas por feature) ---

    @Provides
    @Singleton
    fun providePerfilApi(retrofit: Retrofit): com.costumi.apiclient.apis.PerfilControllerApi =
        retrofit.create(com.costumi.apiclient.apis.PerfilControllerApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthControllerApi =
        retrofit.create(AuthControllerApi::class.java)

    // Fase B: cambio de contexto (Comprando/Trabajando), membresías y desvinculación.
    @Provides
    @Singleton
    fun provideMembresiaApi(retrofit: Retrofit): com.costumi.apiclient.apis.MembresiaControllerApi =
        retrofit.create(com.costumi.apiclient.apis.MembresiaControllerApi::class.java)

    // Fase B (paso 5): las capacidades del propio usuario, para armar la navegación por permisos.
    @Provides
    @Singleton
    fun provideMisPermisosApi(retrofit: Retrofit): com.costumi.apiclient.apis.MisPermisosControllerApi =
        retrofit.create(com.costumi.apiclient.apis.MisPermisosControllerApi::class.java)

    @Provides
    @Singleton
    fun provideMarketplaceApi(retrofit: Retrofit): MarketplaceControllerApi =
        retrofit.create(MarketplaceControllerApi::class.java)

    @Provides
    @Singleton
    fun provideDisfrazMarketplaceApi(retrofit: Retrofit): DisfrazMarketplaceControllerApi =
        retrofit.create(DisfrazMarketplaceControllerApi::class.java)

    @Provides
    @Singleton
    fun provideCarritoApi(retrofit: Retrofit): CarritoControllerApi =
        retrofit.create(CarritoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideSucursalApi(retrofit: Retrofit): SucursalControllerApi =
        retrofit.create(SucursalControllerApi::class.java)

    @Provides
    @Singleton
    fun provideClienteApi(retrofit: Retrofit): ClienteControllerApi =
        retrofit.create(ClienteControllerApi::class.java)

    @Provides
    @Singleton
    fun provideReembolsoApi(retrofit: Retrofit): ReembolsoControllerApi =
        retrofit.create(ReembolsoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideEmpresaApi(retrofit: Retrofit): EmpresaControllerApi =
        retrofit.create(EmpresaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideReporteApi(retrofit: Retrofit): ReporteControllerApi =
        retrofit.create(ReporteControllerApi::class.java)

    @Provides
    @Singleton
    fun providePrendaApi(retrofit: Retrofit): PrendaControllerApi =
        retrofit.create(PrendaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideCategoriaApi(retrofit: Retrofit): CategoriaControllerApi =
        retrofit.create(CategoriaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideCategoriaDisfrazApi(retrofit: Retrofit): com.costumi.apiclient.apis.CategoriaDeDisfrazControllerApi =
        retrofit.create(com.costumi.apiclient.apis.CategoriaDeDisfrazControllerApi::class.java)

    @Provides
    @Singleton
    fun provideGrupoStockApi(retrofit: Retrofit): GrupoDeStockControllerApi =
        retrofit.create(GrupoDeStockControllerApi::class.java)

    @Provides
    @Singleton
    fun provideConteoApi(retrofit: Retrofit): ConteoDeDependenciasControllerApi =
        retrofit.create(ConteoDeDependenciasControllerApi::class.java)

    @Provides
    @Singleton
    fun provideTipoEtiquetaApi(retrofit: Retrofit): TipoEtiquetaControllerApi =
        retrofit.create(TipoEtiquetaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideDisfrazApi(retrofit: Retrofit): DisfrazControllerApi =
        retrofit.create(DisfrazControllerApi::class.java)

    @Provides
    @Singleton
    fun provideFotoPrendaApi(retrofit: Retrofit): FotoPrendaApi =
        retrofit.create(FotoPrendaApi::class.java)

    @Provides
    @Singleton
    fun provideFotoDisfrazApi(retrofit: Retrofit): com.costumi.app.data.remote.FotoDisfrazApi =
        retrofit.create(com.costumi.app.data.remote.FotoDisfrazApi::class.java)

    @Provides
    @Singleton
    fun provideFotoPerfilApi(retrofit: Retrofit): com.costumi.app.data.remote.FotoPerfilApi =
        retrofit.create(com.costumi.app.data.remote.FotoPerfilApi::class.java)

    @Provides
    @Singleton
    fun provideFotoSucursalApi(retrofit: Retrofit): com.costumi.app.data.remote.FotoSucursalApi =
        retrofit.create(com.costumi.app.data.remote.FotoSucursalApi::class.java)

    @Provides
    @Singleton
    fun provideFotoEmpresaApi(retrofit: Retrofit): com.costumi.app.data.remote.FotoEmpresaApi =
        retrofit.create(com.costumi.app.data.remote.FotoEmpresaApi::class.java)

    @Provides
    @Singleton
    fun provideMiEmpresaApi(retrofit: Retrofit): com.costumi.app.data.remote.MiEmpresaApi =
        retrofit.create(com.costumi.app.data.remote.MiEmpresaApi::class.java)

    @Provides
    @Singleton
    fun provideMisCarritosApi(retrofit: Retrofit): com.costumi.app.data.remote.MisCarritosApi =
        retrofit.create(com.costumi.app.data.remote.MisCarritosApi::class.java)

    @Provides
    @Singleton
    fun provideMisDeudasApi(retrofit: Retrofit): com.costumi.app.data.remote.MisDeudasApi =
        retrofit.create(com.costumi.app.data.remote.MisDeudasApi::class.java)

    @Provides
    @Singleton
    fun provideVentaApi(retrofit: Retrofit): VentaControllerApi =
        retrofit.create(VentaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideRentaApi(retrofit: Retrofit): RentaControllerApi =
        retrofit.create(RentaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideContratoRentaApi(retrofit: Retrofit): ContratoRentaApi =
        retrofit.create(ContratoRentaApi::class.java)

    @Provides
    @Singleton
    fun provideDevolucionApi(retrofit: Retrofit): DevolucionControllerApi =
        retrofit.create(DevolucionControllerApi::class.java)

    @Provides
    @Singleton
    fun providePagoApi(retrofit: Retrofit): PagoControllerApi =
        retrofit.create(PagoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideComprobantePagoApi(retrofit: Retrofit): ComprobantePagoApi =
        retrofit.create(ComprobantePagoApi::class.java)

    @Provides
    @Singleton
    fun provideCajaApi(retrofit: Retrofit): CajaControllerApi =
        retrofit.create(CajaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideReporteExportApi(retrofit: Retrofit): ReporteExportApi =
        retrofit.create(ReporteExportApi::class.java)

    @Provides
    @Singleton
    fun provideEmpleadoApi(retrofit: Retrofit): EmpleadoControllerApi =
        retrofit.create(EmpleadoControllerApi::class.java)

    // Aceptar invitación de trabajo (pública): preview por token + aceptar con T&C.
    @Provides
    @Singleton
    fun provideInvitacionApi(retrofit: Retrofit): com.costumi.apiclient.apis.InvitacionControllerApi =
        retrofit.create(com.costumi.apiclient.apis.InvitacionControllerApi::class.java)

    @Provides
    @Singleton
    fun providePermisosEmpleadoApi(retrofit: Retrofit): PermisosEmpleadoControllerApi =
        retrofit.create(PermisosEmpleadoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideActividadEmpleadoApi(retrofit: Retrofit): ActividadDeEmpleadoControllerApi =
        retrofit.create(ActividadDeEmpleadoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideConfiguracionApi(retrofit: Retrofit): ConfiguracionControllerApi =
        retrofit.create(ConfiguracionControllerApi::class.java)

    @Provides
    @Singleton
    fun provideNotificacionApi(retrofit: Retrofit): NotificacionControllerApi =
        retrofit.create(NotificacionControllerApi::class.java)

    @Provides
    @Singleton
    fun provideAuditoriaApi(retrofit: Retrofit): AuditoriaControllerApi =
        retrofit.create(AuditoriaControllerApi::class.java)

    @Provides
    @Singleton
    fun providePlantillaApi(retrofit: Retrofit): PlantillaNotificacionControllerApi =
        retrofit.create(PlantillaNotificacionControllerApi::class.java)
}
