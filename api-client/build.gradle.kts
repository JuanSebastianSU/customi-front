import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Modulo :api-client — cliente Retrofit/Kotlin GENERADO del contrato OpenAPI del backend
// (openapi-generator, generador `kotlin`, libreria jvm-retrofit2). NUNCA se edita a mano:
// para regenerar, ver README-regenerar.md en la raiz del modulo.
// Es JVM puro (no Android): expone retrofit/gson/okhttp como `api` para que :app arme el cliente.

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.retrofit.scalars)
    api(libs.gson)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.coroutines.core)
}
