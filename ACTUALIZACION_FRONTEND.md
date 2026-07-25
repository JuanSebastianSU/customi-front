# Actualización del frontend Android — cambios de backlog backend (2026-07-19)

Este documento lista **todo lo que hay que tocar en la app** (`AppCustomi2`) para quedar al día con
las últimas rebanadas del backend. Está ordenado por prioridad: primero lo que **rompe la compilación**
tras regenerar el cliente, después las **mejoras de UI** (opcionales pero recomendadas) y al final lo
que **no requiere cambios**.

> Regla de oro: nada de esto es urgente para que la app siga funcionando contra Railway. La app actual
> sigue andando; estos cambios son para **exponer las features nuevas** y mantener el `:api-client`
> sincronizado con el contrato.

## Backend que cambió (contexto)
Cuatro rebanadas ya mergeadas/en PR, todas verificadas con la suite completa en Docker:

| PR / rama | RF | Qué agregó al contrato |
|---|---|---|
| `feat/aviso-dueno-vencidas` (mergeada) | RF-11.1 | Enum `CanalNotificacion` gana **`IN_APP`** (aviso al dueño) |
| `feat/backlog-avisos-y-escalacion` (en PR) | RF-11.2 | Nuevo **`POST /api/v1/notificaciones/avisar-stock-bajo`** |
| `feat/backlog-avisos-y-escalacion` (en PR) | RF-11.5/11.6 | **`GET /api/v1/clientes`** gana el parámetro **`filtro`** (`PENDIENTES\|VENCIDAS\|MULTAS\|SALDOS`) |
| `feat/backlog-avisos-y-escalacion` (en PR) | RF-15.4 | (sin cambios de API — solo un job que loguea) |

---

## Paso 0 — Regenerar el `:api-client` (obligatorio)
Hacelo **después** de que la PR esté mergeada y Railway haya redeployado (para tomar el contrato real).

1. Bajar el contrato desplegado a `openapi.json`:
   `https://just-upliftment-production-cb1f.up.railway.app/v3/api-docs`
2. Regenerar con el mismo generador de siempre (openapi-generator, kotlin + jvm-retrofit2 + gson).
3. **Ojo con la renumeración `listarN`/`actualizarN`:** agregar un endpoint nuevo (aquí,
   `avisar-stock-bajo`) puede correr la numeración de métodos generados que comparten nombre base.
   **No adivines**: recompilá y dejá que el compilador te marque cada referencia rota. Los puntos
   conocidos a revisar están abajo.

---

## 1) Cambios que ROMPEN la compilación tras regenerar (obligatorios)

### 1.1 `GET /clientes` ahora recibe `filtro` → cambia la firma de `listar13(...)`
El método generado `ClienteControllerApi.listar13(...)` gana un parámetro nuevo (`filtro: String?`).
Hay **dos llamadas** que actualizar:

- **[ClientesPagingSource.kt](app/src/main/java/com/costumi/app/data/repo/ClientesPagingSource.kt)**
  (línea ~22): pasar el filtro. Requiere:
  - Agregar `private val filtro: String?` al constructor de `ClientesPagingSource`.
  - En `api.listar13(...)` agregar `filtro = filtro`.
- **[NotificacionRepository.kt](app/src/main/java/com/costumi/app/data/repo/NotificacionRepository.kt)**
  (línea ~42, método `clientes()`): la llamada `clienteApi.listar13(null, false, false, 0, 100)` pasa a
  `clienteApi.listar13(null, false, null, false, 0, 100)` (o usando nombres de parámetro). Verificá el
  **orden exacto** de los parámetros en el método regenerado antes de tocar.

Y para propagar el filtro desde la UI:
- **[ClientesRepository.kt](app/src/main/java/com/costumi/app/data/repo/ClientesRepository.kt)** (`clientes(...)`,
  línea ~29): agregar el parámetro `filtro: String?` y pasarlo al `ClientesPagingSource`.
