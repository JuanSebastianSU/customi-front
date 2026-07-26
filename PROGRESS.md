# Costumi — PROGRESS de la app Android

> **Qué es este documento.** El tablero **vivo** del frente Android: qué está hecho, qué está en curso y
> qué sigue. Es lo primero que se lee al retomar el trabajo.
>
> **No confundir** con el `PROGRESS.md` del **backend** (repo `costumi-backend`), que es otro frente.
>
> **Documentos hermanos:**
> - `PLAN_PRODUCTO.md` — los 16 ítems para llegar a producto comercializable (el **qué** y el **por qué**).
> - `PLAN_ROOM_OFFLINE.md` — el detalle técnico de la capa local (normas, procedimiento, tests).
> - `PENDIENTE_FRONTEND.md` — historial de correcciones funcionales ya cerradas.
>
> **Reglas de este archivo:** se actualiza **al terminar** cada ítem, no antes. No se borra historial: se
> mueven ítems entre secciones. Un ítem solo pasa a ✅ si cumple su definición de "hecho".

---

## Donde vive cada cosa (2026-07-23)

| Que | Donde |
|---|---|
| **App Android** (codigo + estos documentos) | `C:\Users\User\AndroidStudioProjects\AppCustomi2` |
| **Backend** (codigo + `BACKEND_REQUIREMENTS.md` + su `PROGRESS.md`) | `C:\Users\User\AndroidStudioProjects\costumi-backend` — repo git, remote `JuanSebastianSU/costumi-backend` |
| **Scripts de datos de prueba** | `AppCustomi2/scripts-datos-prueba/` (ver su `LEEME.md`) |

> El backend **estuvo clonado en la carpeta temporal de la sesion** hasta el 2026-07-23; se movio a disco
> permanente porque ahi se pierde todo (incluidos commits sin subir). **Nada de valor vive en el temporal.**
> Las decisiones funcionales del backend se anotan en **su** `BACKEND_REQUIREMENTS.md` y **su** `PROGRESS.md`,
> no aca; aca va el **lote** de lo que el rediseno le pide.

## Estado general

| Frente | Estado |
|---|---|
| **Arquitectura** (MVVM, lifecycle, hilos) | ✅ Sana — ver auditoría abajo |
| **Funcionalidad** | ✅ Los flujos de cliente y gestión operan de punta a punta |
| **Persistencia local (Room)** | 🔻 Una sola tabla; plan aprobado sin ejecutar |
| **Listo para publicar** | ❌ Faltan los 4 bloqueantes del `PLAN_PRODUCTO.md` |
| **Tests** | ❌ 1 unitario, 0 instrumentados |

---

## 🔜 En curso

**Rediseño por tandas** (antes de Room). Corazón del producto primero: la pantalla de armar disfraz.

> **★ REGLA DEL REDISEÑO (Juan, repetido 2026-07-24).** El front se hace **ideal / correcto / mejor**,
> **NO** limitado a lo que el backend expone hoy. Cada pantalla debe tener **lo que necesitaría para ser
> excelente**, aunque el backend no lo dé todavía: se **maqueta** la UI completa (degradando bien cuando
> falta el dato) y se **anota el backend** que debe cumplirlo. Orden: **front ideal → el backend cumple.**
> No recortar una pantalla porque el backend no tiene el campo; no preguntar si maquetar: se maqueta.

- [x] **Armado de disfraz (cliente)** rediseñado: lista de piezas por fila, progreso, hoja de selección
      en grilla con etiquetas con nombre, total real en vivo. Verificado en emulador. Ver `C4` en
      `ESPECIFICACION_UI.md`.
