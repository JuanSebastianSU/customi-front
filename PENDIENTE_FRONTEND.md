# Costumi — Pendiente del FRONTEND (app Android)

> Checklist de lo que el dueño (usuario) pidió y **falta en el front**. El backend de toda la
> visión (inventario / disfraz / carrito) ya está completo en `main` + Railway. Actualizado 2026-07-21.
>
> Regla que impuso el usuario: **NO cambiar cómo se crea un stock** (el flujo actual está bien).
> No inventar requerimientos; si algo no está claro, preguntar.

## ✅ OP-7 y OP-8 — el cliente no podia llegar a su carrito ni a sus multas (2026-07-22)

Los dos eran **huecos del backend**, no solo de la app: el dato no se podia pedir.

- ✅ **OP-7 — no habia forma de abrir el carrito.** Solo se llegaba justo despues de agregar un
  articulo, asi que al cambiar de tienda habia que agregar algo otra vez para reencontrarlo. Y el
  backend tampoco podia listarlos: `GET /carritos` exige saber de antemano empresa, sucursal y tipo.
  - Backend: `GET /carritos/mios` (PR #153).
  - App: pestaña **Carrito** en la barra del cliente (`MisCarritosFragment`), entre Explorar y Mis
    pedidos. Lista "Renta · Centro · 3 articulos" y al tocar abre ese carrito. Se refresca en `onResume`.
  - Los **carritos vacios no aparecen**: con solo entrar a una tienda se crea uno, y listarlos llenaria
    la pantalla de tiendas apenas visitadas.
  - Muestra **unidades, no importe**: el precio no esta guardado, se calcula al abrir el carrito.
    Calcularlo tambien en la consulta pondria la formula en dos lugares.
- ✅ **OP-8 — el cliente no podia ver sus multas.** El estado de cuenta ya existia y es completo, pero
  solo como `GET /clientes/{id}/estado-cuenta`, que es **de la tienda** (exige `empresa_id` y rol de
  personal).
  - Backend: `GET /clientes/me/deudas` (PR #154), con el desglose y el nombre de cada tienda.
  - App: pantalla **Mis multas** (`MisDeudasFragment`), con el total a pagar arriba y por renta el
    desglose *"Danos $150 - Deposito $50 = Multa $100"*. Una multa ya pagada se sigue viendo, pero sin
    la cifra en rojo.
  - Se entra desde **Perfil**, no como quinta pestaña: la barra ya tiene cuatro y las multas son
    informacion de cuenta, no navegacion primaria.

## ✅ OP-1..OP-5 — problemas de operacion reportados usando la app (2026-07-22)

Reportados por el dueño probando la tienda de verdad. **Los cinco cerrados.** Cuatro eran del front y
uno del backend.

- ✅ **OP-1 — el dueño ve el nombre de su tienda.** Faltaba en el **backend**: las unicas lecturas de
  empresa eran de SUPERADMIN y no existia `GET /empresas/{id}`. Se agrego `GET /api/v1/empresas/mia`
  (PR #152, ya en Railway). En la app: `MiEmpresaApi` (a mano, porque el `:api-client` se genera del
  contrato ya desplegado), `MiEmpresaRepository` con cache en memoria que **se limpia al cerrar sesion**
  (si no, el proximo dueño veria el nombre anterior), y el nombre encabeza el **Panel**.
  Verificado en vivo: 200 "Disfraces Demo 635935" para el dueño, **403** para un CLIENTE, 401 sin token.
- ✅ **OP-2 — no se podian aprobar reembolsos.** El backend exige que el item este devuelto
  (`ItemNoDevuelto`, 409) y **desde la bandeja no habia forma de registrar la devolucion**: habia que
  salir a Ventas/Rentas a buscar la operacion, sin que nada lo indicara, asi que en la practica solo se
  podia rechazar. Nueva accion **"Registrar devolucion"** en el menu de cada solicitud pendiente, con
  confirmacion (reingresa stock). Devuelve **todo lo pendiente**; para devolver solo algunas unidades
  sigue estando el dialogo de Ventas.
  - Confirmado con datos reales: la solicitud aprobada tenia su venta `DEVUELTA`; la rechazada estaba
    `CONFIRMADA` con 0 devueltas.
- ✅ **OP-3 — no se podia elegir cantidad al rentar.** Era **solo del front**: el backend siempre acepto
  `cantidad` en renta. En prenda estaba oculto a proposito; **en disfraz no existia** y viajaba fijo en 1
  (`PedidoRepository`). Ahora el contador esta en los dos y en ambos modos.
- ✅ **OP-4 — la tienda no veia el codigo de retiro.** El backend **ya lo enviaba** en `VentaResponse` y
  `RentaResponse`; la app solo lo mostraba dentro del `DesgloseSheet`. Ahora sale en la **lista** de
  ventas, en la de rentas y en **Pagos/cobros**, que es donde se coteja al entregar.
- ✅ **OP-5 — "las fechas se desmarcan y me deja rentar igual".** **Nunca se rento sin fechas**: viajan
  con cada linea al agregarla y el backend las usa para cobrar. El problema es que **el carrito no las
  mostraba**, asi que parecian perdidas y se rentaba a ciegas. Ahora cada linea muestra su periodo.
  Ademas las fechas elegidas pasaron del Fragment al **ViewModel**, porque la vista se recrea al volver
  atras y ahi se perdian (prenda y disfraz).

## ⛔ PENDIENTE DEL FRONT — auditoria del 2026-07-22

Barrido cruzando el codigo contra el contrato: **158 de 160 operaciones del backend ya tienen pantalla**.
Estos son los 6 huecos reales (los backend correspondientes estan en `PROGRESS.md` del backend como
BACK-1/2/3).

- ✅ **FRONT-1 — HECHO (2026-07-22).**
  - **Mis pedidos**: las piezas de un mismo disfraz se colapsan en **un solo articulo con el nombre del
    disfraz** (`LineaPedidoAdapter.agrupar`). Se agrupa por `disfrazGrupo` y **no** por `disfrazId`, porque
    el mismo disfraz puede ir dos veces con piezas distintas y son dos articulos.
  - **Reportes**: dos secciones nuevas, **"Disfraces mas vendidos"** y **"Disfraces mas rentados"**, que
    cuentan DISFRACES y no piezas.
  - Necesito un backend extra: **PR #145** (descuido de BACK-1 — "Mis pedidos" usa el read-model
    `/clientes/me/historial`, que seguia devolviendo solo prendas).
  - Verificado E2E: Reportes muestra *"Traje_Pirata_Opciones (2 disfraces) — $300.000"*, y en Mis pedidos
    los pedidos NUEVOS dicen *"Traje_Pirat… x2 · 1 pieza"* mientras los viejos siguen diciendo "Capa Real".
  - ⚠️ Los pedidos **anteriores a la migracion V65 no se pueden reconstruir**: nunca se guardo de que
    disfraz salieron. Solo aplica de aqui en adelante.
  - Pendiente menor: **Devoluciones** sigue mostrando piezas sueltas sin agrupar por disfraz.
- ✅ **FRONT-2 — HECHO (2026-07-22).** Buscador en las 10 listas que faltaban: Inventario, Disfraces,
  Rentas, Ventas, Pagos, Devoluciones, Reembolsos, Empleados, Notificaciones y Auditoria.
  - Piezas nuevas reutilizables: **`res/layout/barra_busqueda.xml`** (se incluye con `<include>`) y la
    extension **`EditText.alBuscar { }`**, que espera 350 ms a que el usuario deje de escribir — sin eso,
    tipear "camisa" dispararia seis consultas y la lista parpadearia.
  - El hint dice QUE se busca en cada pantalla (por nombre, por codigo de retiro, por motivo...).
  - Las paginadas (Inventario, Rentas, Ventas, Pagos) reemiten el Pager con `flatMapLatest`, asi que al
    escribir se vuelve a pedir la primera pagina; las simples recargan.
  - Tambien se adapto la app al **cambio de contrato de BACK-2**: seis endpoints pasaron de arreglo a
    `{contenido, total, ...}`. Se agrego `RespuestaRed.mapear { }` para quedarse con el `contenido` sin
    repetir el `when` en cada repositorio.
  - `:api-client` regenerado. Gotchas reaplicados + sufijos corridos (`actualizar1`→`actualizar2` en
    configuracion, `actualizar`→`actualizar1` en plantillas) y **argumentos con nombre** en las llamadas
    paginadas: el nuevo `buscar` quedo primero y los posicionales se corrieron en silencio.
  - Verificado E2E: Inventario "capa" → solo Capa Real; Auditoria "stock" → solo los dos ajustes de stock;
    Ventas con el codigo real `V-743252E5` → esa sola venta.
- ✅ **FRONT-3 — HECHO (2026-07-22).** Caja de busqueda en Explorar.
  - **No se reutilizo `refrescarEmpresas`**: ese metodo **reemplaza la cache de Room**, asi que buscar por
    ahi habria dejado al usuario con solo las coincidencias guardadas y sin nada que ver sin conexion.
    Se agrego `buscarEmpresas(texto)`, que consulta al servidor y devuelve los resultados **sin tocar la
    cache**; al limpiar el texto vuelve a mostrarse la lista cacheada.
  - Se busca en el servidor (no filtrando local) porque el punto es encontrar tiendas que el usuario
    todavia no tiene guardadas.
  - Se descartan respuestas viejas si el usuario siguio escribiendo (evita que un resultado lento pise a
    uno mas nuevo).
  - Verificado E2E: "juan" → solo Juan Tienda; al limpiar vuelven las 4 tiendas desde la cache.
- 🟡 **FRONT-4 — app LISTA; falta la credencial en Railway.**
  - **Hecho y verificado**: plugin `google-services` + Firebase BOM (solo messaging), `google-services.json`
    en `app/`, `ServicioDeMensajeria` (atiende `onNewToken`, que es lo que rota y rompe el push en
    silencio), `PushRepository` y registro **al iniciar sesion**. En el log del emulador:
    *"Dispositivo registrado para push"*, y el backend responde 204.
  - **Hueco que encontre y corregi**: el permiso `POST_NOTIFICATIONS` estaba declarado en el manifiesto
    pero **no se pedia en tiempo de ejecucion**. Sin eso Android deja la app en `importance=NONE` y las
    push no se muestran aunque lleguen. Ahora se pide al entrar al shell del cliente.
  - **Backend**: PR #146 (endpoint `/clientes/me/device-token` + `CanalFcm` migrado a **FCM HTTP v1**; la
    API legacy que usaba fue apagada por Google).
  - ⛔ **Falta**: cargar **`COSTUMI_FCM_CREDENTIALS`** en Railway con el JSON de la cuenta de servicio.
    Sin esa variable `CanalFcm` no envia y el router **cae al log**, asi que la notificacion queda
    `estado: ENVIADA` **aunque no haya salido** — `ENVIADA` no prueba que llego.
  - Probado: con permiso concedido y la app en segundo plano, la push **no aparece** en la barra. Eso
    apunta a la credencial faltante, que es lo unico que queda por configurar.
