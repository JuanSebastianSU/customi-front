# Costumi — Plan de producto (app Android)

> **Qué es este documento.** El roadmap para llevar la app de "proyecto que funciona" a **producto
> comercializable**. Cada ítem dice qué falta, por qué importa y cuándo se considera terminado.
>
> **Documentos hermanos:**
> - `PLAN_ROOM_OFFLINE.md` — el detalle técnico del ítem 7 (Room/offline).
> - `PROGRESS.md` — el tablero vivo: qué está hecho, qué sigue. **Se actualiza al terminar cada ítem.**
> - `PENDIENTE_FRONTEND.md` — historial de las correcciones funcionales ya cerradas.
>
> **Diagnóstico base (medido sobre el código, 2026-07-22):** la arquitectura está sana —MVVM correcto,
> 55/55 Fragments sin fugas de binding, 53/53 ViewModels con `viewModelScope`, `repeatOnLifecycle`
> centralizado, cero `GlobalScope`, cero `runBlocking`, tokens cifrados, sin tráfico en claro—. **Lo que
> falta no es arreglar lo hecho, es completar lo que un producto necesita alrededor.**

---

## 🔴 Bloque 1 — Bloqueantes para publicar

Sin estos cuatro **no se puede vender**. Son configuración, no arquitectura.

### 1. Firma de release
- **Estado:** no hay `signingConfig` en `app/build.gradle.kts`.
- **Por qué bloquea:** Play Store **rechaza** un APK/AAB sin firmar. No es opcional.
- **Hecho cuando:** existe un keystore de release **fuera del repositorio**, referenciado por variables
  de entorno o `local.properties` (que está en `.gitignore`), y `./gradlew :app:bundleRelease` produce un
  AAB firmado.
- ⚠️ **El keystore nunca se commitea.** Si se pierde, no se puede volver a publicar la misma app: hay que
  crear una ficha nueva en Play Store. Se guarda con copia de seguridad.

### 2. Ofuscación y reducción (R8)
- **Estado:** `isMinifyEnabled = false`.
- **Por qué importa:** el APK va sin comprimir y **con los nombres de clases y métodos legibles**.
  Cualquiera lo descompila y ve la lógica, los endpoints y la estructura del backend.
- **Hecho cuando:** `isMinifyEnabled = true` + `isShrinkResources = true` en release, con reglas de
  ProGuard para lo que usa reflexión: **Gson/DTOs del `:api-client`**, **Room**, **Retrofit**, **Hilt**.
- ⚠️ **Riesgo real:** R8 mal configurado rompe la serialización en release y **no en debug**, así que el
  error aparece recién en producción. **Obligatorio probar el APK de release a mano** antes de publicar.

### 3. Reporte de errores en producción
- **Estado:** cero Crashlytics, cero herramienta de errores.
- **Por qué importa:** si a un cliente le crashea la app, **no te enterás**. Te enterás cuando deja de
  usarla. Hoy volás a ciegas.
- **Hecho cuando:** Crashlytics recibe crashes y errores no fatales, con `userId` **anónimo** (nunca
  email ni teléfono), y se registran los fallos de red relevantes como no-fatales.
- **Nota:** el backend **ya tiene** observabilidad (SigNoz + OpenTelemetry). La app no. Esa es la brecha.

### 4. Estrategia de versionado
- **Estado:** `versionCode = 1`, `versionName = "1.0"` fijos.
- **Por qué bloquea:** Play Store exige que cada subida tenga un `versionCode` **mayor**. Con el valor
  fijo, la segunda publicación se rechaza.
- **Hecho cuando:** el `versionCode` se incrementa de forma automática o hay un procedimiento escrito, y
  el `versionName` sigue un esquema claro (`MAYOR.MENOR.PARCHE`).

---

## 🟠 Bloque 2 — Calidad profesional

Lo que separa un proyecto de un producto que se puede mantener.

### 5. Tests
- **Estado:** **1** test unitario y **0** instrumentados, para 53 ViewModels y 28 repositorios.
- **Por qué es el mayor riesgo del proyecto:** el backend tiene **540 tests**; la app tiene 1. Hoy cada
  cambio se verifica compilando y probando a mano. Con clientes reales, un cambio en el carrito puede
  romper el checkout y te enterás por un reclamo.
- **Enfoque:** **no** cubrir 53 pantallas. Cubrir lo que mueve dinero y lo que rompería el negocio:
  - [ ] Carrito: agregar, quitar, cantidades, fechas de renta.
  - [ ] Checkout de venta y de renta.
  - [ ] Cobros y devoluciones.
  - [ ] Sesión: login, refresh de token, expiración.
  - [ ] Los repositorios con caché (ver `PLAN_ROOM_OFFLINE.md` §7).
