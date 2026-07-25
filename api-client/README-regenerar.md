# Módulo `:api-client` — cliente REST GENERADO (no editar a mano)

Este módulo es el cliente Kotlin/Retrofit generado del contrato OpenAPI del backend Costumi.
**Nunca** se editan sus fuentes a mano: cuando el backend cambia, se regenera y se reemplazan.

## Cómo regenerar

1. Bajar el contrato actualizado a la raíz del proyecto:
   ```powershell
   Invoke-WebRequest -Uri "https://just-upliftment-production-cb1f.up.railway.app/v3/api-docs" -OutFile "openapi.json"
   ```
2. Generar con openapi-generator-cli (7.x, soporta OpenAPI 3.1). Con Java 21:
   ```powershell
   java -jar openapi-generator-cli.jar generate `
     -i openapi.json `
     -g kotlin --library jvm-retrofit2 `
     --additional-properties=serializationLibrary=gson,useCoroutines=true,packageName=com.costumi.apiclient,dateLibrary=java8 `
     -o ./api-client-gen
   ```
3. Reemplazar las fuentes del módulo con las nuevas:
   ```powershell
   Remove-Item -Recurse -Force api-client/src/main/kotlin/*
   Copy-Item -Recurse -Force api-client-gen/src/main/kotlin/* api-client/src/main/kotlin/
   ```
   (Se copia SOLO `src/main/kotlin`; el `build.gradle.kts` del módulo es propio, no el que emite el generador.)

## Notas
- Generador: `kotlin` + librería `jvm-retrofit2`, serialización **gson**, `useCoroutines=true`
  (las APIs son funciones `suspend` que devuelven `retrofit2.Response<T>`), `dateLibrary=java8`
  (usa `java.time` → el `:app` habilita **core library desugaring** por minSdk 24).
- Es un módulo **Kotlin/JVM puro** (plugin `org.jetbrains.kotlin.jvm`); expone
  retrofit/okhttp/gson como `api` para que `:app` construya el cliente con sus interceptores.
- Todas las rutas quedan bajo `api/v1/...`; la URL base (host) la aporta `:app` vía `BuildConfig.BASE_URL`.
- `infrastructure/Serializer.gsonBuilder` ya registra los adapters de `LocalDate`/`OffsetDateTime`:
  reutilizarlo al armar el `GsonConverterFactory` en el `:app`.
