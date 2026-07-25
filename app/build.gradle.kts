import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.costumi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.costumi.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base del backend en Railway (contrato en /v3/api-docs). El path relativo de cada
        // endpoint lo aporta el cliente generado; Retrofit concatena sobre esta base.
        buildConfigField("String", "BASE_URL", "\"https://just-upliftment-production-cb1f.up.railway.app/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // El cliente generado usa java.time (dateLibrary=java8) y minSdk 24 < 26 → desugaring.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// El compilador de Kotlin lo aporta AGP 9.1.1 (Kotlin 2.2.21). Forzamos que kotlin-stdlib
// coincida con esa version: alguna dependencia muy nueva podria traer una stdlib compilada
// con Kotlin 2.4, cuya metadata este compilador no puede leer.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.21")
    }
}

dependencies {
    // Push (FCM). El BOM alinea versiones; solo se usa messaging.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.coroutines.play.services)

    // Cliente REST generado del contrato OpenAPI (expone retrofit/okhttp/gson transitivamente)
    implementation(project(":api-client"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX base / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.swiperefresh)

    // Ciclo de vida + corrutinas
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.fragment.ktx)
    implementation(libs.coroutines.android)

    // Navegación
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Caché local (Room, procesador vía KSP)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Inyección de dependencias (Hilt, procesador vía KSP). hilt-navigation-fragment se omite
    // adrede: arrastra hilt-android 2.59 (requiere AGP 9) y no lo necesitamos con `by viewModels()`.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Token seguro
    implementation(libs.security.crypto)

    // Imágenes
    implementation(libs.coil)
    implementation(libs.coil.network)

    // Listas grandes (paginación)
    implementation(libs.paging.runtime)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
