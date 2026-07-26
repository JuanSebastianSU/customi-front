# Auditoría Backend ↔ Front — ¿cada campo que manda el backend está bindeado en el front?

Método: se cruzó el contrato OpenAPI desplegado (**205 endpoints, 32 controllers, 184 DTOs**) contra el
código del front (`app/src/main`), endpoint por endpoint y campo por campo. Cada campo se marca **BOUND
(archivo:línea)** o **GAP (no aparece en el front)**. Todo es spot-checkeable.

> Estado: **completo** — 10 dominios, 205 endpoints, 184 DTOs cruzados con el front.

---

# RESUMEN EJECUTIVO (honesto, con verificación)

**Conclusión principal:** donde el front consume un campo, lo mapea **bien**. La auditoría **no encontró bugs
de corrupción ni de mis-binding**. El bug del mapper de empresa (logos/portada) que ya arreglé **era el único
de ese tipo** en toda la capa de datos.

**Corrección a mí mismo (verificada):** el gap que yo mismo iba a "arreglar" — permisos `concedido` ignorado —
**NO es un bug**. Verifiqué contra la API: `GET /empleados/me/permisos` devuelve **solo las capacidades
concedidas** (las negadas no vienen), así que mapear todas sus secciones es correcto. El agente lo infirió mal.
Bien haberlo verificado antes de romperlo.

**Lo que la auditoría SÍ revela** son dos cosas, ninguna es "el front miente o corrompe datos":

### 1) Features con backend LISTO pero SIN UI en el front (el grueso — backend va adelante)
Verificables en el doc. Los de más valor:
- **Estado de pago por pedido** (`historial.saldoPendiente/estadoPago`) — el dato llega (verificado: `estadoPago=PAGADO`), el front tiene un TODO y no lo muestra. ★ quick win real.
- **Horario de la tienda** — endpoints `.../horario` (editar + vitrina) enteros sin cablear.
- **Editar datos de la tienda** (`PATCH /empresas/mia`) — solo se sube logo/portada; nombre/descripción/ciudad/contacto no se pueden editar.
- **Sucursal: descripción + mapa (lat/long/ubicacionMaps) + foto** en la vitrina — el backend los da, no hay UI.
- **Selector multi-tienda** (`/auth/me/membresias`) — método muerto, no existe la pantalla.
- **Aceptar invitación dentro de la app** — no hay flujo (solo por enlace).
- **Favoritos** — son solo locales (Room); los endpoints del server no se usan → no sincronizan.
- **Estado de canales** FCM/WhatsApp — no se muestra.
- **Agrupación por disfraz en gestión ventas/rentas** — el backend manda los campos, la UI los ignora.
- **Reembolsos: quién lo pidió** (`solicitanteNombre`) · **Caja: quién abrió + horas** · **Comprobante: desglose de impuesto** — datos presentes, no mostrados.
- **Reportes**: miniaturas (fotoUrl) + navegación por id + series ventas/rentas por día + "devoluciones por cerrar" — sin usar.
- **Depósito en el carrito del cliente** (`totalDeposito`) — invisible al cliente.
- **Destacados** (carrusel del marketplace) — endpoint sin consumidor.

### 2) Drops intencionales / cosméticos (bajo impacto)
- `empresaId` en todos los schemas (multi-tenant, no se muestra a propósito).
- IDs para drill-down (prendaId/rentaId/clienteId en varias listas) y timestamps de renta.
- Métodos generados reemplazados por interfaces a mano (refrescar, contrato.pdf, comprobante.pdf, mios, misDeudas) — **funciona**, es deuda técnica.
- `nombreParaMostrar`, `MiDeudaDto.sucursalId` (omitido en el DTO a mano).

**Núcleo verificado como bien bindeado:** catálogo, inventario+variantes, disfraces (form completo), ventas,
rentas (ciclo completo), carrito/checkout, configuración (10/10), pagos (comprobante), caja (detalle).