- ✅ **FRONT-5 — HECHO (2026-07-22).** El Perfil deja editar **nombre y telefono** y **cambiar la
  contrasena** (dialogo con actual + nueva + repetir, con ojito para ver lo escrito).
  - Piezas: `PerfilRepository` + `providePerfilApi` en Hilt + `dialog_cambiar_contrasena.xml`. La pantalla
    paso a `ScrollView` porque el contenido ya no entra fijo.
  - Los campos **no se pisan mientras se escribe**: solo se rellenan si estan vacios.
  - Se valida en el cliente que la nueva coincida y tenga 8+; el backend igual exige la actual.
  - Verificado E2E: nombre y telefono persisten (`GET /api/v1/perfil` los devuelve); con la contrasena
    actual equivocada sale **"Credenciales invalidas"** y no cambia nada; con la correcta cambia de verdad
    (login con la vieja da 401 y con la nueva 200). Se restauro la clave del cliente de prueba.
- ✅ **FRONT-6 — HECHO (2026-07-22).** ⚠️ **Mi hallazgo original estaba mal**: la app **si** lee las
  sucursales asignadas — vienen dentro de `GET /api/v1/empleados` (`EmpleadoDetalleResponse.sucursales`) y
  el dialogo de asignar ya las premarcaba. El endpoint suelto `/empleados/{id}/sucursales` no se usa
  porque **no hace falta**, no por un olvido.
  - El hueco real, mas chico: la fila decia **"1 sucursal(es)"**, un conteo sin decir cuales. Ahora muestra
    los **nombres** ("Desactivado · Casa Matriz"); con mas de 3 vuelve al conteo para que la fila no quede
    ilegible, y sin ninguna dice "Sin sucursal asignada".
  - Verificado E2E con el empleado de prueba.