- **Hecho cuando:** esos flujos tienen test, y `./gradlew :app:testDebugUnitTest` corre en verde.

### 6. Textos a `strings.xml`
- **Estado:** **199** textos incrustados en layouts + **60** en Kotlin. `strings.xml` tiene **1** entrada.
- **Por qué importa** (no es purismo): sin extraerlos **no se puede traducir**, no se puede corregir un
  texto sin recompilar, y es lo primero que mira quien audita el código.
- **Hecho cuando:** los 259 textos visibles están en `strings.xml`, con nombres consistentes, y los
  plurales usan `plurals`. Trabajo mecánico pero largo; se puede hacer por pantallas.
- **Regla al escribir código nuevo:** desde ahora, **texto visible nuevo va a `strings.xml`**, para no
  agrandar la deuda mientras se paga.

### 7. Room / offline
- Detallado en **`PLAN_ROOM_OFFLINE.md`**. Resumen: hoy hay **una sola tabla** (`empresa`), cuando el
  diseño pedía cache-first en todas las listas (`ORDEN_CONSTRUCCION.md` Fase 8).

---

## 🟡 Bloque 3 — Lo que un producto comercial necesita

### 8. Actualización forzada (kill switch)
- **Por qué:** cuando cambies el backend de forma incompatible, **las apps viejas van a romper**.
  Necesitás poder decir "actualizá para seguir usando".
- **Hecho cuando:** la app consulta la versión mínima soportada al arrancar y, si está por debajo,
  muestra una pantalla bloqueante con el enlace a la tienda.
- **Implica backend:** un endpoint que devuelva la versión mínima.

### 9. Política de privacidad y declaración de datos
- **Por qué:** Play Store **la exige** cuando se recolectan datos personales. Costumi guarda **nombre,
  teléfono e historial de compras**.
- **Hecho cuando:** existe la política publicada en una URL, está enlazada desde la app, y la sección
  *Data safety* de Play Console declara exactamente lo que se recolecta.
- **No es opcional ni es trámite:** una declaración incorrecta es motivo de suspensión.

### 10. Pantalla de sesión expirada
- **Estado:** ya existe `TokenAuthenticator` con refresh automático. Falta el caso **"el refresh también
  falló"**.
- **Por qué:** hoy el usuario se queda viendo errores sin entender por qué.
- **Hecho cuando:** al fallar el refresh, la app limpia la sesión y **la caché** (ver norma N1 del plan de
  Room) y lleva al login con un mensaje claro.

### 11. Reintentos con espera
- **Por qué:** un fallo puntual de red o un servidor lento hoy se traduce en error directo. Un reintento
  con retroceso exponencial resuelve la mayoría sin que el usuario lo note.
- **Hecho cuando:** las peticiones **idempotentes** (GET) reintentan con espera creciente.
- ⚠️ **Nunca reintentar automáticamente lo que cobra o crea**: el backend tiene clave de idempotencia,
  pero un reintento ciego sobre un checkout es la forma clásica de duplicar una venta.

### 12. Biometría para entrar
- **Por qué:** en un mostrador el empleado abre la app decenas de veces al día. Huella en vez de contraseña.
- **Hecho cuando:** con sesión válida guardada, se puede desbloquear con biometría; sigue existiendo la
  vía de contraseña como respaldo.

---

## 🟢 Bloque 4 — Mejoras de alto impacto (opcionales)

### 13. Onboarding la primera vez
Un dueño que instala y no entiende qué hacer, desinstala. Tres pantallas explicando el flujo.

### 14. Layouts para tablet
En un mostrador es común una tablet; hoy se ve como un teléfono estirado. `sw600dp` con listas y detalle
en dos paneles.

### 15. Buscador global en gestión
Buscar un **código de retiro** sin saber de antemano si fue venta o renta. Hoy hay que elegir la pantalla
primero.

### 16. Widget y accesos directos
"Nueva venta" desde el escritorio del teléfono.

---

## Orden de ejecución acordado

1. **Room** (ítem 7) — plan aprobado y detallado; es lo que más se nota al usar la app.
2. **Crashlytics + firma + R8 + versionado** (1-4) — poco trabajo, desbloquea publicar.
   ⚠️ **Crashlytics conviene antes de tener usuarios reales**, no después.
3. **Tests** (5) — empezando por carrito, checkout y cobros.
4. **Strings** (6) — mecánico, se puede intercalar.
5. **Bloque 3** (8-12) según se acerque la publicación.
6. **Bloque 4** (13-16) según lo que pida el mercado.

**Regla de trabajo:** un ítem por rama y por PR. Pequeño, verificable y reversible.