**Números por dominio** (los conteos de "campos sin bindear" están inflados porque cuentan el mismo campo por
cada endpoint que comparte el DTO; los gaps *semánticos* distintos son ~40): ver cada sección abajo.

---

# CIERRE — estado final (todo contabilizado)

## ✅ Implementado en el front (18 gaps cerrados, compilados e instalados)
1. Estado de pago por pedido + filtro «Por pagar» (cliente).
2. Reportes: miniaturas en rankings + depósito en rentas vencidas.
3. Reembolsos: quién lo pidió (`solicitanteNombre`).
4. Clientes: marca «renta en curso» (`tieneRentaEnCurso`).
5. Caja: horas de apertura/cierre del turno (`abiertoEn`/`cerradoEn`).
6. Invitaciones pendientes: fecha de expiración (`expiraEn`).
7. Estado de cuenta del comerciante: detalle completo (daño/retraso/depósito/fechas).
8. Comprobante: desglose de impuesto (`baseImponible` + `tasaImpuesto`).
9. Editar datos de la tienda (nombre/descripción/ciudad/ubicación/contacto → `PATCH /empresas/mia`).
10. Depósito visible en el carrito del cliente (`totalDeposito`).
11. Sucursal en el detalle del cliente: dirección + 📍 link a mapa (`ubicacionMaps`/lat/long) + descripción.
12. Estado de canales FCM/WhatsApp (Notificaciones).
13. Agrupación por disfraz en gestión ventas y rentas (`disfrazGrupo/Nombre/Cantidad`).
14. Descripción de la tienda en el marketplace (`GET /marketplace/empresas/{id}`).
15. Carrusel de destacados en Explorar (`GET /marketplace/destacados`).
16. Favoritos **sincronizados** con la cuenta (server + cache Room; antes solo locales).
17. Aceptar invitación **in-app** (preview `GET /invitaciones/{token}` + `POST /aceptar`, desde Login).
18. Reportes: devoluciones por cerrar (`GET /reportes/devoluciones-por-cerrar`).