- **[ClientesViewModel.kt](app/src/main/java/com/costumi/app/ui/gestion/clientes/ClientesViewModel.kt)**:
  agregar `filtro: String? = null` al `data class FiltrosClientes` (línea ~19) y pasarlo en
  `repo.clientes(it.buscar, it.filtro, it.incluirArchivados)` (línea ~35).

> Si querés el cambio **mínimo** para solo compilar (sin UI de filtros todavía): pasá `filtro = null` en
> las dos llamadas a `listar13` y listo. El comportamiento queda idéntico al de hoy.

### 1.2 Revisar renumeración de otros `listarN`/`actualizarN`
Por el endpoint nuevo, recompilá y corregí lo que marque el compilador. Históricamente se movieron, por
ejemplo, referencias en `AuditoriaRepository` y `ConfiguracionRepository`. **El compilador es la fuente
de verdad** — no cambies números a ojo.

---

## 2) Mejoras de UI recomendadas (no rompen; exponen las features nuevas)

### 2.1 Notificaciones — canal `IN_APP` y avisos al dueño (RF-11.1 / RF-11.2)
Los resúmenes al dueño (devoluciones vencidas, stock bajo) llegan con **`canal = "IN_APP"`** y
**`clienteId = null`**. Hoy la bandeja los muestra, pero poco claros:

- **[NotificacionAdapter.kt](app/src/main/java/com/costumi/app/ui/gestion/notificaciones/NotificacionAdapter.kt)**
  (línea ~27): `IN_APP` se renderiza como `"In_app"`. Mapealo a una etiqueta amigable, p. ej.
  `"App"` o `"Aviso interno"`. Sugerencia: un `when (n.canal) { "IN_APP" -> "Aviso interno"; "WHATSAPP" -> "WhatsApp"; ... }`.
- Línea ~31: `nombreCliente(n.clienteId)` con `clienteId == null` (aviso al negocio) debería mostrar
  algo como **"Tu negocio"** en vez de vacío. Ajustá el lambda `nombreCliente` para el caso `null`.
- Opcional: un ícono/orden distinto para los avisos internos, para que el dueño los distinga de los
  mensajes a clientes.

### 2.2 Clientes — filtros por categoría (RF-11.5/11.6)
Nueva capacidad: filtrar la lista de clientes por **PENDIENTES / VENCIDAS / MULTAS / SALDOS**.

- En **[ClientesFragment](app/src/main/java/com/costumi/app/ui/gestion/clientes/ClientesFragment.kt)**
  agregar un `ChipGroup` (selección única) con esas 4 categorías + "Todos".
- Al elegir un chip, llamar a un nuevo `viewModel.filtrar(categoria)` que haga
  `_filtros.value = _filtros.value.copy(filtro = categoria)`.
- Semántica para mostrar al dueño:
  - **PENDIENTES**: tiene rentas activas por devolver **o** algún saldo por cobrar (indicador general).
  - **VENCIDAS**: renta activa cuya fecha de devolución ya pasó.
  - **MULTAS**: incurrió en una multa (daños/retraso que superaron el depósito), pagada o no.
  - **SALDOS**: debe dinero (lo pagado no cubre importe + multa de alguna renta).
- El toggle actual de "con pendientes" sigue funcionando: `conPendientes=true` equivale a
  `filtro=PENDIENTES`. Podés reemplazarlo por el ChipGroup o dejar ambos.

### 2.3 (Opcional) Botón manual de "avisar stock bajo"
El aviso de stock bajo es **automático** (job diario), así que no hace falta UI. Si querés un disparador
manual para el dueño (p. ej. en Dashboard/Inventario), agregá en
**[NotificacionRepository.kt](app/src/main/java/com/costumi/app/data/repo/NotificacionRepository.kt)** un
método `avisarStockBajo()` que llame al nuevo endpoint generado (`notificacionApi.avisarStockBajo()` o el
nombre que le ponga el generador) y mostrá el `enviadas` en un snackbar. La respuesta reusa
`RecordatorioResponse(enviadas)`.