**Ya verificado como HECHO** (no perseguir): subir foto al disfraz y exportar reportes a PDF/CSV — usan APIs
escritas a mano (`FotoDisfrazApi`, `ReporteExportApi`), por eso no aparecen al buscar en el cliente generado.

---

## Estado
- ✅ Hecho y verificado
- ⛔ Pendiente

---

## 1. Inventario por categoría (visión de "stocks") — ✅ HECHO (2026-07-21, verificado E2E)
- ✅ **Inventario navegable POR CATEGORÍA con filtros por etiqueta y stock.** En la pantalla Inventario se
  agregó una barra de filtros: chips de categoría ("Todas" + cada categoría) + botón **"Filtros"** que abre
  un diálogo de valores de etiqueta (Color/Talla/Material…). Al elegir categoría o filtros, la lista muestra
  los **stocks con su stock disponible** (chip "N disp." / "Sin stock") y sus valores de etiqueta ("Rojo · M");
  tocar una fila abre sus Grupos de stock. Sin filtros = la lista de gestión de siempre (crear/editar/stock/
  archivar intactos). AND entre dimensiones (Camisa + Rojo → 1), OR dentro.
  - Endpoint: `GET /api/v1/prendas/catalogo?categoriaId=&etiqueta=tipo:valor`.
  - Archivos: `InventarioRepository.catalogo/tiposConValores`, `InventarioViewModel` (filtros+catálogo),
    `CatalogoStockAdapter` + `item_stock_catalogo.xml` + `chip_filtro.xml`, `InventarioFragment` (barra +
    diálogo + toggle de lista), `fragment_inventario.xml` (barra de filtros).
  - Recordatorio: misma camisa en distinta talla/color = **stocks distintos** (así ya funciona; no se tocó el alta).