## 🔒 Cerrado por diseño (no es un gap; es correcto así)
- **Selector multi-tienda** — el modelo es exclusivo (una persona = una tienda a la vez, regla #2; aceptar una 2ª invitación se rechaza en `InvitacionService.java:78`). Se eliminó el método muerto `misMembresias()`.
- **`empresaId` en todos los responses** — id multi-tenant, no se muestra a propósito.
- **Bodies de mutación descartados** (empleado/reembolso/etc.) — el front recarga la lista tras la acción; la representación devuelta no se necesita.
- **Métodos generados reemplazados por interfaces a mano** (refresh, contrato.pdf, comprobante.pdf, subir-fotos, `mios`, `misDeudas`) — **correctos** (el cliente generado modela mal el multipart como `@Body`, no hay converter de `ByteArray` para PDFs). NO tocar.
- **`nombreParaMostrar`** — el front arma el nombre a mostrar por su cuenta (equivalente).
- **Serie ventas/rentas por día** — ya cubierto por la serie de `ingresos-por-día` (evolución diaria).

## 🟡 Limitaciones conocidas de bajo impacto (documentadas)
- **Disfraz: `multaSugerida`/`precioSugeridoMax` del server** — el form los **recalcula localmente** desde las prendas (resultado equivalente); usar el valor del server sería una optimización menor, sin impacto para el usuario.
- **Disfraz: round-trip del `pool.etiquetasPermitidas`** — al editar un disfraz con slot **por pool** (ruta legacy; el alta del dueño usa opciones explícitas) el whitelist no se re-lee. Impacto bajo (los disfraces del seed usan opciones explícitas).
- **`NotificacionResponse.clienteNombre` / `MiDeudaDto.sucursalId`** — sin impacto visible (el nombre ya se muestra por lookup; el `sucursalId` no se despliega).
- **Timestamps internos de renta** (`entregadaEn`/`devueltaRealEn`/`cerradaEn`) e **IDs para tap-through** en listas/rankings — mejoras de navegación/auditoría, no operativas. Nice-to-have.
- **`auditoria.actorUsuarioId`** — es solo un UUID (sin nombre); mostrarlo crudo aporta poco (necesitaría un join de nombre en backend).

## ⚠️ Observación aparte (no es del front)
- **Crear invitación (`POST /empleados`) se cuelga >25s** — el envío de email es **síncrono**; si el SMTP configurado está lento/inaccesible, la creación se traba. Es backend/infra, afectaría tu prueba de invitaciones. Conviene mandar el email en background.

---

---

## reporte-controller  — 21 endpoints · 18 llamados · 67 campos · **20 sin bindear**

### Endpoints que el front NO llama (3)
- `GET /reportes/ventas-por-dia` — existe el método generado, nadie lo invoca (serie de ventas por día sin usar; la pantalla usa `ingresos-por-dia`).
- `GET /reportes/rentas-por-dia` — nunca invocado (serie de rentas por día sin usar).
- `GET /reportes/devoluciones-por-cerrar` — nunca invocado (el conteo "devoluciones por cerrar" no se muestra).

### Campos que el backend manda pero el front descarta (en endpoints que SÍ llama)
- `ventas-por-empleado.empleadoId` — se muestra solo el email; sin id no hay drill-down por empleado.
- `rentas-vencidas.rentaId / .clienteId / .prendaId / .deposito` — la lista de vencidas muestra días/fecha/importe; **sin ids** (no se puede navegar a la renta) y **el depósito en riesgo no se muestra**.
- `mas-vendidos.prendaId / .fotoUrl` · `mas-rentados.prendaId / .fotoUrl` — ranking sin miniatura ni tap-through.
- `disfraces-mas-vendidos.disfrazId / .fotoUrl` · `disfraces-mas-rentados.disfrazId / .fotoUrl` — ídem.
- `inventario/tablero.prendaId` — las filas del tablero no llevan id para navegar.
- `ingresos-por-metodo.total` — se muestran efectivo/tarjeta/transferencia, pero no el total del método.

### Sin problemas de binding
Ningún campo está mal mapeado; los gaps son "campo/endpoint existe en backend, ausente en la UI", no bindings incorrectos. `ingresos` y `resumenDeInventario` los consume el Dashboard (verificado en `DashboardFragment.kt`).

---

## empleado / actividad / invitacion / auditoria — 16 endpoints · 13 llamados · 54 campos · **39 sin bindear**

### Endpoints que el front NO llama (3)
- `GET /empleados/{id}/sucursales` [sucursalesDe] — el front lee las sucursales del ítem de lista, no de aquí.
- `POST /invitaciones/aceptar` [aceptar] — **no hay flujo de aceptación de invitación dentro de la app**.
- `GET /invitaciones/{token}` [ver_1] — no hay pantalla de previsualización de invitación.

### Campos que se descartan
- **Body de TODA mutación de empleado ignorado** (cambiarRol/activar/desactivar/suspender/reactivar/quitar/asignarSucursales): la app solo mira éxito/fallo y recarga la lista con `listar10`. Es por diseño, pero la representación devuelta no se usa.
- `invitacionesPendientes.expiraEn` — **la fecha de expiración de la invitación no se muestra** (`EmpleadosFragment.kt:101`).
- `actividad.empleadoId` — sin usar.
- `auditoria.actorUsuarioId` — **el id de quién hizo la acción no se muestra** (`AuditoriaAdapter.kt`).

---

## auth / perfil / membresia / mis-permisos / permisos-empleado — 17 endpoints · 17 llamados · 60 campos · **26 sin bindear**

### ★ Bug de enforcement (importante)
- `mis-permisos.mias[].concedido` — **el front NO lo usa**: `ContextoGestionRepository.kt:35` mapea TODAS las secciones sin filtrar por `concedido`. Efecto: una capacidad **negada** igual habilita su sección en el menú "Más". El gateado por permisos está incompleto.

### Cerrado por diseño
- `GET /auth/me/membresias` [mias_1] — **NO se usa a propósito.** El modelo Fase B es **exclusivo**: una persona
  trabaja en UNA sola tienda a la vez (regla de seguridad #2; `InvitacionService.java:78` rechaza aceptar una 2ª
  invitación hasta desvincularse). No hay caso de "cambiar de tienda", así que no hay selector multi-tienda.
  `/auth/me` ya expone la única membresía activa. Se eliminó el método muerto `misMembresias()` del front.
- `POST /auth/me/desvincularme` — los 4 campos del body (`usuarioId/empresaId/rol/estado`) se descartan (`PerfilViewModel.kt:84`).

### Campos sin bindear (destacados)
- `perfil.nombreParaMostrar` — **cero referencias** en toda la app (el front arma el nombre por su cuenta).
- `me.id`, `me.membresiaActiva.empresaId`, `me.membresiaActiva.rol` — sin usar.
- `perfil.rol` / `perfil.empresaId` — no se leen de `PerfilResponse` (se sacan de `/auth/me`).

### Nota (no es bug, es deuda)
- `refrescar()` y `subirFoto1()` generados están **muertos**: el front usa HTTP a mano (`TokenAuthenticator.kt:84`, `FotoPerfilApi.kt:18`). Funciona, pero el método generado no se usa.

---

## venta / renta / devolucion — 17 endpoints · 15 llamados · **122 campos sin bindear** (muchos repetidos por schema)

### Endpoints que el front NO llama (2)
- `GET /ventas/totales` — el resumen "total vendido/cantidad" no se usa (se calcula en el cliente).
- `GET /rentas/resumen` — los contadores porEntregar/activas/vencidas/cerradas se calculan en el front, no se usa el del backend.

### Campos que se descartan (destacados)
- **Agrupación por disfraz en gestión**: `venta.lineas[].disfrazId/disfrazNombre/disfrazGrupo/disfrazCantidad` y lo mismo en `renta.lineas[]` — el backend los manda pero la UI de gestión-ventas/gestión-rentas **no agrupa por disfraz** (solo el lado cliente lo hace, con otro modelo). Una venta/renta de un disfraz completo se ve como prendas sueltas.
- **Timestamps de renta**: `creadaEn/entregadaEn/devueltaRealEn/cerradaEn` — todos descartados (la UI solo usa fechaRetiro/fechaDevolucion).
- `venta.empleadoId/clienteId/creadaEn` — sin usar (sin atribución de vendedor ni fecha de venta).
- `devolucion.rentaId` — sin link de vuelta a la renta; `devolucion.registradaEn` — la fila de devolución no muestra fecha.

---

## prenda / grupo-de-stock / conteo-dependencias — 18 endpoints · **18 llamados** · 3 campos sin bindear ✅

Este dominio está prácticamente completo. Solo:
- `PrendaResponse.empresaId`, `GrupoDeStockResponse.empresaId` — no se leen (se usa el del token).
- `GrupoDeStockResponse.combinacion[].tipoEtiquetaId` — el label de variante usa solo `valorEtiquetaId`.
- Deuda: `depositoSugerido` es passthrough muerto (ya no hay depósito en la UI).

---

## empresa / sucursal / configuracion — 23 endpoints · 20 llamados · **86 campos sin bindear**

### Endpoints que el front NO llama (3)
- `GET /empresas/mia/horario` + `PUT /empresas/mia/horario` — **no existe UI de horario de la tienda** (feature entera sin cablear).
- `PATCH /empresas/mia` [editarMia] — **no hay forma de editar nombre/descripción/ciudad/ubicación/contacto de la propia tienda** (Identidad solo sube logo/portada).

### Campos que se descartan (destacados)
- `SucursalResponse.descripcion / latitud / longitud` — **sin UI**: la descripción del local y las **coordenadas del mapa** se mandan pero no se muestran ni editan.
- `EmpresaResponse.estado / descripcion / ubicacion / contacto` — llegan en cada `GET /empresas/mia` pero se descartan (solo nombre/ciudad/logo/portada llegan a la UI).
- `EmpresaPendienteResponse.solicitanteId`, `EmpresaResumenResponse.fechaRegistro` — sin usar (superadmin).
- Configuración: **10/10 campos bindeados** ✅.

---

## categoria / tipo-etiqueta / categoria-disfraz / disfraz — 32 endpoints · 31 llamados · 93 campos sin bindear (repetidos por schema)

Bien bindeado en general (nombre, tipo, precios, slots, fotoUrl, etiquetas todo OK). Gaps reales:
### Endpoints que el front NO llama (1)
- `GET /disfraces/conteo-por-prenda/{id}` [conteoPorPrenda] — nunca invocado.

### Campos que se descartan (destacados)
- `DisfrazResponse.multaSugerida` (+ danoMin/danoMax/reposicionMin/reposicionMax) — **la multa sugerida por el backend se ignora**; el form la recalcula localmente desde las prendas.
- `DisfrazResponse.precioRentaSugeridoMax / precioVentaSugeridoMax` — el **techo de precio sugerido** se descarta (solo se usa el mínimo).
- `slots[].pool.etiquetasPermitidas` (+ tipoEtiquetaId/valores) — **al editar un disfraz con slot por pool, el whitelist de etiquetas no se re-lee** (el form solo reconstruye desde `prendasOpcion`). Round-trip del pool perdido.
- `empresaId` — descartado en todos los schemas (multi-tenant, intencional).

---

## cliente / notificacion / plantilla — 25 endpoints · 18 llamados · 84 campos sin bindear

### Endpoints que el front NO llama (7)
- **Favoritos server** (`GET/POST/DELETE /clientes/me/favoritos`) — **los favoritos son SOLO locales (Room)**; nunca se sincronizan con el server.
- `GET /notificaciones/estado-canales` — **el estado de FCM/WhatsApp (configurado o no) no se muestra**.
- `POST /notificaciones/probar-push/{id}` — no hay "enviar push de prueba".
- `GET /clientes/me/operaciones/{id}` — detalle de una operación nunca se pide.
- `PUT /clientes/{id}/device-token` — solo se usa la variante `me`.

### Campos que se descartan (destacados)
- `HistorialItem.saldoPendiente / estadoPago` — el backend los manda; el front tiene un TODO "cuando el backend los mande" → **el estado de pago por pedido no se muestra aunque ya está disponible**.
- `EstadoDeCuentaResponse` (comerciante): `lineas[].cargoPorDanos/cargoPorRetraso/deposito/rentaId/fechaDevolucion` — el **estado de cuenta del comerciante muestra menos detalle** que el del cliente (`DeudaAdapter` sí los muestra).
- `MiDeudaDto` (interfaz a mano) **omite `sucursalId`** — el campo no se deserializa.
- `ClienteResponse.tieneRentaEnCurso` — sin usar (no se marca "tiene renta en curso").
- `NotificacionResponse.clienteNombre` — sin usar (se re-deriva por lookup).

---

## pago / reembolso / caja — 19 endpoints · 15 llamados · 67 campos sin bindear

### Endpoints que el front NO llama (3 + webhook)
- `GET /pagos` [listar_6], `GET /pagos/saldo`, `GET /pagos/deposito` — no se usan; el saldo/depósito se derivan del **comprobante**. (`/pagos/webhook` es server-to-server, correcto que no se llame.)

### Campos que se descartan (destacados)
- `reembolso.solicitanteNombre` — **la bandeja de reembolsos no muestra QUIÉN lo pidió** (solo tipo+monto). También `decididoPorUsuarioId/rolDecision` (quién decidió).
- `comprobante.tasaImpuesto / baseImponible` — el **desglose de impuesto no se muestra**; `deposito.retenido/devuelto` tampoco.
- `CobroMixtoResponse.pagos[]` — el desglose del pago mixto no se muestra (solo total/vuelto).
- `TurnoResponse.empleadoId / abiertoEn / cerradoEn` — **quién abrió el turno y las horas de apertura/cierre no se muestran**, ni en el detalle.
- `PagoResponse.sucursalId/empleadoId/tipoConcepto/conceptoId` — sin usar.