---

## 3) Sin cambios en la app
- **RF-15.4 (escalación de solicitudes vencidas):** es un job de plataforma que solo **emite un log
  WARN** (lo levanta la observabilidad/SigNoz). No hay endpoint ni pantalla nueva. Nada que tocar.
  Si en el futuro se quiere una bandeja de alertas para el SUPERADMIN, sería una feature aparte.
- **Schedulers (vencidas, próximas, stock bajo, escalación):** corren solos en el backend; la app no los
  invoca (salvo los botones manuales ya existentes de recordatorios).

---

## Checklist
- [ ] PR `feat/backlog-avisos-y-escalacion` mergeada y Railway redeployado.
- [ ] `openapi.json` actualizado desde `/v3/api-docs` y `:api-client` regenerado.
- [ ] Compila: corregidas las llamadas a `listar13` (filtro) y cualquier `listarN`/`actualizarN` renumerado.
- [ ] (UI) Etiqueta amigable para `IN_APP` y destinatario "Tu negocio" cuando `clienteId == null`.
- [ ] (UI) ChipGroup de filtros en Clientes (PENDIENTES/VENCIDAS/MULTAS/SALDOS).
- [ ] (Opcional) Botón manual de aviso de stock bajo.
- [ ] Prueba E2E en el teléfono con el DUEÑO de demo (`dueno.demo.635935@costumi.test`).

---

# Actualización 2 — recorrido de compra + disfraz completo + pagos (2026-07-20)

Esto cubre las ramas **#111** (`feat/mis-pedidos-y-tiendas-operables`) y **`feat/codigo-de-retiro`** (que trae TODO el
disfraz + el código de retiro). **Al mergearlas, regenerar `:api-client`** y actualizar la app como sigue.

## Reglas de negocio confirmadas por el dueño (para diseñar bien las pantallas)
- **No hay adelanto.** Todo se paga **de golpe**: con **tarjeta → pago en línea del total**; con **efectivo → se paga todo
  en la tienda** (el pedido queda con su código de retiro).
- **El disfraz no tiene depósito.** Se paga por **su precio** (el que el dueño le puso; la suma de las prendas es solo la
  **sugerencia** que ve el dueño al armarlo y puede cambiar). Si al devolver está dañado, se cobra la **multa** que el dueño
  definió por prenda (no un depósito).
- La tienda muestra **dos apartados separados: DISFRACES y PRENDAS**.

## Contrato nuevo (campos y endpoints que aparecen tras regenerar)
- **Renta/Venta** (`RentaResponse`/`VentaResponse`): nuevo campo **`codigoRetiro`** (`R-XXXXXXXX` / `V-XXXXXXXX`). Cada
  **línea** trae ahora `nombre` + `fotoUrl` de la prenda (además de `prendaId`, cantidad, precio).
- **"Mis Pedidos"/historial** (`HistorialItem`): nuevo campo **`lineas`** (lista de artículos con `prendaId, nombre, fotoUrl,
  cantidad, precioUnitario`). Antes el historial no traía los artículos → por eso "Mis Pedidos" salía vacío de ítems.
- **Vitrina de prendas** (`PrendaVitrinaResponse`): nuevo campo **`fotoUrl`**.
- **Disfraz** (`DisfrazResponse`): nuevos campos **`precioRentaSugerido`**, **`precioVentaSugerido`** y **`fotoUrl`**.
- **Endpoints nuevos del disfraz:**
  - `POST /api/v1/disfraces/{id}/foto` (multipart `archivo`) — el dueño sube la foto del disfraz.
  - `POST /api/v1/disfraces/{id}/vender` — vender el disfraz (mismo cuerpo que rentar pero **sin fechas**: `sucursalId`,
    `empresaId` (cliente), `clienteId`, `selecciones`). Devuelve `{ ventaId }`.
