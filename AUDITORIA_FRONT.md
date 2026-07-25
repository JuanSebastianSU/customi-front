# Costumi — Auditoría de diseño del FRONT (antes del backend)

> **Por qué existe.** Juan (2026-07-24): antes de diseñar el backend hay que revisar si el front tiene algo
> **mal diseñado**, porque si el backend se cimenta sobre una idea mala, se friega todo. Ejemplo que dio:
> **Reportes** — «ahí veo varios problemas». Esta auditoría es el análisis **a fondo**, pantalla por
> pantalla, buscando problemas de **coherencia de datos, correctitud y diseño**, no solo estética.
>
> **Regla de honestidad del proyecto (Juan):** todo debe quedar **funcional y bien vinculado al backend**.
> Nada simulado, nada inventado, todas las operaciones reales; precios, estados, procesos y valores tienen
> que **cuadrar de verdad**. Al terminar se prueba rol por rol y acción por acción.

## Reglas globales nuevas (Juan, 2026-07-24) — aplican a TODAS las pantallas
1. **Recencia primero.** Todo listado ordena **lo más reciente arriba** (rentas/ventas/pagos/etc. muestran
   las últimas realizadas, nunca las iniciales). Hoy hay que verificar el `sort` de cada lista (varias
   ordenan por id o de forma indefinida).
2. **Filtro de sucursal en TODA vista de datos** para quien tenga acceso a varias sucursales. Sin él, se
   mezclan las sucursales y es un lío. (En Reportes ya existe; falta llevarlo a Ventas, Rentas, Pagos,
   Devoluciones, Caja, Clientes, Panel, etc.) Ligado a la «sucursal activa» de `PLAN_IDENTIDAD_PERMISOS.md`.

---

## G15 · Reportes — ⛔ problemas serios (analizado a fondo 2026-07-24)

**1. ★ El filtro por fecha es INCOHERENTE (problema de datos, grave).**
La pantalla tiene un «Rango de fechas», pero solo **algunos** endpoints lo respetan:
- Respetan `desde/hasta`: `ingresosPorMetodo`, `masRentados`, `disfracesMasRentados`.
- **NO** lo respetan (ignoran la fecha): **`ingresos` (el número grande) y `ganancia`**, además de
  `masVendidos`, `disfracesMasVendidos`, `depositos`, `vencidas`, `empleados`, `tablero`.
Resultado: el usuario elige un rango y **la cifra principal de ingresos/ganancia NO cambia** (es histórico
acumulado), mientras otras secciones sí → **la pantalla miente sobre el período**. Es exactamente el tipo
de diseño que rompería el backend si lo copiamos.
> **Implicación de backend (sistémico):** `/reportes/ingresos` y `/reportes/ganancia` son **acumulado
> histórico sin período**. Lo mismo afecta al **Panel (G1)**, que muestra «Ingresos acumulados» porque el
> endpoint no sabe de «hoy». **El backend debe hacer ingresos/ganancia period-aware** (`desde/hasta`), y
> así Reportes filtra coherente y el Panel puede mostrar «hoy / 7 días». (Ya estaba anotado a medias como
> «ingresos del día» en `PROGRESS.md`; acá se ve que es transversal a todo el reporte.)

**2. No hay período seleccionable de verdad.** Solo un rango libre (que además aplica parcial). Falta el
selector hoy / semana / mes / rango que pide la spec, y que aplique a **todo** por igual.

**3. Todo es texto plano, no un dashboard.** Ingresos, ganancia, método y depósitos son `TextView` con
strings concatenados («Renta X · Venta Y», «Ingresos X − costo Y = Z», «Efectivo/Tarjeta/Transf»). Para una
pantalla de métricas es pobre: sin tarjetas, sin **gráficas** (la spec las pide), la cifra clave se lee
como una frase. Los rankings sí tienen barras (bien); el resto no.

**4. Ganancia como fórmula apretada en una línea** — densa e ilegible. Debería ser cifra destacada +
desglose.

**5. «Ventas por empleado» muestra el EMAIL, no el nombre.** `e.email` → «mostrador.h3@costumi.test (3)».
Ilegible; debe ser el nombre. (Backend: `EmpleadoVentas` ¿trae nombre? si no, agregarlo.)

**6. «Rentas vencidas» sin identidad.** Muestra «N días · vence FECHA · importe» pero **no dice de qué
renta / qué cliente / código**. No es accionable y no se sabe de quién es.

**7. Sin agrupación.** Es un scroll larguísimo que mezcla Finanzas + Productos + Personal + Inventario sin
pestañas ni secciones. Abrumador. Debería agruparse (o tabs).

**8. Falta el filtro de sucursal como algo prominente** (existe como dropdown perdido arriba) y la regla
global #2 lo pide en todas las de datos, no solo acá.