- ✅ (aclarado) NO se tocó el flujo de creación de un stock.

## 2. Creación de disfraz (`DisfrazFormFragment`) — ✅ HECHO (2026-07-21, verificado E2E)
- ✅ Elegir elementos (prendas) por parte personalizable con picker (con stock "N disp.").
- ✅ Cuadrito de **precio sugerido en RANGO (mín–máx)** en vivo, sin reemplazar el precio final del dueño.
- ✅ Parte **FIJA** sin opciones (solo prenda fija).
- ✅ **Filtros por ETIQUETA** (color/talla) **dentro del picker**: botón "Filtros" en el picker → diálogo de
  valores → recarga el catálogo filtrado (usa `/prendas/catalogo` con `etiqueta=tipo:valor`).
- ✅ **Multa sugerida por tipo** (daño/reposición, rango) en el cuadrito en vivo — se calcula de
  `valorDano`/`valorReposicion` de los elementos; se oculta si son 0 (misma lógica que el rango de precio).
- ✅ **Enviar la CATEGORÍA del disfraz** (de la taxonomía de disfraz) y el **precio de venta general** desde el
  form (`CrearDisfrazRequest.categoriaId`/`precioVentaGeneral`).

## 3. Disfraces por categoría — ✅ HECHO (2026-07-21, verificado E2E)
- ✅ **Chips de categoría** en la lista de disfraces (Todas + cada categoría de DISFRAZ); filtran vía
  `GET /api/v1/disfraces?categoriaId=` (client-side sobre la lista). Recarga al volver de "Categorias de disfraz".

## 3.b Categorías de disfraz = taxonomía APARTE — ✅ HECHO (2026-07-21, backend PR #138 mergeado + verificado E2E)
- ✅ El dueño aclaró que las categorías de disfraz son **completamente separadas** de las de prenda. Backend:
  nueva tabla `categoria_disfraz` + endpoints `/api/v1/disfraces/categorias`. Front: **pantalla de gestión**
  (Disfraces → menú ⋮ → "Categorias de disfraz": crear/renombrar/archivar); el form y los chips usan ESTA
  taxonomía (no la de prenda). Regenerado `:api-client` (con los 3 reapply de gotchas + sufijos corridos).

## 4. Carrito del cliente + checkout
- ✅ **HECHO (2026-07-21, verificado E2E)** — **Carrito del cliente con DISFRACES**: en el detalle del disfraz el
  cliente arma las piezas (ruleta) y toca **"Agregar al carrito"** (antes rentaba/vendía directo). El carrito
  muestra la línea de disfraz ("Nombre x1 · disfraz (N piezas)"), su precio unitario, subtotal y el **total**;
  "Finalizar pedido" cierra la compra/renta.
  - Verificado E2E: cliente → tienda → "Traje_Pirata_Opciones" → elige pieza → Comprar → Agregar al carrito →
    carrito ($150.000) → Finalizar pedido → **compra creada con código de retiro V-75EC3602**.
  - Archivos: `PedidoRepository.agregarDisfrazAlCarrito`, `DetalleDisfrazViewModel.agregarAlCarrito`,
    `EventoDisfraz.Agregado`, `DetalleDisfrazFragment` (botón + nav al carrito), `CarritoLineaAdapter`
    (etiqueta de disfraz + DiffUtil por prendaId/disfrazId).
  - Cuenta de prueba CLIENTE: `cli.disfraz@costumi.test` / `Costumi123!`.