- **Marketplace de disfraces (ya existía, ahora con precio/foto):**
  `GET /api/v1/marketplace/empresas/{empresaId}/disfraces` (listar, público),
  `.../disfraces/{disfrazId}` (detalle + disponibilidad),
  `.../disfraces/{disfrazId}/slots/{orden}/opciones` (la "ruleta").

## Cambios OBLIGATORIOS tras regenerar (el compilador los marca)
- Ver Actualización 1 (el `filtro` en `listar13` de clientes sigue vigente).
- Revisar que las llamadas a métodos generados renumerados (`listarN`/`actualizarN`) compilen; corregir lo que marque.

## Pantallas / UI a construir o ajustar
1. **Tienda del cliente con DOS apartados:** un tab/sección de **Disfraces** (del catálogo `/marketplace/.../disfraces`, con
   **foto** y **precio**) y otro de **Prendas** (del catálogo `/marketplace/.../catalogo`, con **foto** — ahora expone `fotoUrl`).
2. **Pintar las fotos** (Coil/ImageView) en: listado de disfraces, listado de prendas (cliente y gestión), detalle, y en las
   líneas de los pedidos. Coil ya está en el proyecto; falta usarlo en los adapters/layouts (hoy no hay `ImageView`).
3. **Detalle del disfraz (cliente):** muestra su **foto**, su **precio** (renta y/o venta), y sus **piezas/slots**; para los
   slots personalizables, la "ruleta" (`/slots/{orden}/opciones`) para elegir prenda; ahí se ven las fotos de las prendas.
   Botones **Rentar** y **Comprar** (comprar → `POST /disfraces/{id}/vender`).
4. **Pantalla de PAGO (cliente):** tras confirmar el pedido, mostrar el **desglose** (artículos con imagen + total) y elegir:
   - **Tarjeta →** pago en línea del **total** (flujo MercadoPago; el backend expondrá el intento por el total).
   - **Efectivo →** se genera el pedido con su **código de retiro**; "pasá por la tienda a pagar y retirar".
5. **Mostrar el `codigoRetiro`** en el detalle del pedido y en "Mis Pedidos" (para que el cliente lo dé en la tienda).
6. **"Mis Pedidos"** ahora tiene `lineas` → listar los **artículos con imagen** dentro de cada pedido (hoy sale sin ítems).
7. **Foto del disfraz (gestión):** en el form del disfraz, permitir que el dueño **suba la foto** (`POST /disfraces/{id}/foto`),
   idealmente en el mismo flujo de crear/editar. Mostrar el **precio sugerido** (`precioRentaSugerido`/`precioVentaSugerido`)
   como ayuda, con el campo editable del precio general.
8. **Vender disfraz (gestión, modo asistido):** botón para vender un disfraz a un cliente (`/vender`).
9. Ver Actualización 1: chips de filtro de clientes (PENDIENTES/VENCIDAS/MULTAS/SALDOS), etiqueta `IN_APP` y destinatario
   "Tu negocio" en notificaciones, foto al crear prenda en un solo formulario.

## Nota sobre el backend que TODAVÍA falta (afecta la pantalla de pago)
La pantalla de pago depende de piezas de backend aún pendientes: que la **tarjeta cobre el total completo**, que se **valide
el monto** del intento, y que el **checkout exija el pago**. Coordinar: primero cerrar eso en el backend, después cablear la
pantalla de pago del cliente.

---

# Actualización 3 — pagos, catálogo por categoría, expiración (2026-07-20)

Cierra el backend del recorrido de compra. Ramas pendientes de merge (cada una desde main, sin conflictos):
`feat/pagos-total-de-golpe`, `feat/codigo-en-mis-pedidos`, `feat/expirar-reservas`, `feat/marketplace-filtro-categoria`.
**Al mergearlas, regenerar `:api-client`.**

## Contrato nuevo / cambios de comportamiento
- **Catálogo por categoría:** `GET /api/v1/marketplace/empresas/{id}/catalogo` gana el query param opcional
  **`categoria`** (id de categoría). El método generado del catálogo gana ese parámetro → en el apartado de **Prendas**
  de la tienda, agregar un filtro (chips/dropdown) de categorías que lo pase.
