# Costumi — Plan de persistencia local con Room (offline y caché)

> **Qué es este documento.** La guía completa para implementar la capa local de la app: qué se guarda,
> qué **no**, con qué patrón, con qué reglas y cómo se prueba. Se escribió para poder ejecutarlo sin
> improvisar y sin atajos.
>
> **Estado:** plan aprobado, implementación pendiente. Marcar cada ítem al terminarlo.
>
> **Origen:** `ORDEN_CONSTRUCCION.md` §B.4 (*"cache-first con Room"*) y **Fase 8** (*"Room como fuente de
> verdad en todas las listas"*), `DISENO_PANTALLAS.md` (*"la UI observa Room; el repositorio trae de la red
> y guarda en Room"*), `CLAUDE.md` §5.6 (`Retrofit → repositorio → Room → UI`) y RF-16.6 / RF-17.5 /
> RF-18.12 del spec. Hoy solo está implementado para **empresas**; el resto es lo que falta.

---

## 0. Por qué se hace

Sin caché local, cada pantalla arranca en blanco y depende de que haya red. Con caché:

- La pantalla **abre al instante** con lo último guardado y refresca por detrás. Sin spinner en blanco.
- La app **sirve sin conexión** para consultar (no para operar).
- Se **gasta menos dato móvil**, que en un empleado en la calle importa.

No se hace por cumplir un requisito: se hace porque cambia cómo se siente la app.

---

## 1. Principio rector: qué se cachea y qué no

> **La regla, en una frase: se cachea lo que DESCRIBE, nunca lo que DECIDE.**

| Se cachea (describe) | NO se cachea (decide) |
|---|---|
| Nombres, fotos, descripciones | **Stock disponible** |
| Precios de catálogo (informativos) | **Saldos y multas al momento de cobrar** |
| Categorías y etiquetas | **Disponibilidad de fechas de una renta** |
| Historial (pedidos pasados: ya no cambian) | **Estado de una operación en curso** |
| Sucursales de una tienda | **Totales de un checkout** |

**Por qué esta línea.** Mostrar un nombre viejo es un detalle estético. Mostrar un **stock** viejo hace
que dos empleados renten la misma prenda y quede un cliente sin su disfraz en el mostrador. El daño no
es simétrico, así que el criterio no puede ser simétrico.

**Corolario:** cuando una pantalla mezcla ambos (p. ej. catálogo con disponibilidad), se cachea la parte
descriptiva y la decisiva se pide siempre a la red.

---

## 2. El patrón: Room como fuente única de verdad (SSOT)

El patrón **ya existe y funciona** en `MarketplaceRepository` (empresas). Es el modelo a replicar; no se
inventa uno nuevo.

```
UI  ──observa──►  Flow del DAO  ◄──escribe──  Repositorio  ──pide──►  Retrofit
     (nunca pide a la red directamente)                    (única puerta a la red)
```

**Reglas del patrón:**

1. La UI **siempre** lee de un `Flow` de Room. Nunca consume la respuesta de red directamente.
2. El repositorio expone **dos** cosas por dato cacheado:
   - `observarX(): Flow<List<XEntity>>` → lectura, desde el DAO.
   - `suspend refrescarX(): RespuestaRed<Unit>` → trae de la red y **escribe en Room**. Devuelve el
     resultado solo para poder mostrar el error; **los datos llegan por el Flow**.
3. **Nunca** se devuelven entidades de Room a la UI sin necesidad; se mapean a lo que la pantalla usa
   (o se usa la entidad si es un espejo directo, como hoy con `EmpresaEntity`).
4. Escribir es **reemplazar en una transacción**, no borrar-y-luego-insertar en pasos sueltos: si falla a
   la mitad, la lista queda vacía en pantalla.

**Ejemplo de referencia (ya en el código):**

```kotlin
fun observarEmpresas(): Flow<List<EmpresaEntity>> = empresaDao.observarTodas()

suspend fun refrescarEmpresas(buscar: String?): RespuestaRed<Unit> = withContext(dispatchers.io) {
    // ... llama a la red y, si hay éxito:
    empresaDao.reemplazar(r.data.mapNotNull { it.aEntity() })
}
```

⚠️ **Cuidado documentado:** `refrescarEmpresas` **reemplaza toda la tabla**. Por eso la búsqueda de
tiendas NO lo reutiliza (ver `PENDIENTE_FRONTEND.md`): buscar "pira" borraría el resto de la caché.
**Regla:** un método que reemplaza la tabla completa solo se usa para la carga completa, jamás para
resultados filtrados.

---

## 3. Normas duras (no negociables)

Estas reglas existen porque cada una corresponde a un error concreto que se paga caro.

### N1 — La caché se borra al cerrar sesión
**Es una norma de seguridad, no de limpieza.** Si no se borra, el siguiente usuario del teléfono ve los
pedidos, multas y datos del anterior. En un negocio con un teléfono compartido en mostrador, es una fuga
de datos personales.

- Se implementa en `AuthRepository.logout()`, junto a `sesion.limpiar()` y `miEmpresa.limpiar()`.
- Debe borrar **todas** las tablas, no las de la pantalla actual.
- **Test obligatorio**: tras logout, todos los DAOs devuelven vacío.

### N2 — Nada sensible en la caché
No se guardan en Room: contraseñas, tokens, números de tarjeta, ni el `device_token` de push. Los tokens
siguen en `EncryptedSharedPreferences`. Room **no está cifrado**.

### N3 — La caché nunca decide, solo muestra
Ninguna acción que escriba en el servidor (checkout, cobro, devolución, ajuste de stock) puede tomar como
entrada un dato leído de Room. Siempre se relee del servidor antes de operar.

### N4 — El usuario tiene que saber que está viendo datos guardados
Si se muestra caché porque no hay red, la pantalla lo dice (banner/etiqueta "sin conexión · datos del
<hora>"). Mostrar datos viejos **sin avisar** es peor que mostrar un error: el usuario toma decisiones
creyendo que está al día.

### N5 — Toda escritura en Room va en `withContext(dispatchers.io)`
Nunca en el hilo principal. Se usa el `DispatcherProvider` inyectado, **no** `Dispatchers.IO` directo
(lo segundo no es testeable). Regla que ya cumple el resto del proyecto.

### N6 — Un DAO por entidad, en `data/local/dao/`
Sin DAOs "cajón de sastre" con consultas de varias tablas. Nombres en español, consistentes con el
proyecto: `observarTodas()`, `reemplazar()`, `borrarTodo()`.

### N7 — Migraciones: destructivas mientras es caché
Al ser **solo caché** (el servidor es la verdad), perder la base local no pierde información: se vuelve a
descargar. Se mantiene `fallbackToDestructiveMigration`. **Si algún día se guarda algo que no está en el
servidor** (borradores, outbox), esta regla cambia y hay que escribir migraciones reales.

### N8 — Cada entidad nueva sube la versión de la base
`@Database(version = N)`. Olvidarlo hace que la app crashee al abrir en un dispositivo con la versión
anterior instalada.

### N9 — Nada de `SELECT *` implícito hacia la UI
Las entidades guardan **solo los campos que la pantalla pinta**. No se replica el DTO completo del
backend "por si acaso": cada campo guardado es un campo que hay que migrar y mantener.

---

## 4. Estructura de archivos

Se respeta la que ya existe:

```
data/local/
├── CostumiDatabase.kt          ← registrar aquí cada entidad y DAO nuevos
├── dao/
│   ├── EmpresaDao.kt           (ya existe)
│   ├── PrendaVitrinaDao.kt     ← nuevo
│   ├── DisfrazVitrinaDao.kt    ← nuevo
│   ├── SucursalDao.kt          ← nuevo
│   ├── PedidoDao.kt            ← nuevo
│   ├── DeudaDao.kt             ← nuevo
│   └── RemoteKeysDao.kt        ← nuevo (solo para el RemoteMediator)
└── entity/
    ├── EmpresaEntity.kt        (ya existe)
    └── ...                     ← una por DAO
```

**Convención de nombres:** entidad `XEntity` con `@Entity(tableName = "x")` en minúscula singular, igual
que `EmpresaEntity` → `"empresa"`.

**Mapeadores:** las funciones `DTO → Entity` y `Entity → modelo de UI` van en `data/repo/Mapeadores.kt`,
donde ya viven las demás. No se ponen dentro de la entidad.

---

## 5. Trabajo a realizar

### Bloque A — Datos del cliente

Cada ítem sigue el mismo procedimiento (§6). El orden es por valor descendente.

- [ ] **A1. Catálogo de prendas por tienda** (`PrendaVitrinaEntity`)
  - Clave: `id`; **índice por `empresaId`** (siempre se consulta por tienda).
  - Campos: id, empresaId, nombre, fotoUrl, precioRenta, precioVenta, categoriaNombre, tipoArticulo.
  - ⚠️ **No** guardar disponibilidad ni stock (N3).
  - Repositorio: `MarketplaceRepository`.

- [ ] **A2. Disfraces por tienda** (`DisfrazVitrinaEntity`)
  - Igual que A1. Guardar el precio sugerido y el tipo; **no** la disponibilidad (se deriva del stock).
  - Los **slots** no se cachean en esta fase: son estructura anidada y solo se ven al abrir el detalle.

- [ ] **A3. Sucursales por tienda** (`SucursalEntity`)
  - Cambian casi nunca y se piden en cada pedido. Índice por `empresaId`.

- [ ] **A4. Mis pedidos** (`PedidoEntity` + `LineaPedidoEntity`)
  - **El mejor candidato**: es historial, ya no cambia.
  - Relación 1-N; borrado en cascada de las líneas al reemplazar el pedido.
  - Cuidado: la agrupación por disfraz (`disfrazGrupo`) debe conservarse — ver `LineaPedidoAdapter`.

- [ ] **A5. Mis multas** (`DeudaEntity`)
  - Útil de consultar sin red. **Se muestra con la marca de N4**: una multa que ya se pagó y sigue
    apareciendo genera un reclamo. El importe siempre se reconfirma contra el servidor antes de pagar (N3).

- [ ] **A6. Mi perfil y mi tienda** (`PerfilEntity`, `MiEmpresaEntity`)
  - Trivial, evita el parpadeo al abrir Perfil y el Panel.
  - `MiEmpresaRepository` ya cachea en memoria: se pasa a Room y se elimina la caché en memoria (una sola
    fuente de verdad, no dos).

### Bloque B — Transversales (obligatorios si se hace A)

- [ ] **B1. Borrado de la caché al cerrar sesión** (N1) — con test.
- [ ] **B2. Subir `version` de `CostumiDatabase`** y verificar que instala sobre la versión anterior (N8).
- [ ] **B3. Indicador de "sin conexión / datos guardados"** en las pantallas con caché (N4).
      Reutilizar `StateView`; ya existe el estado de sin conexión en el diseño.
- [ ] **B4. Tests de DAOs** con base en memoria (§7).

### Bloque C — Inventario con Paging 3 + RemoteMediator

> **Solo Inventario.** Ventas y Rentas quedan **fuera a propósito**: son datos operativos donde ver algo
> desactualizado provoca doble reserva y errores de cobro (§1).

- [ ] **C1. `PrendaInventarioEntity`** — espejo de lo que pinta la lista de inventario.
- [ ] **C2. `RemoteKeysEntity`** — página siguiente/anterior por prenda. Sin esto el mediator no sabe
      por dónde va.
- [ ] **C3. `PrendaRemoteMediator`** — implementa `load()` para `REFRESH`, `PREPEND`, `APPEND`.
  - `REFRESH`: limpia tabla + keys y pide la página 0 **en una transacción**.
  - `APPEND`: pide la siguiente según la última key; `endOfPaginationReached` cuando la página viene vacía.
  - `PREPEND`: devolver `endOfPaginationReached = true` (no se pagina hacia atrás en esta app).
- [ ] **C4. Cambiar `PrendasPagingSource`** por `pager.flow` con `remoteMediator` y el DAO como fuente.
- [ ] **C5. Política de frescura**: si la caché tiene más de **X minutos**, forzar `REFRESH`.
      Valor propuesto: **5 minutos**. Se guarda `actualizadoEn` por tabla.

**Riesgo conocido de C:** es la parte más difícil de Paging 3 y la más difícil de depurar. Se hace al
final, después de que A y B estén verdes, y en su propia rama.

---

## 6. Procedimiento por cada ítem (no saltarse pasos)

1. **Entidad** en `data/local/entity/`, con los campos mínimos que la pantalla pinta (N9) y los índices
   por los que se consulta.
2. **DAO** en `data/local/dao/`: `observarX(): Flow<...>`, `reemplazar(...)` con `@Transaction`,
   `borrarTodo()`.
3. **Registrar** entidad y DAO en `CostumiDatabase` y **subir la versión** (N8).
4. **Proveer el DAO** en `DatabaseModule`.
5. **Mapeadores** DTO↔Entity en `Mapeadores.kt`.
6. **Repositorio**: agregar `observarX()` y `refrescarX()` (§2). El resto del repositorio no cambia.
7. **ViewModel**: pasar a observar el `Flow` del repositorio; llamar a `refrescarX()` al iniciar y en
   swipe-to-refresh. El estado de carga refleja el refresco, no la lectura local.
8. **Pantalla**: no cambia su estructura; ya usa `observar()` + `StateView`.
9. **Añadir la tabla al borrado de sesión** (N1). **Se hace en el mismo commit**, no después.
10. **Tests** (§7).
11. **Compilar y probar el flujo real**: abrir con red, matar la red, volver a abrir.

---

## 7. Tests obligatorios

Hoy la app tiene **1 test**. Esta capa es la oportunidad de empezar bien, porque es la parte más testeable.

**Por cada DAO** (`app/src/androidTest/`, base en memoria con `Room.inMemoryDatabaseBuilder`):
- [ ] Insertar y leer devuelve lo insertado.
- [ ] `reemplazar` deja **solo** lo nuevo (no acumula duplicados).
- [ ] `observarX()` **emite otra vez** al escribir (es lo que hace que la UI se actualise sola).
- [ ] `borrarTodo` vacía la tabla.

**Por cada repositorio con caché** (unit test, `app/src/test/`, con DAO y API falsos):
- [ ] Con red OK: escribe en el DAO lo que devolvió la API.
- [ ] Con red caída: **no borra** lo que ya había y devuelve el error.
- [ ] El `Flow` sigue emitiendo la caché aunque la red falle.

**Transversal:**
- [ ] Tras `logout()`, todos los DAOs quedan vacíos (N1).

**Del RemoteMediator (C):**
- [ ] `REFRESH` limpia y carga la primera página.
- [ ] `APPEND` agrega sin duplicar.
- [ ] Página vacía → `endOfPaginationReached`.

---

## 8. Definición de "hecho"

Un ítem no se marca hasta cumplir **todo** esto:

- [ ] La pantalla **abre con datos** al segundo intento, sin spinner en blanco.
- [ ] **En modo avión** muestra lo guardado, con el aviso de N4.
- [ ] Al **cerrar sesión** la tabla queda vacía (verificado, no asumido).
- [ ] Los tests de §7 pasan.
- [ ] `./gradlew :app:assembleDebug` en verde **filtrando por `BUILD`**, no por `^e:`
      (los errores de recursos XML no aparecen con `^e:` — está documentado en la memoria del proyecto).
- [ ] No se cachea nada de la columna "NO se cachea" de §1.
- [ ] Documentado en `PENDIENTE_FRONTEND.md`.

---

## 9. Errores a evitar (aprendidos en este proyecto)

1. **Reutilizar un `refrescar` que reemplaza toda la tabla para una búsqueda filtrada.** Borra la caché.
   Ya pasó con `refrescarEmpresas`; está documentado.
2. **Dejar dos fuentes de verdad** (caché en memoria + Room para el mismo dato). Se elige una: Room.
3. **Olvidar el borrado por sesión** al agregar una tabla. Por eso el paso 9 de §6 va en el mismo commit.
4. **Guardar el DTO completo** porque "es más fácil". Cada campo se paga en migraciones.
5. **Cachear disponibilidad o stock.** Es la regla que más tienta romper y la que más caro sale.
6. **Emitir `UiState.Loading` mientras hay caché**: si hay datos guardados, se muestran; el refresco se
   indica con el spinner del swipe-to-refresh, no tapando la pantalla.
7. **Ids compartidos entre layouts distintos con `StateView`** → `ClassCastException` de ViewBinding.
   Documentado en la memoria del proyecto.

---

## 10. Orden de ejecución sugerido

1. **B1 + B2** primero (el borrado por sesión y el versionado): así toda tabla nueva nace protegida.
2. **A4 (Mis pedidos)**: el caso más claro y el que mejor demuestra el patrón.
3. **A1, A2, A3** (catálogo, disfraces, sucursales): el grueso del valor para el cliente.
4. **A5, A6**: rápidos.
5. **B3 + B4**: aviso de sin conexión y tests.
6. **C** completo, en rama aparte, al final.

Cada bloque es un PR: pequeño, verificable y reversible.