- ✅ **HECHO (2026-07-21, verificado E2E)** — En el armado del disfraz, **las opciones sin stock NO se pueden
  seleccionar**: se muestran atenuadas con "Sin stock" en rojo y no responden al toque; la preselección cae en la
  primera opción CON stock, y si una pieza obligatoria no tiene ninguna opción disponible se avisa al intentar
  agregar. Aplica al armado del cliente **y** al modo asistido del dueño (comparten `SlotOpcionAdapter`).
  - Verificado E2E: se agregó "Ropa" (0 disponibles) como opción de "Traje_Pirata_Opciones" → aparece gris con
    "Sin stock" y al tocarla la selección no cambia.
- ✅ **HECHO (2026-07-21, verificado E2E)** — **Aviso al dueño de stock bajo**: el **Panel** (su pantalla de
  inicio) muestra arriba una tarjeta de alerta *"N variantes con stock bajo · toca para revisar"* que al tocarla
  lleva al Inventario. Antes el aviso existía (banner en Inventario + notificación IN_APP en Notificaciones)
  pero el dueño tenía que ir a buscarlo. Se recalcula al volver al Panel.
  - Archivos: `DashboardViewModel.stockBajo`, `DashboardFragment` (tarjeta + navegación), `fragment_dashboard.xml`.
  - Nota: la pantalla Notificaciones ya permitía disparar el aviso a mano (menú ⋮ → stock bajo) y listar los IN_APP.

---

## Ronda 2 de observaciones del dueño (2026-07-21) — ✅ HECHAS
1. **Tipo del disfraz (hueco funcional real):** el disfraz no tenía forma de decir si es de renta, de venta o
   ambos. Ahora tiene **tipo (RENTA/VENTA/AMBOS)** — backend PR #139 (migración V64 + validación en el único
   punto de paso, cubre directo/varios/carrito; suite 500/500). Front: el form pide **"Disponible para"** y
   muestra **solo el precio que aplica** (se fue el "precio de venta opcional" suelto); el **cliente solo ve la
   operación habilitada**. Verificado: elegir "Solo renta" oculta el precio de venta.
2. **Stock bajo: cuál.** Cada prenda con variante baja lleva ahora un chip **"Stock bajo"** en su fila del
   inventario (además del banner y del aviso en el Panel). Verificado con "Ropa".
3. **Categorías que no escalan.** La fila ya no crece sin control: se muestran hasta **5 chips** (más la elegida)
   y un chip **"Ver todas (N)"** que abre una **lista buscable**. Aplica al Inventario y a Disfraces.
4. **Elementos que no escalan.** El picker de elementos ahora es una **lista buscable** (escribís y filtra en
   vivo), con el stock de cada uno como subtítulo y los filtros por etiqueta que ya tenía. Verificado: "cap" →
   solo "Capa Real".
   - Componente reutilizable: `ui/common/ListaBuscable.kt` (+ `dialog_lista_buscable.xml`,
     `item_seleccion_buscable.xml`).

### Verificacion E2E del tipo (2026-07-22, contra Railway)
- Se crearon dos disfraces de prueba en la tienda demo: **"Pirata SOLO RENTA"** (tipo RENTA) y
  **"Pirata SOLO VENTA"** (tipo VENTA).
- **Cliente, detalle:** en SOLO RENTA **no aparece** el selector Rentar/Comprar y solo se ve "Renta $ 30";
  en SOLO VENTA solo se ve "Venta $ 150.000" y **desaparecen las fechas de renta**.
- **Cliente, lista del catalogo:** ahora cada tarjeta muestra **solo el precio de la operacion habilitada**
  (antes mostraba renta y venta siempre). `DisfrazVitrinaAdapter` — tambien pasa a respetar
  `precioVentaGeneral` cuando el dueño lo fijo.
- **Lista del dueño:** cada disfraz aclara "solo renta" / "solo venta" y muestra el precio que corresponde
  (`DisfrazAdapter`).
- **Backend (no solo la UI):** forzando la operacion prohibida por API el checkout responde
  *"Este disfraz es solo para renta, no se puede comprar"* y *"...solo para venta, no se puede rentar"*.