- **Pago en línea = total exacto:** `POST /api/v1/pagos/intento` ahora **exige** que `monto` sea el total pendiente del
  concepto (renta/venta). Si mandás un monto que no lo cubre, responde **400**. En la pantalla de pago, cuando el cliente
  elige **tarjeta**, mandá como `monto` el **total del pedido** (el backend además lo valida). No hay pagos parciales.
- **"Mis Pedidos" trae `codigoRetiro`** (además de `lineas`): mostralo en cada pedido.

## Reglas para la UX de pago (confirmadas por el dueño)
- **Tarjeta → pago en línea del total, obligatorio.** El pedido no vale sin ese pago. (En backend: si no se paga, el pedido
  **expira a las 24 h** y se cancela solo. La app debería avisar "tenés 24 h para completar el pago" o directamente llevar
  al pago en línea al confirmar con tarjeta.)
- **Efectivo → reserva.** Se crea el pedido con su código de retiro; el cliente paga en la tienda al retirar. También expira
  a las **24 h** si no se retira/paga. Mostrar ese plazo al cliente para que no se lleve una sorpresa.

## Nada que hacer en la app por estas piezas
- La **expiración de reservas** es un job del backend; la app no lo invoca. Solo conviene **mostrar el plazo de 24 h** y que
  un pedido puede aparecer **CANCELADA** si venció (ya se refleja en el estado del pedido en "Mis Pedidos").

## Corrección (rama feat/ruleta-foto)
La "ruleta" de un slot personalizable (`GET /marketplace/.../disfraces/{d}/slots/{orden}/opciones`) ahora devuelve
**`fotoUrl`** en cada opción, además de nombre/precio/stock/etiquetas. → En la pantalla de la ruleta, **pintar la foto**
de cada prenda-opción (con Coil) para que el cliente elija viendo la imagen.

---

# Actualización 4 — pago en línea del CLIENTE (rama feat/pago-en-linea-cliente) (2026-07-20)

**Por qué:** el `POST /api/v1/pagos/intento` que ya existía es **modo asistido por personal** (saca la tienda del token
`empresa_id`), así que **un cliente puro del marketplace NO puede usarlo** (su token no lleva `empresa_id`). Sin esto,
la pantalla de pago con **tarjeta** del cliente no tenía backend. Ya está cerrado.

## Endpoint nuevo (tras regenerar el `:api-client`)
- **`POST /api/v1/pagos/intento/cliente`** — solo rol **CLIENTE**. Cuerpo (`IntentoDePagoDeClienteRequest`):
  `empresaId` (la tienda), `sucursalId`, `tipoConcepto` (`VENTA`|`RENTA`), `conceptoId` (la venta/renta), `monto`,
  `moneda`. Devuelve `IntentoDePagoResponse { intentoId, urlCheckout }` (la URL de la pasarela para abrir el checkout).
- **Autorización:** el backend resuelve la ficha del cliente por su token y verifica que la venta/renta sea **suya**;
  si no, **403**. El cliente NO manda su `clienteId` (sale del token), igual que en carrito/checkout.
- **Monto:** se paga **todo de golpe**. El backend valida que `monto` == total pendiente del concepto; si es parcial o
  no cubre, **400**. Es decir, en la pantalla de pago con tarjeta mandá como `monto` el **total del pedido**.
- **Requisito de la tienda:** la empresa debe tener el switch **`pagoEnLinea`** activo; si no, **409**
  ("Pago en línea deshabilitado") → en ese caso la app debería ofrecer solo **efectivo** (reserva + código de retiro).

