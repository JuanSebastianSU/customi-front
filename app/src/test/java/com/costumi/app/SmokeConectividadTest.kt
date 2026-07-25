package com.costumi.app

import com.costumi.apiclient.apis.MarketplaceControllerApi
import com.costumi.apiclient.infrastructure.Serializer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Smoke test de conectividad REAL contra el backend en produccion (Railway), endpoint publico
 * del marketplace. Prueba de punta a punta que el cliente generado + Gson deserializan una
 * respuesta viva. Requiere internet; no forma parte del build normal (assembleDebug no corre tests).
 */
class SmokeConectividadTest {

    @Test
    fun marketplace_empresas_responde_y_deserializa() = runBlocking {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(Serializer.gsonBuilder.create()))
            .build()
        val api = retrofit.create(MarketplaceControllerApi::class.java)

        val respuesta = api.empresas(buscar = null)
        println("Marketplace HTTP ${respuesta.code()} — empresas=${respuesta.body()?.size}")

        assertTrue("Esperaba 2xx del marketplace, fue ${respuesta.code()}", respuesta.isSuccessful)
    }
}