## Ronda 3 (2026-07-22) — ✅ HECHA
1. **Quitar un item del carrito** (backend [PR #140](https://github.com/JuanSebastianSU/costumi-backend/pull/140),
   mergeado, suite 501/501). Antes no existia ni endpoint ni boton, y un articulo que dejaba de ser valorizable
   **bloqueaba el carrito entero para siempre**.
   - `LineaDeCarrito` gana **id propio y estable** (el adaptador lo regeneraba en cada guardado);
     `Carrito.quitarLinea` + puerto `QuitarItemDelCarrito` + **`DELETE /api/v1/carritos/items/{lineaId}`**.
   - La consulta del carrito **ya no se cae** por una linea invalida: se pregunta antes de valorizar
     (`ResumenDeDisfraz` gana `permiteRenta`/`permiteVenta`) y la linea vuelve con `motivoNoDisponible`.
   - Front: cada linea tiene una **✕** (con confirmacion) y, si aplica, el motivo en rojo.
   - Verificado E2E reproduciendo el caso: cliente agrega un disfraz AMBOS para comprar → el dueño lo pasa a
     "solo renta" → el carrito **carga** y muestra *"Este disfraz es solo para renta, no se puede comprar"* →
     se quita con la ✕ y el carrito queda usable.
2. **Carrito vacio** ya no es un error: el 404 "No hay un carrito pendiente" se trata como vacio y se muestra
   *"Tu carrito esta vacio."* (ademas se limpia la lista, que antes quedaba pintada debajo del estado vacio).
3. **Filtro de categorias reformulado** (lo pidio el dueño: la fila horizontal no escala). Se **elimino la fila
   de chips**; queda **un solo boton "Categoria: X"** que abre la **lista con buscador**. Ocupa lo mismo con 3
   que con 300 categorias. Aplica a **Inventario** y a **Disfraces**.
   - Verificado con 12 categorias de disfraz: escribir "ha" deja solo "Halloween"; en Inventario "pan" deja
     "Pantalón" y el chip pasa a "Categoria: Pantalón".
4. **Mismo cambio en el catalogo del CLIENTE (pestaña Prendas)** — era la ultima fila de chips que quedaba
   (`TiendaFragment`). Ahora es el mismo boton "Categoria: X" con buscador; sigue apareciendo solo si la
   tienda tiene 2+ categorias, y si la elegida desaparece del catalogo vuelve a "Todas".
   Verificado: "cam" → "Camisa" → la vitrina queda en las 3 prendas de Camisa.
5. **Filtro por categoria tambien en la pestaña Disfraces del cliente** — no existia. Necesito backend:
   [PR #141](https://github.com/JuanSebastianSU/costumi-backend/pull/141) mergeado (suite 502/502), porque la
   vitrina solo daba `categoriaId` (un UUID) y el cliente **no puede leer la taxonomia de la empresa**; ahora
   `DisfrazResponse` trae tambien el **nombre** (`categoria`), como ya hacia `PrendaVitrinaResponse`.
   - Front: el mismo boton con buscador, generalizado para las dos pestañas. **Cada pestaña recuerda su propia
     categoria** (son taxonomias distintas) y el filtro solo aparece si hay 2+ categorias en ese catalogo.
   - Verificado E2E con 3 categorias de disfraz: "Piratas" deja 2 disfraces; al pasar a Prendas el filtro
     vuelve a "Todas" y al volver a Disfraces sigue en "Piratas".

## Estado: ✅ LOS 8 PENDIENTES ESTÁN HECHOS Y VERIFICADOS E2E (2026-07-21)
(1) inventario por categoría · (2) filtros por etiqueta en el picker · (3) multa en el cuadrito ·
(4) categoría + precio venta general del disfraz · (5) disfraces por categoría · (6) carrito del cliente con
disfraces + total · (7) opciones sin stock no seleccionables · (8) aviso de stock bajo al dueño en el Panel.
\+ La corrección de fondo: **categorías de disfraz como taxonomía aparte** (backend PR #138 + pantalla de
gestión + form + chips). `:api-client` regenerado contra Railway.

**Cuentas de prueba:** DUEÑO `dueno.demo.635935@costumi.test` / `Dueno123!` · CLIENTE `cli.disfraz@costumi.test`
/ `Costumi123!`.

**Limitación conocida de pruebas:** el date-range picker de Material no se puede manejar por adb, así que el
flujo de RENTA del cliente se probó por el camino de VENTA (mismo código); a mano funciona igual.