## Cómo cablear la pantalla de pago (cliente)
1. Tras el checkout (que crea la venta/renta y devuelve su id + `codigoRetiro`), mostrar el desglose y los dos métodos:
   - **Tarjeta →** llamar `POST /pagos/intento/cliente` con el total; abrir `urlCheckout` (WebView/navegador). El pago se
     confirma por el **webhook** de la pasarela (la app no confirma nada). Avisar "tenés 24 h para completar el pago"
     (si no, la reserva **expira** y se cancela sola).
   - **Efectivo →** no se llama a pagos; el pedido ya quedó con su **código de retiro**. "Pasá por la tienda a pagar y
     retirar" (también expira a las 24 h).
2. El endpoint del **personal** sigue siendo `POST /pagos/intento` (para cobros asistidos en mostrador); **no** lo use
   la app del cliente.

> Nota de generación: al agregar `intento/cliente`, el método generado del intento del personal puede renombrarse
> (`intento` vs `intentoDeCliente`/numeración). Como siempre: recompilá y dejá que el compilador marque las referencias.

---

# Actualización 5 — nombre + foto por línea del carrito (rama feat/carrito-nombre-foto) (2026-07-20)

**Por qué:** el carrito (`GET /carritos` y `POST /carritos/items`) sólo devolvía `prendaId` por línea, así que el
carrito mostraba "Articulo xN" sin nombre ni imagen. Ya está enriquecido.

## Contrato nuevo (tras regenerar el `:api-client`)
- **`LineaDeCarritoResponse`** gana **`nombre`** y **`fotoUrl`** (además de `prendaId, cantidad, fechaRetiro,
  fechaDevolucion, precioUnitario, subtotal`). Se poblan igual que las líneas de "Mis Pedidos".

## A cablear en la app (al mergear + regenerar)
- **[CarritoLineaAdapter.kt](app/src/main/java/com/costumi/app/ui/cliente/carrito/CarritoLineaAdapter.kt):** hoy escribe
  `"Articulo  x $cantidad"`. Cambiar a usar `linea.nombre` (fallback "Articulo") y **pintar `linea.fotoUrl`** con el helper
  `ui/Imagenes.kt` `cargarFoto`. Añadir un `ImageView` (miniatura ~56dp) a
  **[item_linea_carrito.xml](app/src/main/res/layout/item_linea_carrito.xml)** (mismo patrón que `item_linea_pedido.xml`).
- Nada más rompe por este cambio (sólo se agregan campos al modelo).

---

# Deuda técnica / pendiente (no es front — documentado 2026-07-20)

- **Push al cliente (device-token):** bloqueado. (a) El endpoint `PUT /clientes/{id}/device-token` es **solo-staff** en el
  backend → el cliente no puede registrar su token (haría falta abrir una vía self-service). (b) Falta un **proyecto
  Firebase/FCM** (`google-services.json` + credenciales), que sólo lo crea el usuario. Sin push la app anda; sólo faltan
  avisos con la app cerrada.
- **Credenciales de producción (Railway env vars) por aplicar:** **MercadoPago** (sin esto el pago con tarjeta no genera
  un cobro real, aunque la app abra la URL), **WhatsApp** (Meta/Twilio), **SMTP**, **FCM**. Todo el código está "listo para
  credencial"; es config del usuario en Railway. (S3 de fotos ya configurado.)

---

# Actualización 6 — cifras de cartera por cliente (rama feat/cliente-saldos) (2026-07-20)

**Por qué:** el listado de gestión ya filtra la cartera (Pendientes/Vencidas/Multas/Saldos) pero no mostraba el
**monto**; `ClienteResponse` no lo traía. Ya está.

## Contrato nuevo (tras regenerar el `:api-client`)
- **`ClienteResponse`** del **listado** (`GET /clientes`) gana **`saldoPendiente`** (cuánto debe todavía) y
  **`multaTotal`** (multa acumulada). En crear/editar/archivar/activar van en 0 (no se calculan ahí).