---

## ★ Hallazgos SISTÉMICOS (afectan a casi todas las listas de dinero/estado)

**A. La «recencia primero» es IMPOSIBLE hoy (regla global #1).** No es solo de ordenar: los modelos **no
traen la fecha**.
- **`VentaResponse` no tiene NINGUNA fecha** → la lista de Ventas no muestra *cuándo* se vendió, no puede
  ordenar por recencia, ni filtrar por período. La tarjeta hoy solo dice cliente·código·total·estado.
- **`RentaResponse`** trae `fechaRetiro`/`fechaDevolucion` **pactadas**, pero **no la fecha en que se
  registró** → no se puede ordenar «la última renta hecha arriba».
- **`OperacionPago`** (Pagos) tampoco trae fecha.
- El orden actual de las listas depende del **default del backend** (los `*PagingSource` no ordenan).
> **Backend:** exponer las **fechas reales del ciclo** (registrado/entregado/devuelto/cerrado) en
> Venta/Renta/Devolución/Pago, y **ordenar DESC por fecha** por defecto. Es «FECHAS DEL CICLO DE VIDA» del
> lote, pero ahora es **obligatorio** por la regla de recencia, no un «nice to have».

**B. NINGUNA lista/panel tiene filtro de sucursal (regla global #2).** Ventas, Rentas, Pagos, Panel,
Devoluciones, Clientes, Caja son **de toda la empresa**: para un dueño con varias sucursales, todo se
mezcla. (Reportes sí lo tiene; el resto no.)
> **Backend:** los endpoints de listado deben aceptar `sucursalId` (como ya hace `/reportes`). **App:**
> filtro de sucursal ligado a la **sucursal activa** (ver `PLAN_IDENTIDAD_PERMISOS.md`).

**C. Ingresos/ganancia sin período** (de Reportes/Panel, arriba): las cifras financieras no reflejan un
rango → `/reportes/ingresos` y `/ganancia` deben aceptar `desde/hasta`.

**D. ★ Dinero que la APP calcula o formatea (riesgo de que NO cuadre).** Donde el dinero lo da el
servidor, cuadra; el riesgo está donde la app lo toca:
- **POS — total del ticket calculado en la app** (suma de líneas). Si la tienda tiene **impuesto**
  configurado (existe en `ConfiguracionResponse`), el total mostrado (sin impuesto) **NO coincide** con el
  `venta.total` que cobra el servidor (con impuesto). El cliente vería un total y se le cobraría otro.
- **Cobro (`PagoConcepto`) — saldo pendiente recomputado en la app**: `pendiente = total(arg) + multa −
  saldoNeto`, mezclando un `total` que llega por argumento (puede venir sin impuesto/desactualizado) con
  valores del comprobante. Debería salir **entero del comprobante del servidor**, no recomputarse.
- **Devolución** (arriba): reimplementa el recargo.
- **Mis multas** (cliente): suma el saldo total en la app (`fold` de los saldos). Suma pura, bajo riesgo,
  pero idealmente el total lo da el servidor.
> **Regla:** el número que se muestra y se cobra debe ser **el del servidor**. La app no recalcula
> impuesto/saldo/liquidación; a lo sumo previsualiza y lo marca como estimación.

**E. Dinero SIN FORMATO (`comoPrecio`) en varios mensajes.** Se imprime el `BigDecimal` crudo:
- Caja: «sobra 5000.00» (cierre de turno).
- Cobro: «Pago registrado por 5000.00», «Cobro mixto por … · vuelto …».
Hay que pasar TODOS por `comoPrecio` («$ 5.000»). Es plata mostrada mal.

---

## G1 · Panel — ⚠️ problemas
- **Ingresos acumulados** (hallazgo C): el número grande es histórico, no «hoy». El label es honesto, pero
  un dueño quiere «hoy» + tendencia. Backend period-aware.
- **Sin filtro de sucursal / sin sucursal activa** (hallazgo B): el panel y sus alertas (vencidas, stock
  bajo, reembolsos) son **de toda la empresa**; con varias sucursales no se puede ver «cómo va la sucursal
  X». Falta la sucursal activa.
- Alerta de **devoluciones por cerrar** falta (ya anotado): no hay forma de contar rentas `DEVUELTA`.
- Estructura de alertas: **bien** (urgente primero, accionable).

## G12 · Pagos y cobros — ⚠️ problemas
- **Sin fecha ni orden por recencia** (hallazgo A): la fila es código·tipo·total·estado, **sin cuándo**;
  difícil encontrar «el cobro que acabo de hacer».
- **Sin filtro de sucursal** (hallazgo B).
- Falta **resumen** (cobrado hoy por método) y **saldo pendiente** por operación (ya anotado): hoy es solo
  un selector de operaciones, no un tablero de cobros.
- El destinatario es solo el **código de retiro**; sin nombre de cliente no se sabe de quién es sin abrir.

## G8 · Ventas (lista) — ⛔ problema de datos
- **`VentaResponse` no trae fecha** (hallazgo A): la lista **no puede** mostrar ni ordenar por cuándo se
  vendió. Rompe la regla de recencia de raíz.
- **Sin filtro de sucursal** (hallazgo B). Fila y acciones (cobrar/devolver): bien.

## G9 · Rentas (lista) — ⚠️ problemas
- **Sin fecha de registro** (hallazgo A): trae las fechas *pactadas* pero no *cuándo se creó*, así que no
  se puede ordenar «la última hecha arriba».
- **Sin filtro de sucursal** (hallazgo B). **Pestañas por estado** faltan (ya anotado).
- Ciclo de estados y acciones por estado: **muy bien diseñado** (entregar cotejando código, devolver con
  aviso, extender con calendario).

---

## G10 · Devoluciones (liquidación) — ✅ sólida, con 1 fragilidad
- **El cálculo del dinero es correcto y se apoya en el servidor**: la liquidación (depósito − daños −
  retraso) se muestra en vivo y, al registrar, se usa el `remanente`/`multa` **reales** del backend
  (verificado que cuadran, en `PROGRESS.md`). El recargo respeta la política (FIJA vs ACUMULATIVA × días).
- ⚠️ **Fragilidad: la lógica de dinero está DUPLICADA** — el preview reimplementa en la app la cuenta del
  recargo/liquidación que también hace el servidor. Si la política del backend cambia (o el `config` que
  lee la app difiere), el **preview miente** frente al cliente aunque el cargo final sea otro. Regla: el
  preview es *estimación* y la verdad es el servidor; hay que garantizar que **nunca divergen** (idealmente
  que el número lo calcule/‌confirme el backend, no la app).
- Menor: los nombres de prenda se resuelven contra una lista topada en **200** (`repo.prendas()`); una
  renta con una prenda fuera de esas 200 mostraría el nombre vacío.

## G11 · Clientes + Estado de cuenta — ✅ coherente, detalles
- El desglose sale de `EstadoDeCuentaResponse` (**una sola fuente de verdad**, servidor): importe, multa,
  pagado, saldo por renta + totales. Bien.
- Menor: la fecha se muestra como **ISO cruda** (`fechaRetiro.toString()` → «2026-07-14»), no legible
  (`comoDiaMes`). Y el orden de las líneas debe ser **reciente primero** (regla #1) — depende del backend.

## G14 · Caja / turnos — ⚠️ problemas
- El **cuadre lo calcula el servidor** (`corte["EFECTIVO"]`, `diferenciaEfectivo`): bien, no se reimplementa.
- ⛔ **Dinero SIN FORMATO en el mensaje de cierre**: «sobra ${dif}» / «falta ${dif.abs()}» imprime el
  `BigDecimal` crudo (p. ej. «sobra 5000.00») en vez de `comoPrecio` («$ 5.000»). Se ve un número mal
  formateado — y es plata. Corregir.
- **`TurnoResponse` sin timestamps** (ya anotado): no se puede mostrar «cuánto lleva abierto» ni fechar.
- **Sin filtro de sucursal** en la lista de turnos (regla #2): con varias sucursales se mezclan.

---

## G7 · Punto de venta — ⚠️ riesgo de total
- El registro manda las líneas y el servidor computa `venta.total` (bien) + idempotencia estable (bien).
- ⛔ **El total del ticket que ve el empleado es una suma de la app**: si hay **impuesto** configurado, no
  coincide con lo que cobra el servidor (hallazgo D). Hay que mostrar el total **con impuesto** (o el que
  confirme el backend).
- Menor: la venta **con disfraz** (`registrarMixto`) emite `total = null` → la confirmación no puede
  mostrar el total cobrado.

## C5 · Carrito / checkout — ✅ total del servidor
- El total sale de `CarritoResponse` (**servidor**), no se recalcula. Bien.
- Pendiente (ya anotado): depósito reembolsable, variante por línea, editar cantidad, fecha de creación.

## Cobro (`PagoConcepto`) + C7/C8/C9 cliente — ⚠️ formato y saldo
- **Saldo pendiente recomputado en la app** (hallazgo D) — debe venir del comprobante del servidor.
- **Mensajes de dinero sin formato** (hallazgo E).
- Cliente (C7 Pago / C8 Mis pedidos / C9 Mis multas): montos y estados salen del **historial/deudas del
  servidor** (bien); C9 suma el total en la app (suma pura). Recencia (regla #1) por confirmar en el orden
  del historial.

---

## G2 Inventario / G4 Stock — ✅ mayormente bien (stock YA es por sucursal)
- ✅ **Buena noticia:** `GrupoDeStockResponse` trae **`sucursalId`** + disponibles/rentadas/dañadas por
  grupo. El **stock ya está modelado por sucursal** en el backend; G4 muestra cada grupo con su sucursal y
  permite transferir entre sucursales. → el epic multi-sucursal **no necesita rehacer el modelo de stock**.
- ⚠️ **G2 (lista de prendas) no muestra stock/disponibilidad de un vistazo** (solo precios): para saber si
  una prenda tiene stock hay que entrar a «Stock». Spec ya lo pedía («stock por sucursal de un vistazo»).
- ⚠️ **G4 mezcla las sucursales** (con etiqueta de sucursal por grupo): el filtro/sucursal activa (regla
  #2) ayudaría a enfocar.

---

## A1 · Acceso (login / registro / splash) — ⚠️ cambia con el epic
- ⛔ **El flujo login → aterrizaje asume `rol ↔ modo` 1:1 (excluyente).** El Splash decide el destino por
  `rolActual().modo`; si el rol es de gestión, entra directo a GESTION. **Bajo el epic de identidad**
  (persona = siempre cliente + membresía), esto **cambia**: debe aterrizar en **modo cliente** con opción
  de **cambiar a modo trabajo** (con re-auth). Login/Registro/Splash hay que rehacerlos para «persona con
  membresía». (Ver `PLAN_IDENTIDAD_PERMISOS.md`.)
- ⛔ **Registro no pide aceptar Términos y Condiciones** (Juan los pidió para invitación; también aplican
  al crear cuenta). Falta el checkbox + la política (ligado a privacidad, PLAN_PRODUCTO #9).
- Sin rediseñar per spec: onboarding en el primer ingreso + biometría (item 12).

## S1 · Panel SuperAdmin — ⚠️ sin rediseñar
- Funcional: solicitudes de tienda (aprobar/rechazar) + empresas (suspender/reactivar). Aprobar «crea la
  Casa Matriz» y promueve a Dueño (ligado a identidad).
- **Nunca entró al rediseño**: es una lista plana que mezcla solicitudes y empresas. Falta lo de la spec:
  métricas de plataforma + ficha por empresa. Sin paginación (interno, bajo riesgo).

---

## Resto (bajo riesgo — no tocan dinero/estado)
Revisadas de pasada y/o durante el rediseño de esta y sesiones previas; sin problemas de datos detectados,
solo detalles menores/estéticos:
- **Formularios** (G3 ficha prenda, G6 armar disfraz, prenda/disfraz form): ya rediseñados; el dinero que
  manejan (precios) va tal cual al backend. Ojo transversal: **impuesto** (ver hallazgo D) donde se
  previsualicen totales.
- **Catálogo cliente** (C2 Tienda, C3/C4 detalle): totales del detalle salen bien; identidad de tienda
  pendiente de backend (ya anotado).
- **Config (G18), Taxonomía (G22), Reembolsos (G13), Notificaciones (G19)/Mensajes (G20), Mis carritos
  (C6), Auditoría (G21)**: rediseñadas; Reembolsos/Auditoría/Disfraces = Grupo B de paginación (ya anotado).
- Aplicar a TODAS: reglas globales #1 (recencia) y #2 (filtro de sucursal) donde haya listas de datos.

---

## Pendiente de auditar (checklist — para no olvidar ninguna)

> Se revisa cada una a fondo (Fragment + ViewModel + layout + repo) buscando problemas de datos/estado/
> orden/diseño. Se marca ✅ cuando queda auditada acá.

**Dinero / estado / datos (prioritarias):**
- [x] **G15 Reportes** · [x] **G1 Panel** · [x] **G12 Pagos** · [x] **G8 Ventas** · [x] **G9 Rentas** — arriba.
- [x] **G10 Devoluciones** · [x] **G14 Caja** · [x] **G11 Clientes + Estado de cuenta** — arriba.
- [x] **G7 Punto de venta** · [x] **C5 Carrito** · [x] **Cobro + C7/C8/C9 cliente** · [x] **G2/G4
      Inventario/Stock** · [x] **A1 Acceso** · [x] **S1 SuperAdmin** — arriba.

**Resto (secundarias, bajo riesgo):** cubiertas en la sección «Resto» de arriba (formularios, catálogo
cliente, config/taxonomía/reembolsos/notif/mensajes/carritos/auditoría). Sin problemas de datos; aplicar
las reglas globales #1 y #2 donde corresponda.

> **Auditoría sustancialmente completa.** Lo crítico (todo lo que toca dinero/estado + lo estructural del
> epic) está revisado a fondo. Los hallazgos que deben guiar el backend son los **sistémicos A–E** +
> **A1** (flujo de acceso cambia con identidad) + **S1** (sin rediseñar).