- [x] **Backend: etiquetas con nombre en la ruleta** (PR #156) — la opción trae `tipoNombre`/`valorNombre`.
- [x] **Backend: nombres distintos para línea de renta/venta** (PR #157) — fix de colisión de schema en
      springdoc; el regen del `:api-client` volvió a ser limpio (sin edits a mano). Cliente regenerado
      completo, app compila y corre.
- [x] **Filtro por facetas** en la hoja de selección de pieza (ver `C4.1`). Backend (PR #158): facetas en
      la respuesta de la ruleta, conteo **dinámico**, semántica OR-intra/AND-entre, estrena
      `seleccionablePorCliente`. App: chips por dimensión con conteo, filtro en vivo, limpiar filtros,
      estado vacío accionable. Verificado en emulador (Camisa: Color/Talla, al elegir L el conteo de Color
      baja a 1).
- [~] **Carrito (cliente)** rediseñado (ver `C5`): cabecera de tienda (sucursal + dirección), línea como
      card con período propio (ícono) separado del aviso de no-disponible (rojo, bloquea el checkout),
      pie con Total a pagar. Verificado en emulador. **Pendiente de backend** para completar: depósito
      reembolsable (fila maquetada, oculta), variante talla/color por línea (`SeleccionResponse` no trae
      etiquetas), editar cantidad sin quitar+reagregar (no hay endpoint), y **fecha en que se armó el
      pedido** (el carrito/línea no trae fecha de creación).
- [~] **Tienda / catálogo (cliente)** (ver `C2`): grilla de 2 columnas (antes lista vertical) con foto
      de vitrina arriba, nombre, piezas/categoría y precio (renta primaria + venta debajo), en las dos
      pestañas (Disfraces, Prendas). Verificado en emulador. **Pendiente de backend**: cabecera de
      identidad (portada, logo, descripción, dirección, horario, abierto/cerrado) — mismo lote de backend.
- [ ] Resto de **Tanda 1** (cliente): explorar (necesita identidad de tienda del backend).
- **Tanda 4 (cliente)** — barrido hecho (2026-07-24):
  - [x] **C7 Pago** — código de retiro como héroe (centrado, display, monoespaciado), método de pago
        rotulado, aviso de 24 h separado. Solo layout. Compila; **no verificado en emulador** (solo se
        alcanza tras completar un checkout, y el date-picker de renta no se maneja por adb).
  - [x] **C8 Mis pedidos** — ✅ **verificado en emulador**: chips de filtro por estado
        (Todos/Por retirar/Activos/Cerrados, client-side interino — Grupo B) —probado que «Por retirar»
        filtra a 1—, pastilla de estado, fecha legible. **Backend anotado:** «Por pagar» + paginar
        historial + `?filtro=`.
  - [x] **C9 Mis multas** — cabecera de total destacada + aviso de pago en tienda + pastilla
        Pendiente/Pagado. En emulador se vio el **estado vacío** (el cliente de prueba no debe nada); la
        tarjeta poblada no se pudo mostrar en vivo por falta de datos. **Backend anotado:** «Pagar ahora»
        online (sucursalId + cobro del saldo).
  - [x] **C10 Perfil** — ✅ **verificado en emulador**: cabecera con avatar-inicial «J» + nombre + correo,
        secciones (Tus datos / Cuenta), logout separado en rojo. **Backend anotado:** foto, direcciones,
        preferencias de notif, cambio de contexto (H1).
  - Quedan bloqueadas por identidad de tienda: **C1 Explorar**, **G17 Sucursales**.
- **Tanda 2 (gestión)** — barrido:
  - [x] **G5 Disfraces** y **G2 Inventario**: ya estaban bien; foto 56→72dp. Verificado en emulador.
  - [x] **G6 Armar disfraz (dueño)** ⭐ — **selector visual UNIFICADO** (grilla con foto/precio/stock) para
        prenda fija (1 toque) y parte personalizable (multi-selección con check + "Listo (N)"), en vez de los
        desplegables por nombre. **Barra de filtros COMPACTA** (una fila: Categoria · Color · Talla; cada chip
        abre un selector con buscador y conteo dinámico) — escala a cientos de valores sin muro de chips.
        Sheet expandido que no se cierra al scrollear. Se quitó el desplegable de categoría redundante del
        slot. Tarjeta con vista previa + sugerido en vivo. **Todo verificado en emulador.** _El peor punto de
        la app, resuelto y a nivel producto._
  - [x] **G7 Punto de venta** ⭐ — **buscador visual de artículos** que agrega al **ticket** (línea con foto,
        nombre, precio editable, **cantidad −/+**, subtotal; total en vivo; agregar el mismo artículo suma
        cantidad); **cliente por selector con buscador**; botón **Cobrar**. **Agregar artículo Y Agregar
        disfraz** son pickers visuales en grilla **con filtros** (artículo: Categoría/Talla/Color; disfraz:
        Categoría). **Verificado en emulador.** _El segundo peor punto, resuelto._ Pendiente (extensiones):
        "Nuevo cliente" en línea (nombre/teléfono/email → backend) y código de retiro grande tras cobrar.
  - [x] **Refactor DRY**: el selector visual de catálogo + los filtros compactos se extrajeron a componentes
        reutilizables (`ui/common/SelectorCatalogo` + `FiltroCompacto` + `PrendaCatalogoGrillaAdapter`). Los
        usan armar disfraz (G6), y ambos pickers del POS. Un solo lugar → misma experiencia en todos.
        Re-verificado G6 tras la extracción.
  - [~] **G9 Rentas** — la fila dejó de ser un volcado de datos y responde en orden: **quién y qué**
        (cliente, código de retiro, nº de artículos) → **cuándo** (período legible con ícono) → **qué urge**
        («Vence en 6 dias», «Vencida hace 3 dias», «Devuelta · falta cerrarla», en el color del estado) →
        **qué hacer** (una acción principal por estado: Entregar / Registrar devolución / Cerrar renta; el
        resto en «Mas»). El menú ya **no ofrece las 8 acciones siempre**: cada estado muestra solo las
        válidas, y si queda una sola el botón la ejecuta directo («Ver contrato»). **Entregar** confirma
        mostrando el código de retiro y cuántos artículos salen (es el momento de cotejarlo); **devolver
        sin revisar** avisa que no cobra daños; **extender** usa calendario (antes había que escribir
        `AAAA-MM-DD` a mano). Pastilla de estado con colores semánticos nuevos (verde = al día,
        ámbar = atención, rojo = vencida): el rosa de Material hacía ver una renta sana como problema.
        **Verificado en emulador** con los 5 estados sembrados (`sembrar_rentas_estado.py`) y probando
        entregar → activa, extender → recalcula importe, menús y desglose.
        **Pendiente de backend** (lote): las **pestañas** Por entregar / Activas / Vencidas / Cerradas con
        conteos — sin filtro en el servidor solo filtrarían la página cargada.
  - [~] **G1 Panel** — abre por **lo que requiere atención hoy**, no por ocho números iguales: tarjeta con
        una fila por alerta real (rentas vencidas · variantes con stock bajo · reembolsos por responder),
        cada una con su gravedad en color y **un toque que lleva a la lista que la resuelve**; si no hay
        nada, lo dice («Todo al dia»). Se recalcula al volver al panel. Debajo: fecha de hoy, ingresos
        (etiquetados **acumulados**, que es lo que devuelve el reporte — decir «Ingresos» a secas hacía
        creer que eran los de hoy), inventario con lo que se mira a diario arriba (Disponibles · Rentadas
        ahora) y lo fuera de circulación resumido en una línea que **nombra solo lo que no es cero**.
        **Barra fija «Nueva venta»** (la acción más frecuente del día). **Verificado en emulador**: las 3
        alertas, el estado «Todo al dia», la navegación de cada alerta y el FAB al POS.
        **Pendiente de backend** (lote): ingresos **del día**, serie de **7 días** y variación vs. ayer;
        y «devoluciones por cerrar» como alerta (hoy no hay forma de contar las rentas DEVUELTAS).
  - [x] **G10 Devoluciones (registrar)** — la **liquidación se ve mientras se revisa**, no después: barra
        fija con «Deposito − danos − retraso» y el resultado en grande, *A devolver al cliente* (verde) o
        *El cliente queda debiendo* (rojo), más «N de M piezas resueltas». Arriba, lo que decide el
        recargo: pactada vs. real y **«3 dias de retraso»**, con calendario para corregir la fecha.
        Los cargos **se sugieren solos**: los daños suman el `valorDano` de las piezas marcadas y el
        recargo sale de la política de la tienda (`recargoPorRetrasoPorDia` × días, o fijo); si el
        usuario escribe el suyo, se respeta y el helper lo dice. Cada pieza se responde con **un toque**
        (chips Bien · Danada · En limpieza · No llego) y muestra **foto** y su valor de daño; la nota
        quedó plegada (antes era un campo obligatorio por pieza). Se eliminó la contradicción
        estado «Perdida» + check «Llego»: no llegó ⇒ perdida.
        **Verificado en emulador y contra el servidor**: la pantalla anticipó $26.000 y la devolución
        registrada devolvió `remanente: 26000` (3 días × $8.000 de recargo sobre $50.000 de depósito).
        _Nota: para poder probarlo se configuró `recargoPorRetrasoPorDia = 8000` en la tienda demo, que
        estaba en 0._
  - [x] **G8 Nueva renta + consistencia de componentes** — al revisar las pantallas juntas aparecieron
        **tres formas distintas de pedir la cantidad** (stepper del cliente, stepper del POS, campo de
        texto en renta y en agregar disfraz) y **tres de pedir fechas** (calendario de rango del cliente,
        dos campos `AAAA-MM-DD` en renta, campo + ícono en devolución). Peor: **Nueva renta seguía con los
        desplegables por nombre** — el gemelo del POS había quedado sin rediseñar.
        Se extrajeron a `ui/common` y ahora hay **una sola** implementación de cada cosa:
        `SelectorDeCantidad` + `widget_cantidad` (stepper − n + con **tope de stock**: al llegar dice
        «es todo el stock» y apaga el +), `SelectorDePeriodo` + `widget_periodo` (calendario de rango, sin
        pasado, con «27 jul → 30 jul · 3 dias»), `LineasDeArticulos` (agregar/quitar/cantidad/precio/
        subtotal, con el multiplicador de días para la renta) y `SelectorDisfraces` (el picker visual que
        estaba embebido en el POS). `item_linea_venta` + `item_linea_renta` se fundieron en
        **`item_linea_articulo`**, una sola tarjeta para vender y rentar.
        **Nueva renta** quedó igual que el POS: catálogo visual con filtros, línea con foto y stepper,
        precio por día, disfraces, y el total en vivo («Total: $150.000 · 1 prenda · 3 dias»).
        El rótulo del cliente se unificó: ambas dicen «Cliente» con un helper que explica la diferencia
        real (en venta es opcional —venta al paso—, en renta es obligatorio).
        Migradas al mismo componente: cliente (detalle de prenda y de disfraz), POS, Nueva renta y
        agregar disfraz al pedido. **Verificado en emulador**: las 4 pantallas, el tope de stock, el
        calendario de rango y el POS reverificado tras la refactorización.
  - [x] **Barrido de 5 (G8·G13·G12·G4·G16)** — todas verificadas en emulador:
    - **G8 Ventas (lista)**: fila con jerarquía como G9 (cliente, código de retiro + nº artículos, total,
      **pastilla de estado** Confirmada/Devuelta en parte/Devuelta) y acciones a mano (**Devolver** solo
      si quedan unidades, **Cobrar** siempre). `EstadoDeVenta.kt` mapea etiqueta/tono.
    - **G13 Reembolsos**: **pestañas Pendientes | Resueltas | Todas** (la bandeja NO es paginada → 100%
      en la app, sin gap), tarjeta con pastilla, **motivo del cliente** en primer plano, y acciones
      (Aprobar + Mas: Rechazar / Registrar devolución). Vacío accionable por pestaña.
    - **G12 Pagos y cobros**: la fila la manda el **código de retiro** (es lo que se cobra a quien lo
      muestra), con tipo·total, pastilla y chevron. Toggle Ventas/Rentas y buscador intactos.
    - **G4 Stock por prenda**: **encabezado que explica qué es un grupo de stock** (la queja de la spec),
      y la variante se lee en cristiano (**«Rojo · M»** / **«Stock general»**) resolviendo los nombres
      desde la taxonomía, en vez de «Variante (2)». Sucursal + total + rentadas debajo.
    - **G16 Empleados**: fila con **avatar**, correo, sucursales por nombre, **rol y estado como
      pastillas**, y las acciones (incl. **Actividad**, que la spec pedía y ya existía) en un botón
      **Gestionar**.
  - [x] **Barrido de 5 (G18·G22·G21·G14·G15)** — todas verificadas en emulador:
    - **G18 Configuración**: cada switch con **nombre claro + una línea que explica qué hace y qué pasa
      si se apaga** (antes eran switches sin contexto), en tarjeta por tema; los campos (impuesto, moneda,
      recargo, ventana) con helper que los explica. **Guardar fijo abajo.**
    - **G22 Taxonomía (tipos de etiqueta)**: **encabezado que explica qué es una etiqueta** con ejemplos
      (Talla/Color → S,M,L / Rojo,Azul), y los flags técnicos en cristiano: **«Separa el stock · El cliente
      filtra por esta»** en vez de «Define variante · Cliente elige».
    - **G21 Auditoría**: **filtro por tipo de acción en chips** generados **desde los datos** (Todas ·
      Devolucion · Empresa · Stock), evento en **lenguaje natural** y **fecha relativa** («ayer 17:34»,
      «14 jul 16:16») con el helper compartido `comoFechaHora`. Verificado el filtro (Stock → 2 eventos).
      **Gap de backend**: `AuditoriaResponse` no trae el **usuario** → no se puede filtrar por quién.
    - **G14 Caja/turnos**: fila con **pastilla de estado** (Abierto/Cerrado) y el **cuadre en color**
      (Cuadre OK verde / Falta rojo / Sobra ámbar / En curso). **Bug encontrado y corregido: la pantalla
      estaba HUÉRFANA** — existía en el nav graph pero **ningún menú navegaba a ella**; se agregó «Caja /
      turnos» al menú Más. **Gap de backend**: `TurnoResponse` no tiene timestamps (apertura/cierre) →
      no se puede mostrar «cuánto lleva abierto» ni fechar el turno.
    - **G15 Reportes**: los **rankings ahora son barras proporcionales** (mini-gráfico calculado en la
      app, sin librería) — prendas/disfraces más vendidos y ventas por empleado. **Gaps de backend**:
      gráficas de serie temporal + ingresos por día/período, y **rankings con foto** (el ranking no trae
      `fotoUrl`).
  - [x] **G3 Ficha de prenda** — era una columna de nueve campos con la **foto al final** y los **cinco
        precios siempre**, incluido «precio de renta por día» en una prenda que solo se vende. Ahora va por
        secciones que responden en orden: **qué es** (foto arriba + nombre/categoría) → **qué se puede
        hacer con ella** (*Rentar · Vender · Las dos*, de un toque, y **manda sobre los precios**: solo se
        piden los que aplican) → **precios** (con el costo de adquisición explicado: «lo que te costó a
        vos, el cliente no lo ve») → **«Si vuelve mal o no vuelve»**, que explica que esos dos valores son
        los que la app propone cobrar en la devolución (antes eran dos campos sin contexto) →
        **etiquetas**, diciendo para qué sirven (el cliente filtra con ellas). *Guardar* quedó **fijo
        abajo**. En edición el tipo y la categoría se ven pero no se tocan, con el motivo escrito.
        **Verificado en emulador** (alta, cambio de tipo ocultando el precio que no aplica, edición y
        guardado). Dos bugs cazados al verificar: el toggle no mostraba el tipo en edición (un botón
        deshabilitado deja de pintarse como seleccionado → ahora es no-clickeable, no deshabilitado).
  - [x] **G11 Clientes (lista y ficha)** — la lista ya tenía los filtros pero **en jerga**: *Pendientes ·
        Vencidas · Multas · Saldos*, que no significan nada en el mostrador. Ahora dicen lo que el backend
        filtra de verdad: **Debe plata · Renta vencida · Con multas · Algo pendiente**, y quedaron en **una
        sola fila** con *Ver archivados* separado por un divisor (antes eran dos filas sueltas).
        La fila del cliente pasó a tener jerarquía: **inicial en círculo**, nombre, **teléfono primero**
        (es con lo que se le llama cuando se pasa de la fecha), una sola pastilla de estado (lista negra
        manda sobre archivado) y la deuda como **botón tocable** con chevron —antes era texto rojo con
        «ver desglose» pegado al final, que no parecía tocable—.
        La **ficha** ganó *Guardar* **fijo abajo** (igual que G3) y, en edición, **Estado de cuenta** e
        **Historial de pedidos** dentro de la propia ficha: estaban solo en el menú de la lista, así que
        con el cliente delante había que volver atrás y buscarlo de nuevo.
        **DRY**: el desglose del estado de cuenta se extrajo a `EstadoDeCuentaSheet` — lo abren la lista y
        la ficha, y tienen que contar lo mismo. **Verificado en emulador** (filtro, desglose desde la lista
        y desde la ficha).
  - [x] **Desgloses sin foto**: el sheet compartido pintaba un **recuadro gris** por línea aunque no
        hubiera imagen (un estado de cuenta son ocho placeholders en fila). Ahora la foto se oculta si no
        hay; aplica también a los desgloses de renta, venta y devolución.
  - [x] **Iconos que decían otra cosa**: `ic_explorar` (una **brújula**) se usaba como icono del
        **buscador** en toda la app y en el botón *Agregar foto*. Se crearon `ic_buscar` (lupa) e
        `ic_foto` (cámara); la brújula quedó solo para la pestaña Explorar, que es lo que significa.
  - [x] **Icono de «agregar» en toda la app**: `ic_mas` dibuja **tres puntos** (menú de opciones) y se
        usaba en los FAB y en los botones *Agregar…* de 10 pantallas. Se creó `ic_agregar` (+) y se
        reemplazó donde significa agregar; los `item_*` conservan los tres puntos, que ahí sí son el menú.
  - [x] **G19 Notificaciones + G20 Mensajes automáticos** — ✅ **verificadas en emulador** (2026-07-24):
        G19 muestra canal amigable, pastilla «Enviada» verde, «Tu negocio» y fecha relativa; G20, la vista
        previa en vivo sustituyendo `{cliente}`→«Ana Torres», `{direccion}`, `{maps}`.
    - **G19**: primera pantalla del **Grupo A** paginada de verdad. Estrena el
      **`PaginaRemotaPagingSource` genérico** (para no seguir copiando `*PagingSource`); scroll infinito,
      buscador server-side, footer de carga. Fila rediseñada: canal amigable (`IN_APP` → «Aviso
      interno»), **pastilla de estado** (`pintarPastilla`/`Tono`), destinatario **«Tu negocio»** en
      avisos internos, **fecha relativa** (`comoFechaHora`). Gap de backend anotado en el lote:
      `NotificacionResponse.clienteNombre` (hoy el nombre sale de una lista topada en 100).
    - **G20**: lista fija de 6, no pagina. Se agregó **vista previa en vivo con datos de ejemplo** al
      editar (el hueco de la spec): `{cliente}`→«Ana Torres», etc.
- [ ] Tandas 3-4 — ver `ESPECIFICACION_UI.md`.

### 📄 Paginación y carga de listas — auditoría (2026-07-24, pedido de Juan)

Revisión de **cómo escala cada lista** al crecer los datos, y si los buscadores siguen sirviendo cuando
el registro está «muy atrás». Conclusión: **conviven dos mecanismos** y uno no escala.

**1. Paginación real (Paging 3, scroll infinito).** Carga de a 20 y pide la página siguiente al
deslizar; el buscador va **al servidor** (no filtra la página cargada), así que encuentra el registro
esté donde esté, y el `Pager` reinicia con `flatMapLatest`. Está bien y escala.
- **Gestión:** `G2 Inventario`, `G8 Ventas`, `G9 Rentas`, `G11 Clientes`, `G12 Pagos` (reusa los
  paging source de ventas/rentas).
- Piezas: `*PagingSource.kt` (4 casi idénticos) + `Pager` en el repo + `PrendasLoadStateAdapter` (footer
  de carga, reutilizable) + `PagingDataAdapter`.

**2. «Carga tope» disfrazada de lista (NO es paginación).** Pide **una sola página de `tamano = 100`**
(constante `TAMANO` en el repo, con el comentario «se muestran completas en pantalla») y pinta una lista
simple. El buscador va al servidor, pero **la respuesta sigue topada en 100**: sin buscar, el registro
#101 en adelante es **invisible** y no hay «cargar más». Es la bomba de tiempo. Afecta 6 listas de
gestión + los selectores (pickers) que piden 100/200.

Al querer paginarlas de verdad aparece el matiz clave: **un filtro que hoy corre en memoria sobre la
lista cargada se rompe al paginar** (solo filtraría las páginas traídas). Por eso las 6 se parten en dos:

- **Grupo A — solo buscador, sin filtro en memoria → se paginan YA (front puro, sin backend, sin
  regresión):**
  - `Devoluciones` (historial, `listar13`) — crece para siempre.
  - `G19 Notificaciones` (bandeja, `listar7`) — crece para siempre.
  - `G16 Empleados` (`listar10`) — rara vez >100, pero correcto y barato.
  - Los tres endpoints **ya devuelven** `RespuestaPaginada…{contenido,totalPaginas}`; el repo lo ignora
    y pide 100. Convertir = cambiar el repo al patrón Paging 3 que ya existe.
  - **Plan de implementación:** en vez de 3 `*PagingSource` casi-duplicados más (ya hay 4), crear **uno
    genérico** `PaginaRemotaPagingSource(fetch: (pagina, tamano) -> RespuestaRed<RespuestaPaginada…>)`
    y refactorizar los 4 existentes a él cuando se toque cada zona (no de golpe). VM: `flatMapLatest` +
    `cachedIn`. Fragment: `PagingDataAdapter` + `withLoadStateFooter` + `loadStateFlow` → `stateView`.
  - ⚠️ **Verificar orden en el backend:** hoy estas listas ordenan en la app (`fecha` desc en
    notificaciones, etc.). Al paginar, el **orden lo tiene que dar el servidor** o se pierde entre
    páginas. Ventas/Rentas ya funcionan así, lo que sugiere que el default del backend es estable; **hay
    que confirmarlo** para Notificaciones/Devoluciones (si el servidor no ordena por fecha desc, es un
    ajuste chico de backend — anotado abajo en el lote).
- **Grupo B — tienen filtro en memoria que la paginación rompería → paginar + `filtro=` server-side
  aterrizan JUNTOS (va al backend, NO se toca ahora para no regresar el filtro que hoy funciona):**
  - `G21 Auditoría` — chips de tipo de acción derivados de los datos cargados.
  - `G13 Reembolsos` — pestañas Pendientes / Resueltas / Todas.
  - `G5 Disfraces` — chips de categoría (`categoriaId` client-side sobre la lista).
  - Es el mismo patrón ya diferido en `G9 Rentas` (`?filtro=`) y `G8 Ventas`. Ítems en el lote de backend.

**Selectores (pickers) que piden 100/200 en memoria** (clientes, prendas para POS/renta/disfraz): son
defendibles **solo** si el usuario nunca hace scroll de 200 sino que **busca**; hoy varios no tienen
buscador server-side. No es urgente, pero cuando se rediseñe cada picker: buscador server-side, no tope.

**Regla para el resto del rediseño:** cualquier lista que pueda crecer sin límite va con Paging 3;
ninguna nueva se queda en `tamano = 100`. Si necesita filtro por estado/categoría, ese filtro va
server-side (a esta lista de backend), nunca en memoria sobre una lista paginada.

**Cliente — barrido (2026-07-24).** Ninguna lista del cliente pagina hoy; casi todas devuelven lista
plana (ni siquiera el tope de 100 de gestión):
- **C8 Mis pedidos** (`CuentaRepository.miHistorial` → `GET /clientes/me/historial`): **lista plana, sin
  paginar ni topar**. Crece con cada compra/renta del cliente → para un recurrente se vuelve enorme. **No
  se puede paginar en el front** (el endpoint no pagina): va al backend. Además la spec de `C8` pide
  **filtro por estado** (Por pagar / Por retirar / Activos / Cerrados) → mismo patrón Grupo B (server-side).
- **C7 Pago** (`PagoViewModel`): **reusa `miHistorial()` completo** y busca el pedido por `operacionId`.
  Se acopla al historial entero: si algún día se pagina/topa, el pago no encontraría su pedido. Convendría
  un endpoint directo «operación por id del cliente». (Backend.)
- **C9 Mis multas** (`MisDeudasRepository.mias` → `GET /clientes/me/deudas`): lista plana, pero
  **naturalmente acotada** (pocas deudas por cliente). Riesgo bajo; se deja como está.
- **C1 Explorar** (`MarketplaceRepository`): cache-first en Room + búsqueda server-side, pero **carga
  todas las empresas** (sin paginar). Al crecer el marketplace escala mal → paginación + RemoteMediator.
  Bloqueada además por identidad de tienda. (Backend.)
- **C2/C4 catálogo de tienda**: pendientes de rediseño; cuando se toquen, aplicar la misma regla (paginar
  la vitrina de disfraces/prendas server-side, no cargar todo).

### 👥 Completitud por rol — auditoría (2026-07-24, pedido de Juan)

Juan: el objetivo del rediseño es que **cada pantalla tenga TODO lo que necesita para ser usable por
dueño, cliente y empleado** (contenido/estructura, no colores). Auditoría de qué falta por rol:

- **EMPLEADO** (MOSTRADOR/ATENCION/BODEGA/ENCARGADO):
  - ✅ **Menú «Más» filtrado por rol** (`MasFragment`, H2) — **verificado en emulador** (MOSTRADOR y
    BODEGA). Cada rol ve solo lo que su puesto usa: DUEÑO/ENCARGADO = todo; **MOSTRADOR** = Rentas·
    Devoluciones·Pagos·Caja·Reembolsos·Notificaciones; **ATENCION** = Rentas·Devoluciones·Reembolsos·
    Pagos·Notificaciones; **BODEGA** = Devoluciones·Notificaciones (nada de Reportes/Empleados/Sucursales/
    Config/Auditoría, ni Rentas/Pagos/Caja que le darían 403). ⚠️ Es una **aproximación por rol**: el
    filtro **preciso**, que respete lo que el dueño concede a CADA empleado, necesita
    `GET /empleados/me/permisos` (lote de backend). Sin eso, un permiso custom por-empleado no se refleja.
  - [x] **H3** — ✅ **verificado en emulador** (empleado MOSTRADOR): cada rol tiene su **menú inferior
        propio** (`GestionShellFragment` infla el menú por rol): DUEÑO/ENCARGADO =
        Panel·Inventario·Ventas·Clientes·Más; **MOSTRADOR/ATENCION = Ventas·Rentas·Clientes·Más** (Rentas
        a un toque, antes hundido en Más); BODEGA = Inventario·Más. Aterriza en la primera pestaña del rol.
        Filtro fino por permisos por-empleado → `GET /empleados/me/permisos` (backend, anotado).
        _Nota: se creó el empleado de prueba `mostrador.h3@costumi.test` (rol Mostrador) en Railway para
        esta verificación; el dueño puede desactivarlo._
  - [ ] El empleado no ve **su propia actividad**; y no puede actuar como cliente (H1, backend).
- **CLIENTE:**
  - [~] **Foto de perfil (C10)**: maquetado el afford­ance — el avatar tiene insignia de cámara y avisa
        que llega pronto. Falta el backend (`PerfilResponse.fotoUrl` + subida). Ver lote.
  - [x] **Favoritos/Guardados**: ✅ hecho y **verificado en emulador** con **persistencia local (Room,
        tabla `favorito_disfraz`, DB v2)**. Corazón sobre la foto del disfraz (se llena al guardar),
        pantalla **«Mis guardados»** desde Perfil (lista + estado vacío), se limpia al cerrar sesión.
        **Backend anotado**: sincronizar favoritos con la cuenta entre dispositivos
        (`GET/POST/DELETE /clientes/me/favoritos`).
  - [x] **C4 Armado**: **vista previa gráfica** del disfraz armado — fila con la foto de cada pieza
        (elegidas a color, pendientes atenuadas), bajo el progreso. ✅ verificado en emulador (2026-07-24).
  - Acciones que faltan en carrito/pedidos/multas → backend (ya anotado).
- **DUEÑO:**
  - [~] **«Mi tienda» / foto**: maquetado el **dónde** — `G17 Sucursales` ahora muestra un slot de foto
        por sucursal (`item_sucursal`) y el diálogo tiene botón **«Foto de la tienda»** (avisa que llega
        pronto). Falta el backend: subir foto/portada + descripción + horario (identidad de tienda, #1).
  - Reportes sin gráficas, panel sin ingresos del día → backend (ya anotado).

**Orden de ejecución front (sin backend):** C4 vista previa → Favoritos (UI) → G17 «Mi tienda» → H3.

#### 🔑 Permisos granulares — análisis (2026-07-24, Juan preguntó por «la pantalla que falta»)

**La pantalla NO falta: existe y es alcanzable.** `Empleados → «Gestionar» → «Permisos»` abre
`PermisosEmpleadoFragment`: matriz de **12 secciones × (Ver / Operar)** con toggles, guarda por permiso.
Backend completo: `GET /empleados/{id}/permisos` + `establecer(seccion, accion, concedido)`. Secciones:
INVENTARIO, DISFRACES, VENTAS, RENTAS, DEVOLUCIONES, PAGOS, CAJA, REPORTES, CLIENTES, CONFIGURACION,
NOTIFICACIONES, EMPLEADOS. Acciones: **VER** (ver la sección) / **ACCION** (operar en ella).

**El problema real (por qué «parece que falta»): está DESCONECTADO.** La app filtra la navegación
(barra + «Más», H2/H3) por **ROL**, e **ignora la matriz de permisos**. Consecuencia: si el dueño le da a
un bodeguero «Pagos: Ver+Operar», **no cambia nada** — el filtro por rol le sigue ocultando Pagos.
Configurar permisos hoy **no afecta** lo que el empleado ve ni puede hacer.

**Bloqueo técnico:** para respetar los permisos reales, el empleado debe leer **los suyos** al entrar.
`/auth/me` da `id/email/rol/empresaId` pero **NO los permisos**; `matriz(id)` es admin (el dueño mirando a
otro) → un empleado pidiendo su propia matriz da 403. **Falta `GET /empleados/me/permisos`** (o permisos
dentro de `/auth/me`). Ya está en el lote de backend.

**Implica re-arquitectura del manejo de roles (grande):**
- El filtrado por ROL (`menuDe`/`pestanasDe`/`seccionesDe`) es un **placeholder**. El eje real es la
  **matriz de permisos**; el rol pasa a ser solo un **preset** que el dueño ajusta por persona.
- Nueva dimensión que el rol no captura: **Ver vs Operar**. No alcanza con mostrar/ocultar la sección;
  hay que **gatear los botones de acción DENTRO de cada pantalla** por `ACCION` (cobrar, crear, devolver…).
  Un empleado con solo VER debería ver la lista pero no poder operar.
- **Mapeo sección↔pantalla**: las 12 secciones del permiso no calzan 1:1 con el menú «Más» (que tiene
  Sucursales/Mensajes/Auditoría/Reembolsos, ausentes de la matriz; y la matriz tiene DISFRACES/REPORTES/
  CONFIGURACION). Hay que definir ese mapeo.
- **Plan cuando esté el endpoint:** el shell de gestión carga la matriz al entrar → un `Permisos` en
  memoria → filtra pestañas/menú por VER y habilita/oculta acciones por ACCION en cada pantalla. Reemplaza
  el filtro por rol (que queda como fallback si la matriz no carga).

**→ Esto creció a un epic completo: ver `PLAN_IDENTIDAD_PERMISOS.md`** (identidad persona=cliente +
membresías + invitación/desvinculación + multi-sucursal + permisos exhaustivos). Decisiones de Juan
2026-07-24 capturadas ahí; mayormente backend. El filtro por rol actual (H2/H3) es el interino/fallback.

### 🧱 Lote de backend del rediseño (se hace junto, al final del barrido de pantallas)

Todo lo que el rediseño necesita del backend, en un solo lugar (para no ir cambio-y-ajuste). Cada ítem
dice qué pantalla lo pide.

> **REGLA (2026-07-23, tras perder 3 ítems).** Si al rediseñar una pantalla aparece algo que el backend no
> da, **el ítem se escribe AQUÍ en el mismo momento**, no en el párrafo de esa pantalla. La ficha de la
> pantalla puede mencionarlo, pero **esta lista es la única fuente**: si no está acá, no existe. Antes de
> cerrar cada pantalla se verifica que sus «pendiente de backend» tengan su ítem en esta lista.

- **★ IDENTIDAD DE TIENDA — PRIMER FRENTE DEL BACKEND** (Juan, 2026-07-24: «ya sabes»). Desbloquea
  `C1 Explorar`, `C2 Tienda`, `G17 Sucursales`. Hoy `EmpresaVitrinaResponse` solo trae `id`+`nombre` y
  `Empresa`/`Sucursal` no guardan nada de esto. El front YA está maquetado esperando estos campos:
  - **Empresa/tienda:** `logoUrl`, `portadaUrl`, `descripcion`, `ciudad`/`barrio`, `direccion`,
    `horario` (por día) → derivar **abierto/cerrado** y «cierra 18:00», y `disfracesCount`. Endpoints
    para **subir** logo y portada (multipart, como `FotoPrendaApi`/`FotoDisfrazApi`).
  - **`EmpresaVitrinaResponse`** (lista del marketplace `C1`): sumar `logoUrl`, `ciudad`/`barrio`,
    `disfracesCount`, `abierto` (y opcional `distanciaKm` si se envía ubicación). El adapter ya los
    pinta/oculta (`item_empresa` maquetado).
  - **Sucursal (`G17`)**: foto/portada, descripción, horario y ubicación (lat/lng para mapa).
  - **`C1 Explorar` — resto del ideal:** endpoint de **destacados** para el carrusel «Para este fin de
    semana» (disfraces destacados entre tiendas, con foto/precio/tienda), y **categorías del marketplace**
    (facetas) para los chips. Ciudad del usuario (perfil o ubicación) para el saludo.
- **Foto de perfil del CLIENTE** (`C10`): subir/servir avatar del cliente (`PerfilResponse.fotoUrl` +
  endpoint multipart). Hoy la app muestra la inicial en círculo, maquetada para recibir la foto.
- **Favoritos del cliente sincronizados** (`C4`/«Mis guardados»): hoy los favoritos son **locales** (Room),
  así que no siguen al cliente entre dispositivos ni sobreviven a reinstalar. Endpoints
  `GET /clientes/me/favoritos`, `POST /clientes/me/favoritos/{disfrazId}`,
  `DELETE /clientes/me/favoritos/{disfrazId}` para persistirlos en la cuenta; la app ya tiene la UI y el
  repo listos para cablearlos.
- **Permisos propios del empleado** (menú «Más» y navegación por rol, H2): hoy el front filtra el menú
  «Más» por **rol** (oculta Reportes/Empleados/Sucursales/Configuración/Mensajes/Auditoría a quien no es
  DUEÑO/ENCARGADO, para no mostrar lo que da 403). El filtro **preciso** —que respete los overrides que el
  dueño pone por empleado— necesita un endpoint **`GET /empleados/me/permisos`** (la matriz del propio
  usuario); hoy `matriz(id)` es por-empleado y solo la ve el dueño. Con eso, la app filtra menú + acciones
  por permiso real, no por rol aproximado.
- **Depósito en el carrito**: depósito reembolsable por línea + total. Pide: `C5 Carrito`.
- **Variante (talla/color) por línea de carrito**: `SeleccionResponse` solo trae `orden`+`prendaId`.
  Pide: `C5 Carrito`.
- **Editar cantidad de una línea del carrito**: solo hay agregar/quitar, falta endpoint. Pide: `C5`.
- **Fecha de creación del carrito/línea**: no se expone. Pide: `C5 Carrito`.
- **Bandejas de rentas por estado** (pide: `G9 Rentas`).
  **Problema:** `GET /rentas` no acepta filtro por estado y la lista es paginada, así que filtrar en la
  app solo filtraría la página cargada → pestañas vacías o incompletas. Tiene que filtrar el servidor.
  **Qué implementar** (especificación completa, alcanza con esto):
  - `GET /rentas?filtro=POR_ENTREGAR|ACTIVAS|VENCIDAS|CERRADAS` (sin `filtro` = todas, como hoy).
    Semántica de cada bandeja, con `hoy` = fecha del servidor:
    - `POR_ENTREGAR` = estado `RESERVADA`.
    - `ACTIVAS` = `ACTIVA` **con `fechaDevolucion >= hoy`** + todas las `DEVUELTA` (faltan por cerrar).
      Es decir: excluye las activas ya vencidas — esas son su propia bandeja.
    - `VENCIDAS` = `ACTIVA` **con `fechaDevolucion < hoy`**.
    - `CERRADAS` = `CERRADA` + `CANCELADA` (historial).
  - **Orden:** bandejas abiertas por `fechaDevolucion` **ascendente** (lo que vence antes, arriba);
    historial (`CERRADAS`/todas) por `fechaRetiro` descendente. Desempate por `id`.
  - `GET /rentas/resumen` → `{ porEntregar, activas, vencidas, cerradas }` con el conteo de cada bandeja
    (para los números de las pestañas). Ambos endpoints aceptan `clienteId` opcional, como el listado.
  - Se resuelve en la capa de datos (un JPQL con `estados`+`soloVencidas`+`soloEnFecha`, para que la
    query no conozca el enum de bandejas). La app ya está lista: `CicloDeRenta.kt` calcula lo mismo del
    lado cliente, así que al llegar los endpoints solo hay que cablear las pestañas.
  > **NOTA (2026-07-23): ESTO ES SOLO UNA ANOTACIÓN, no código a mantener.** Me equivoqué y llegué a
  > implementarlo (rama local `feat/rentas-por-estado`, commit `02c1b75`) **saltándome la regla de que
  > primero van las pantallas**. Esa rama es descartable: **la fuente de verdad es esta especificación.**
  > Si el branch se pierde, se reimplementa desde acá. No se toca el backend hasta que Juan lo autorice.
- **Archivar una prenda que usa un disfraz** — *verificado en el backend (main), no supuesto:*
  - Catálogo del cliente y de inventario: filtran `archivada` ✔.
  - Slot **personalizable por pool** (categoría+etiquetas): `opcionesDelPool` filtra `!archivada` y el
    checkout la rechaza vía `prendaEnPool` ✔. Reactivar la devuelve sola (no hay estado extra).
  - Slot personalizable con **lista explícita** de opciones: `opcionesElegibles` resuelve por id con
    `opcionDePrenda`, **que NO filtra archivada**, y al cobrar solo se valida
    `slot.prendasOpcion().contains(...)` ✘ → se sigue ofreciendo y vendiendo una prenda archivada.
  - Slot **FIJO**: igual, y peor: `resolverPrenda` devuelve `slot.prendaFijaId()` **sin validar nada** ✘.
  - Y a la vez el precio/tipo sugerido (`prendasValuadasDeEmpresa`) **sí** filtra archivadas, así que el
    mismo disfraz se comporta de dos maneras según qué le preguntes.
  - **Raíz:** `opcionDePrenda` es la única consulta del inventario que no filtra `archivada`.
  - **DECIDIDO** (2026-07-23). La regla no es «la prenda está archivada» sino **«el disfraz tiene todas
    sus piezas disponibles»**, evaluada al vuelo. Así se arregla sola por cualquiera de los dos caminos:
    el dueño **reactiva** la prenda, **o** edita el disfraz y **cambia esa pieza** por otra. Sin estado
    nuevo que mantener ni que se pueda desincronizar.
    - **Slot FIJO** con la prenda archivada → el disfraz **no se ofrece** mientras dure.
    - **Slot personalizable**: la opción archivada desaparece de la ruleta (por pool ya pasa; falta para
      la lista explícita). Si el slot se queda **sin ninguna opción válida**, el disfraz cae en el mismo
      estado «incompleto».
    - **Qué ve cada uno** (la parte que se preguntó: ¿desaparecer o sombrear?):
      - **Cliente: desaparece.** Mostrarle sombreado algo que no puede comprar es ofrecerle un producto
        que no existe. **Excepción**: si ya lo tenía en el carrito, ahí **sí** se queda visible con el
        aviso que bloquea el checkout (patrón que ya existe en `C5`) — ahí la visibilidad sí sirve,
        porque él ya lo había elegido y tiene que poder entender y quitarlo.
      - **Dueño: se queda visible y marcado** (atenuado + motivo), nunca desaparece: si se esfumara de su
        lista no sabría por qué dejó de vender. La tarjeta dice **«No se ofrece: falta "Botas altas"
        (archivada)»** con las dos salidas a un toque: *reactivar la prenda* o *cambiar la pieza*.
      - Es el mismo criterio que ya rige para las prendas: el dueño las ve con el filtro «archivadas»,
        el cliente no.
    - **Aviso de impacto antes de archivar**: la app dice a cuántos disfraces afecta y los nombra
      (extender `ConteoDeDependenciasController` a prendas→disfraces, que ya existe para categorías y
      etiquetas).
    - **Raíz técnica a corregir**: `opcionDePrenda` debe filtrar `archivada` (o devolver el estado para
      que quien la llame decida), y `resolverPrenda` debe validar la pieza FIJA en el checkout.
    Pide: `G3`, `G5`, `G6`, `C2`, `C4`.
- **Marcar en la lista de clientes quién tiene una renta activa**: `ClienteResponse` trae
  `saldoPendiente` y `multaTotal`, pero no si el cliente tiene rentas en curso — el filtro del servidor
  sí lo sabe (`FiltroDeClientes.PENDIENTES`), pero la fila no puede mostrarlo. Pide: `G11 Clientes`.
- **«Nuevo cliente» en línea desde el punto de venta**: registrar un walk-in con nombre/teléfono/email
  sin salir de la venta (hoy hay que abandonar el ticket, crearlo en Clientes y volver a empezar).
  Verificar si `crear-cliente` acepta teléfono y email. Pide: `G7 Punto de venta`.
- **Código de retiro visible en grande tras cobrar**: al terminar la venta/renta, mostrar el código que el
  cliente debe presentar (hoy hay que ir a buscarlo a la lista). Puede necesitar que la respuesta del
  cobro lo devuelva. Pide: `G7 Punto de venta`.
- **Ingresos del DÍA + serie de 7 días + variación vs. ayer**: `/reportes/ingresos` es **acumulado
  histórico y sin rango**, así que el panel no puede mostrar «hoy» ni el gráfico de tendencia. Pide:
  `G1 Panel`.
- **Contar las rentas en estado DEVUELTA** («devoluciones por cerrar») para poder ofrecerlas como alerta
  accionable del panel. Pide: `G1 Panel`.
- **Filtros por fecha/estado y totales del período en Ventas** (`G8`): `GET /ventas` solo acepta
  `buscar` y la lista es paginada; además `VentaResponse` **no trae la fecha** de la venta. Para las
  pestañas (hoy/semana/rango, por estado) y el resumen del período hace falta que el servidor filtre y
  devuelva la fecha. (Mismo patrón que las bandejas de rentas.)
- **Resumen de cobros y saldo pendiente en Pagos** (`G12`): `OperacionPago` trae total y estado pero
  **no el saldo pendiente** por operación, ni un agregado «cobrado hoy por método»; sin eso no se puede
  mostrar el resumen ni el total pendiente que pide la spec.
- **Nombre del cliente en la solicitud de reembolso** (`G13`): `SolicitudDeReembolsoResponse` trae
  `solicitanteClienteId` pero **no el nombre**; la tarjeta querría mostrar de quién es sin abrir.
- **★ FECHAS DEL CICLO DE VIDA (transversal, pedido de Juan 2026-07-24).** Auditado modelo por modelo:
  hoy **casi ninguna pantalla puede mostrar cuándo pasó algo**, porque el backend no expone las fechas.
  Estado verificado en el `:api-client`:
  - `RentaResponse` → solo `fechaRetiro` y `fechaDevolucion` **pactadas** (planeadas). **Faltan los hitos
    REALES:** cuándo se **registró**, cuándo se **entregó** (RESERVADA→ACTIVA), cuándo **volvió de verdad**
    (la real, no la pactada) y cuándo se **cerró**. Es literalmente lo que pidió Juan («cuándo se rentó,
    cuándo se devolvió, cuándo se cerró el proceso»). Pide: `G9 Rentas`, `C8 Mis pedidos`, desglose.
  - `VentaResponse` → **ninguna fecha**. El dominio la tiene (`Instant.now()` al crear) pero no se expone.
    Sin ella `G8 Ventas` no puede mostrar cuándo se vendió ni filtrar por período. Pide: `G8`, `C8`.
  - `DevolucionResponse` → **sin fecha**. `fechaDevolucionReal` se manda en el request pero la respuesta
    no la devuelve; `G10` (lista de devoluciones) no puede fechar nada. Pide: `G10`.
  - Ya **exponen** fecha y se aprovechan: `SolicitudDeReembolsoResponse.creadaEn`/`decididaEn` (G13 ya
    muestra ambas) y `PagoResponse.fecha` (G12 ya la muestra). ✅ front al día.
  - **Decisión (Juan): mostrar lo que hay + anotar el resto.** Se dejan visibles las fechas existentes;
    los hitos que faltan se implementan cuando toque el lote. **No implementado.**
- **Usuario en el registro de auditoría** (`G21`): `AuditoriaResponse` trae acción/detalle/fecha pero
  **no quién** lo hizo; sin eso no se puede filtrar «por usuario» que pide la spec.
- **Timestamps del turno de caja** (`G14`): `TurnoResponse` no tiene hora de apertura ni de cierre, así
  que no se puede mostrar «cuánto lleva abierto» ni fechar el turno (relacionado con el ítem de fechas).
- **Reportes: gráficas de serie + rankings con foto** (`G15`): ingresos por día/período para el gráfico
  temporal (ver el ítem de fechas/ingresos del día), y `fotoUrl` en el ranking para mostrar la prenda.
- **★ PAGINAR + FILTRAR EN EL SERVIDOR — Grupo B (2026-07-24, ver §«Paginación y carga de listas»).**
  Estas tres listas tienen hoy un **filtro en memoria** sobre una lista topada en 100; para escalar,
  **el servidor tiene que filtrar y paginar** (filtrar en la app solo filtraría la página cargada). Las
  tres van juntas porque comparten patrón (mismo que `G9 Rentas ?filtro=` ya especificado arriba). No se
  tocan en el front hasta que existan estos endpoints:
  - `G21 Auditoría`: `GET /auditoria?filtro=<tipoDeAccion>` (VENTA/RENTA/PRENDA/DEVOLUCION/EMPRESA/…),
    el tipo = primera palabra de `accion`. Ya pagina (`listar19`), falta el `filtro`. (Se combina con el
    ítem «Usuario en el registro de auditoría» de abajo: idealmente también `?usuario=` y `?desde/hasta`.)
  - `G13 Reembolsos`: `GET /reembolsos?filtro=PENDIENTES|RESUELTAS` (sin `filtro` = todas). Ya pagina
    (`listar3`); hoy la app trae 100 y filtra las pestañas en memoria.
  - `G5 Disfraces`: `GET /disfraces?categoriaId=<uuid>` server-side (sin él = todos). Ya pagina
    (`listar11`); hoy la app trae 100 y filtra la categoría en memoria.
- **Nombre del destinatario en la notificación** (`G19`): `NotificacionResponse` trae `clienteId` pero
  **no el nombre**. Hoy la fila lo resuelve contra una lista de clientes topada en 100 (`repo.clientes()`),
  así que un aviso a un cliente fuera de esos 100 muestra «Cliente» genérico. Agregar `clienteNombre`
  (como ya lo traen `RentaResponse`/`VentaResponse`/reembolsos) elimina esa llamada extra y el tope.
  Los avisos internos (`clienteId == null`) ya se muestran como «Tu negocio» sin depender de esto.
- **Orden por defecto de las listas paginadas del Grupo A (2026-07-24).** Notificaciones y Devoluciones
  hoy se ordenan en la app (`fecha`/hito desc); al paginarlas (front puro, ya en curso) el orden lo debe
  garantizar el servidor. **Verificar** que `GET /notificaciones` y `GET /devoluciones` devuelvan más
  reciente primero por defecto; si no, agregar el `sort` (es el único toque de backend que necesita el
  Grupo A, y es chico). `G16 Empleados` ordena por `activo` — cosmético, no bloquea.
- **★ PAGINAR EL HISTORIAL DEL CLIENTE** (`C8 Mis pedidos`, también lo usa `C7 Pago`): hoy
  `GET /clientes/me/historial` devuelve **lista plana** (`List<HistorialItem>`, sin paginar) → para un
  cliente recurrente crece sin límite. Implementar paginación (`{contenido,totalPaginas}`, como el resto)
  **+ `?filtro=POR_PAGAR|POR_RETIRAR|ACTIVOS|CERRADOS`** que pide la spec de `C8` (mismo patrón Grupo B:
  el filtro por estado tiene que ser server-side, no en la página cargada). Orden: más reciente primero.
- **Operación por id para el cliente** (`C7 Pago`): hoy Pago trae **todo** el historial y busca el pedido
  por `operacionId`. Un `GET /clientes/me/operaciones/{id}` (o que el checkout ya devuelva total+código)
  lo desacopla del historial completo. Menor, pero se agrava cuando el historial se pagine.
- **Paginar el marketplace de tiendas** (`C1 Explorar`): `MarketplaceRepository` carga **todas** las
  empresas a Room. Al crecer el marketplace hace falta paginar la lista (server-side) + RemoteMediator.
  (Ya bloqueada por identidad de tienda; se resuelve en el mismo frente.)
- **Estado de pago del pedido** (`C8 Mis pedidos`, `C7 Pago`): `HistorialItem` no dice si el pedido está
  **pagado / con saldo**. Sin eso: (a) el filtro **«Por pagar»** de la spec no se pudo implementar (los
  otros —Por retirar/Activos/Cerrados— salen de `estado`), y (b) la acción contextual **«pagar lo
  pendiente»** tampoco. Agregar `saldoPendiente`/`estadoPago` a `HistorialItem`. («Repetir pedido» es
  front —re-armar el carrito— y se hará aparte.)
- **Pagar una multa online** (`C9 Mis multas`): hoy el `POST /pagos/intento/cliente` exige `sucursalId`
  y `MiDeudaResponse` **no lo trae** → no se puede ofrecer «Pagar ahora» desde la app (la pantalla dice
  «pagá en la tienda»). Falta: `sucursalId` en `MiDeudaResponse` **y** que el backend acepte cobrar el
  **saldo de una renta con multa** por ese endpoint (validar monto == saldo pendiente, no el total original).
- **Perfil del cliente — datos que hoy no existen** (`C10 Perfil`): la app ya reorganizó la pantalla, pero
  estos bloques necesitan backend: **foto de perfil** (subir/servir avatar; hoy es la inicial en un
  círculo), **direcciones guardadas** (CRUD por cliente), **preferencias de notificación** (por canal), y
  el **cambio de contexto** (H1: separar *persona* de *membresías* de trabajo — condiciona la navegación).
- _(se irá ampliando a medida que avanza el barrido de pantallas de gestión)_

## 🎨 Rediseño de interfaz

Auditoría completa de UI hecha el 2026-07-22. Documentos:
- **`ESPECIFICACION_UI.md`** — qué lleva cada pantalla, para cada rol: lo que hay hoy, lo que falta y la
  estructura de bloques propuesta. Es la fuente de verdad del rediseño.
- **`PROMPT_CLAUDE_DESIGN.md`** — los prompts listos para generar el sistema de diseño y las pantallas.

**Tres hallazgos que condicionan el trabajo:**

1. **Un empleado o dueño no puede comprar en ninguna tienda.** El rol y la empresa son un mismo campo
   excluyente, así que quien trabaja no puede ser cliente. Requiere separar *quién sos* de *dónde
   trabajás* (membresías) y un cambio de contexto en la app. **Condiciona toda la navegación.**
2. **El menú "Más" ofrece las 11 secciones a todos los roles.** Un empleado de bodega ve Reportes y
   Auditoría, entra, y recibe un 403. Lo que un rol no puede hacer no debe mostrarse.
3. **Las dos pantallas profesionales son las peores de la app**: armar un disfraz (dueño) y el punto de
   venta usan **desplegables por nombre, sin fotos** — mientras el cliente tiene una ruleta ilustrada.
   El dueño pasa horas ahí. Son la prioridad del rediseño.

**Medición visual:** 0 carruseles de descubrimiento, 2 colores propios, 2 estilos reutilizables, 0
animaciones, 0 imágenes con forma, 0 cabeceras colapsables — sobre 130 layouts. La app usa el tema de
fábrica de Material Design.

---

## 📋 Cola de trabajo

Orden acordado. El detalle de cada ítem está en `PLAN_PRODUCTO.md`.

### Ahora — Room / offline (ítem 7)
Procedimiento completo en `PLAN_ROOM_OFFLINE.md`. Orden interno acordado:

> **Avance 2026-07-26** (rama `feat/room-sucursales-y-tests`, aún sin mergear a `main`): se aplicaron **A3
> Sucursales**, **A1 Catálogo de prendas** y **A2 Disfraces** cache-first + se cumplieron **B1** (logout
> limpia la caché) y **B2** (base v3→v6). Se estrenaron los **tests** (SucursalRepositoryTest 2/2 +
> MarketplaceRepositoryTest 6/6 unit; SucursalDaoTest 3/3 + PrendaVitrinaDaoTest 3/3 + DisfrazVitrinaDaoTest
> 3/3 instrumentado; deps mockk + coroutines-test + room-testing). El molde por-tienda ya está probado.
>
> **A1/A2 detalle:** catálogo y disfraces cacheados **por tienda** (índice `empresaId`), con reemplazo
> por-empresa (`reemplazarDeEmpresa`) para no borrar el caché de otras tiendas al abrir una nueva (§9.1).
> Precios `BigDecimal` como texto, etiquetas como JSON, `tipo` de disfraz como texto; **los slots NO se
> cachean** (solo el conteo de piezas, el detalle se re-pide a la red). `catalogo()`/`disfraces()` viejos
> quedan intactos (usos filtrados de RentaForm/VentaPos siguen server-side). `TiendaViewModel` observa Room
> (prendas y disfraces) y refresca sin tapar la caché.

- [x] **B1** Borrado de la caché al cerrar sesión (norma de **seguridad**) — hecho (empresa/favoritos/sucursal/prendas/disfraces)
- [x] **B2** Subir `version` de `CostumiDatabase` — hecho (v6 con Prenda/DisfrazVitrinaEntity)
- [x] **A3** Sucursales por tienda — **HECHO** (entidad/DAO/repo observar-refrescar/VM observa/logout/tests)
- [x] **A1** Catálogo de prendas por tienda — **HECHO** (índice+reemplazo por empresa, precios/etiquetas, VM cache-first, tests)
- [x] **A2** Disfraces por tienda — **HECHO** (mismo molde; se cachea el conteo de piezas, no los slots)
- [ ] **A4** Mis pedidos (el mejor candidato: es historial, ya no cambia)
- [ ] **A5** Mis multas
- [ ] **A6** Mi perfil y mi tienda (elimina la caché en memoria de `MiEmpresaRepository`)
- [ ] **B3** Indicador de "sin conexión / datos guardados"
- [~] **B4** Tests de DAOs y repositorios con caché — SucursalDao/Repository + Prenda/DisfrazVitrinaDao + MarketplaceRepository (6 unit); se suma por tabla
- [ ] **C** Inventario con Paging 3 + RemoteMediator _(rama aparte, al final)_

### Después — Bloqueantes para publicar (ítems 1-4)
- [ ] **3** Crashlytics ⚠️ _conviene **antes** de tener usuarios reales_
- [ ] **1** Firma de release (keystore fuera del repo)
- [ ] **2** Ofuscación R8 + `shrinkResources` _(probar el APK de release a mano)_
- [ ] **4** Estrategia de versionado

### Luego — Calidad (ítems 5-6)
- [ ] **5** Tests de carrito, checkout, cobros y sesión
- [ ] **6** Extraer 259 textos a `strings.xml`

### Más adelante — Producto comercial (ítems 8-12)
- [ ] **8** Actualización forzada (kill switch) _— implica endpoint en el backend_
- [ ] **9** Política de privacidad + declaración de datos en Play Console
- [ ] **10** Pantalla de sesión expirada (refresh fallido)
- [ ] **11** Reintentos con espera _(solo GET; nunca sobre checkout)_
- [ ] **12** Biometría para entrar

### Opcionales (ítems 13-16)
- [ ] **13** Onboarding
- [ ] **14** Layouts para tablet
- [ ] **15** Buscador global en gestión
- [ ] **16** Widget y accesos directos

---

## ✅ Hecho

### Auditoría de arquitectura (2026-07-22)
Medida sobre el código, no estimada:

| Control | Resultado |
|---|---|
| Patrón | **MVVM** — 53 ViewModels, 28 Repositories, 55 Fragments, 1 Activity |
| Estado reactivo | **StateFlow** en 60 archivos, **cero** LiveData |
| Adapters de RecyclerView | 37 |
| Fugas de binding | **0** — 55/55 liberan en `onDestroyView` |
| `viewModelScope` | **53/53** ViewModels |
| `GlobalScope` / `runBlocking` / `Thread.sleep` | **0 / 0 / 0** |
| Ciclo de vida | `observar()` centralizado con `repeatOnLifecycle(STARTED)`, 145 usos; **cero** `collect` crudo |
| Dispatchers | `DispatcherProvider` **inyectable** (testeable); 28 repos en `io` |
| Seguridad base | Tokens en `EncryptedSharedPreferences`, sin tráfico en claro, logs de red solo en debug |
| UX | 64 pantallas con estados, 84 confirmaciones destructivas, modo oscuro, swipe-to-refresh |

**Conclusión:** la base está sólida. Lo que falta es completar lo de alrededor, no rehacer lo hecho.

**Detalles menores detectados** (no bugs, pendientes de limpiar cuando se toque esa zona):
- 4 Fragments hacen `withContext(Dispatchers.IO)` directo en vez de usar el `DispatcherProvider`:
  `ConfiguracionFragment` (×2), `DisfrazFormFragment`, `PrendaFormFragment`. Leen archivos; funcionan,
  pero ponen I/O en la vista y no son testeables.
- `ServicioDeMensajeria` usa su propio scope con `Dispatchers.IO` — correcto para un `Service`, aunque
  también evita el provider.

### Decisiones de arquitectura tomadas
- **Sin capa `domain`.** Los ViewModels llaman al repositorio directo. Las reglas de negocio viven en el
  backend (hexagonal); en la app los casos de uso solo reenviarían llamadas (~50 archivos vacíos de
  contenido). **Se creará solo si aparece un ViewModel que combine varios repositorios** — el Dashboard
  es el único candidato hoy.
- **Room es caché, no fuente de datos propia.** El servidor es la verdad; por eso las migraciones pueden
  ser destructivas (ver norma N7 del plan de Room).
- **No se cachea lo que decide** (stock, saldos, disponibilidad): solo lo que describe. Ver §1 del plan
  de Room.
- **RemoteMediator solo en Inventario**, no en Ventas ni Rentas: en operaciones de dinero, un dato viejo
  provoca doble reserva y errores de cobro.

### Correcciones funcionales
Ver `PENDIENTE_FRONTEND.md` — auditoría de 6 hallazgos (FRONT-1..6) y problemas de operación
(OP-1..OP-8), todos cerrados y verificados contra Railway.

---

## Registro de sesiones

- **2026-07-26** — Tanda de cierre para presentación. **En `main`** (commit `21f6d47`): notificaciones
  (toggle activar/desactivar en Perfil + canal + foreground), **pago con tarjeta SIMULADO** (panel local, no
  toca backend), **blindaje de los 6 pickers de foto** (PickVisualMedia + lectura segura → no crashea al
  volver del selector), invitar/reenviar "email-first" + manejo del 409, **firma de release** (keystore
  fuera del repo) + **AAB firmado** para Play, y `AUDITORIA_MVVM.md`. **En rama `feat/room-sucursales-y-tests`**:
  Room A3 Sucursales cache-first + primeros tests (verdes). **Backend** (PRs sin mergear): email por HTTP
  (Brevo, ya desplegado), texto del correo de invitación, y fallback del router a FCM. **Play Store**: cuenta
  creada, **esperando verificación de identidad de Google** (bloqueo para publicar). Ver memoria
  `estado-actual-play-room-tests`.
- **2026-07-22** — Auditoría completa de arquitectura (MVVM, lifecycles, dispatchers, Room). Se detectó
  que Room quedó en una sola tabla pese a que el diseño pedía cache-first (`ORDEN_CONSTRUCCION.md` Fase 8).
  Se decidió: no agregar capa `domain`, ejecutar el plan de Room completo + RemoteMediator solo en
  Inventario. Se escribieron `PLAN_ROOM_OFFLINE.md`, `PLAN_PRODUCTO.md` y este documento.