## A cablear en la app (al mergear + regenerar)
- **[ClienteAdapter.kt](app/src/main/java/com/costumi/app/ui/gestion/clientes/ClienteAdapter.kt):** mostrar el saldo/multa
  junto a cada cliente cuando `saldoPendiente > 0` o `multaTotal > 0` (p. ej. un chip/línea "Debe $X · Multa $Y").
  Formatear con `comoPrecio()`. Nada rompe por este cambio (solo se agregan campos al modelo).

---

# Actualización 7 — nombre del cliente en venta/renta + foto al crear + sin depósito (2026-07-20)

## Contrato nuevo (rama feat/venta-renta-cliente-nombre → tras regenerar el :api-client)
- **`RentaResponse`** y **`VentaResponse`** ganan **`clienteNombre`** (nombre de la ficha del cliente, resuelto en
  el backend). A cablear: mostrarlo en la tarjeta de `RentaAdapter`/`VentaAdapter` (antes de abrir el detalle), p. ej.
  bajo el total: "Cliente: <nombre>". Ya se muestra el `codigoRetiro`/desglose al tocar.

## Ya hecho en la app (no requiere merge)
- **Foto al CREAR** prenda y disfraz (además de editar): se elige la foto, se ve la vista previa y se sube al guardar
  (best-effort; si S3 no está, la entidad queda guardada y avisa por Toast).
- **Depósito eliminado** de los forms de prenda y de renta (ya no se usa; se paga directo, daño/pérdida vía
  valorDano/valorReposicion). El campo "Depósito sugerido"/"Depósito" se quitó; se envía `deposito = null`.

---

# Actualización 8 — crashes, fotos por todos lados, email inmutable, desglose de deuda y reportes por sucursal (2026-07-20)

## Crashes arreglados (bugs de layout preexistentes, verificados en emulador)
- **StateView**: su `@id/progreso` (Circular) chocaba con el `progreso` (Linear) de varios fragments →
  `ClassCastException` en `bind()`. Renombrado a `@id/progresoEstado` (`view_estado.xml` + `StateView.kt`).
  Arregla asignar-disfraz (dueño), detalle de disfraz y pago (cliente).
- **TabItem con android:id**: `fragment_tienda.xml` ponía id en los `<TabItem>`; `TabLayout` los quita del árbol →
  `NPE: Missing required view tabDisfraces`. Se quitaron los ids (el fragment usa `selectedTabPosition`).

## Front cableado (tras PR #1 y #2 mergeados + `:api-client` regenerado)
- **Nombre de cliente + foto** en tarjetas de renta/venta (`RentaAdapter`/`VentaAdapter`, `item_renta/venta.xml`).
- **Email no editable** al editar cliente (`ClienteFichaFragment` deshabilita el campo; `EditarClienteRequest` ya no lo lleva).
- **Cobro mixto** con explicación + resumen (cubre/falta/vuelto) en `dialog_cobro_mixto.xml` + `PagoConceptoFragment`.
- **Fotos en devolución** (`DevolucionesFragment`: `PiezaResponse` trae nombre+foto).
- **Artículos con foto en pagos/cobros** (`PagoConceptoFragment` + `PagoRepository.articulos()` vía GET renta/venta por id).
- **Fotos en reembolsos** (`ReembolsosFragment`: tocar solicitud → desglose de artículos con foto).
- **Desglose de deuda del cliente** (`ClientesFragment.mostrarEstadoCuenta` + `estadoCuenta()` → `GET /clientes/{id}/estado-cuenta`):
  menú "Estado de cuenta" y tocar la línea de cartera; muestra por renta importe/multa/pagado/saldo + totales.
- **Reportes por sucursal**: `ReporteRepository` pasa `sucursalId` a ingresos/ganancia/tablero/ventas-por-etiqueta;
  `ReportesViewModel` recarga ventas-por-etiqueta al cambiar de sucursal.

## Pendiente de infra (credenciales del usuario, no código)
- MercadoPago (pago en línea/tarjeta), WhatsApp/SMTP/FCM, y proyecto Firebase (push al cliente). Efectivo y cobro mixto
  funcionan sin MercadoPago (registran el cobro en mostrador).
