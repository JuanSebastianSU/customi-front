# Costumi — Plan de Identidad, Membresías y Permisos

> **Qué es este documento.** El plan para separar **quién sos** (persona = siempre cliente) de **dónde y
> cómo trabajás** (una membresía con rol, permisos y sucursales). Reemplaza el enfoque «por rol» que se
> usa hoy en la navegación. Es **mayormente backend** (modelo + endpoints) con cambios de app.
>
> **Origen.** Surge de la revisión de permisos granulares (2026-07-24): la pantalla de permisos existe y
> es completa, pero **está desconectada** — la app filtra por rol e ignora la matriz. Ver el análisis en
> `PROGRESS.md` → «🔑 Permisos granulares». Este plan es cómo se conecta y se hace bien.
>
> **Estado:** PLAN. No implementado. No se toca código hasta acordar el modelo.

---

## Decisiones tomadas (Juan, 2026-07-24)

1. **Persona = siempre cliente.** Cualquiera, sea lo que sea que llegue a ser (empleado, encargado, dueño),
   **sigue siendo cliente**: puede explorar tiendas y hacer pedidos. Su cuenta es una sola.
2. **Una membresía de trabajo a la vez.** **No se puede trabajar para dos tiendas distintas** (franquicias
   distintas) — es riesgo de seguridad. Para cambiar de empleador: **primero desvincularse** de la actual,
   y recién ahí puede aceptar la invitación de otra tienda (en la sucursal que le toque). Dentro de UNA
   misma tienda sí puede tener varias sucursales.
3. **Invitación por email a cualquier persona.** Al aceptar acepta **términos y condiciones**. Si la
   persona no tiene cuenta todavía, se crea al aceptar. Si ya es cliente, se le **suma** la membresía a su
   cuenta (no se crea una nueva).
4. **Re-auth al entrar a modo trabajo:** una vez por sesión (contraseña, o **biometría** si está
   disponible/es fácil de habilitar). No en cada acción. Idealmente configurable por la tienda.
5. **Desvinculación de dos vías:**
   - **Dueño deshabilita** (suspensión/despido): inmediato, corta el acceso al toque, **reversible por el
     dueño** (re-activar). La persona queda como solo-cliente.
   - **El empleado se va** (botón «Desvincularme de …»): vuelve a solo-cliente, pero para **volver** hace
     falta **re-invitación del dueño + aceptación del empleado** (salir fue decisión suya).
6. **Pirámide de permisos.** Cada quien edita solo a los que están **debajo** suyo, **nunca arriba**. El
   dueño puede todo **menos** lo que es jurisdicción del **superadmin**. Un encargado no ve ni toca los
   permisos del dueño.
7. **Multi-sucursal.** El dueño asigna sucursales (al encargado, varias). Los demás roles quedan ligados a
   su(s) sucursal(es). Hay **sucursal activa** visible. El encargado reasigna empleados **según lo que el
   dueño le conceda** (por eso el permiso debe ser explícito). Historial por sucursal, misma cuenta.
8. **★ El control granular debe ser EXHAUSTIVO y CLARÍSIMO.** Cada permiso dice **qué habilita y qué
   consecuencia** tiene; no queda a la duda. Incluye no solo *Ver/Operar por sección* sino las
   **capacidades de gestión** (reasignar empleados, editar permisos de otros, gestionar sucursales,
   invitar/dar de baja). Esto **expande** el modelo actual y exige **rediseñar** la pantalla de permisos.

9. **★★ PRINCIPIO RECTOR (Juan, 2026-07-24, repetido): TODO es un permiso configurable. El rol es solo el
   PRESET de defaults; no hay reglas "por rol" fijas.** «¿Quién puede invitar / dar de baja / reasignar /
   editar permisos / ver Auditoría / etc.?» → **depende de lo que el dueño (o quien tenga el permiso, en
   pirámide) le conceda a cada persona.** «No puedo saber en quién confía cada uno para qué en la vida
   real» → por eso **cada capacidad es un toggle**, con su default según el preset del rol (p. ej. un
   encargado, por defecto, **sí** puede gestionar personal; el dueño puede quitárselo). **TODAS** las
   secciones —incluidas Sucursales, Mensajes automáticos, Auditoría y Reembolsos— entran como permisos.
10. **Re-auth: configurable por tienda.**

---

## 1. El modelo

### 1.1 Persona / Cuenta (identidad)
- Una cuenta = una persona (email + credenciales). **Siempre tiene faceta cliente.**
- Reemplaza el modelo actual donde `rol` y `empresa` son **un mismo campo excluyente** (por eso hoy un
  empleado no puede comprar — H1).

### 1.2 Membresía (0 o 1 activa)
- Vincula una **persona** con una **tienda (empresa)**, con: **rol** (preset de permisos), **permisos**
  efectivos (matriz), **sucursales asignadas**, **estado** y **motivo de baja**.
- **Máximo una membresía activa** por persona (regla de seguridad #2).
- Estados: `INVITADA` (pendiente de aceptar) · `ACTIVA` · `SUSPENDIDA` (dueño la deshabilitó, reversible
  por él) · `BAJA_EMPLEADO` (el empleado se fue; para volver, re-invitación) · `BAJA_DUENO` (despido/quita).

### 1.3 Permisos (matriz + capacidades)
- Base actual: **12 secciones × (VER / ACCION)** — INVENTARIO, DISFRACES, VENTAS, RENTAS, DEVOLUCIONES,
  PAGOS, CAJA, REPORTES, CLIENTES, CONFIGURACION, NOTIFICACIONES, EMPLEADOS.
- **Falta cubrir** (para que sea exhaustivo, decisión #8): capacidades de **gestión** que hoy no son
  permisos explícitos, p. ej.:
  - Gestión de personal: **invitar**, **dar de baja**, **cambiar rol**, **editar permisos**, **asignar/
    reasignar sucursales**.
  - Identidad de tienda: editar datos/foto/horario (cuando exista, ver identidad de tienda).
  - Secciones que están en el menú pero no en la matriz: **Sucursales, Mensajes automáticos, Auditoría,
    Reembolsos** → decidir si entran como secciones nuevas o quedan bajo otra.
- **Preset por rol** = valores iniciales (los que definimos en H2/H3). El dueño los ajusta por persona; la
  **matriz efectiva** es la verdad, no el rol.

### 1.4 Sucursal
- El empleado tiene **sucursal(es) asignada(s)** y una **sucursal activa** (contexto de operación).
- Una sola sucursal → activa fija/implícita. Varias → **selector** de sucursal activa.
- **Alcance de datos por sucursal activa**: un mostrador de la sucursal A no ve ventas/rentas/caja de la B
  → lo **scopea el backend** según la sucursal activa (no la app).

---

## 2. Flujos

- **Invitar → aceptar:** dueño (o quien tenga el permiso) invita por email → la persona recibe la
  invitación → acepta T&C → membresía `ACTIVA`. Si ya trabaja en otra tienda, **se rechaza** con motivo
  claro («ya trabajás en X; salí primero»).
- **Desvincular (dos vías):** ver decisión #5. Botón del empleado «Desvincularme de …»; acción del dueño
  «Suspender / Quitar». Ambos → solo-cliente; la diferencia es cómo se **vuelve**.
- **Cambio de contexto (H1):** en la app, un switch **«Comprando» ↔ «Trabajando en <Tienda>»**. Entrar a
  modo trabajo pide **re-auth** (#4).
- **Mono → multi sucursal (★ planear bien):** cuando la tienda pasa de 1 a 2+ sucursales, los empleados
  que estaban implícitos en «la única» necesitan **asignación explícita**. Regla base a definir: al crear
  la 2.ª sucursal, todos quedan en la original y el dueño reasigna; aparece el selector de sucursal activa
  para quien tenga varias. **Caso inverso** (multi → mono) también hay que contemplarlo.
- **Reasignación de sucursal:** el encargado mueve empleados **solo entre SUS sucursales** (si tiene el
  permiso). El dueño, a cualquiera.

---

## 3. Qué necesita el BACKEND

- **`GET /empleados/me/permisos`** (o permisos dentro de `/auth/me`) — **crítico**: sin esto la app no
  puede filtrar por permisos reales (hoy `matriz(id)` es admin → 403 para el propio empleado).
- **Modelo de membresía** persona↔tienda con estado + motivo (§1.2), y la **regla de una-membresía-activa**.
- **Invitación + aceptación**: endpoint de invitar (por email), token/estado `INVITADA`, aceptación con
  **T&C**, creación de cuenta si no existe.
- **Alta = invitación** (no crear cuenta suelta): reemplaza el actual `AltaDeEmpleadoRequest(rol,email,pass)`
  que **crea una cuenta nueva**.
- **Sucursal al invitar** + reasignación validando la **pirámide** y las sucursales del que reasigna.
- **Alcance de datos por sucursal activa** (§1.4) — filtra ventas/rentas/caja/etc. server-side.
- **Modelo de permisos expandido** con las **capacidades de gestión** (§1.3) y su validación piramidal.
- **Re-auth / step-up** (si se hace configurable por tienda): flag de configuración.
- Separar `rol`/`empresa` del identity (H1): que el token/`/auth/me` exprese **persona + membresía activa**.

## 4. Qué necesita la APP

- **Cambio de contexto** «Comprando ↔ Trabajando en X» (H1) + **re-auth/biometría** al entrar a trabajo.
- **Navegación por permisos** (reemplaza el filtro por rol): al entrar a modo trabajo, cargar la matriz →
  **mostrar secciones por VER** y **habilitar/ocultar botones de acción por ACCION** dentro de cada
  pantalla. El filtro por rol queda de **fallback**.
- **Pantalla de permisos rediseñada** (decisión #8): exhaustiva, agrupada, con **texto claro de qué
  habilita y qué consecuencia** cada permiso; respeta la **pirámide** (solo muestra/edita a los de abajo).
- **Invitaciones**: recibir/aceptar (con T&C) y **desvincularse** (botón del empleado).
- **Selector de sucursal activa** (solo si hay varias) + sucursal activa siempre visible (patrón #9).
- El alta de empleado pasa a ser **«Invitar»**, no «crear cuenta».

## 5. Riesgos / casos difíciles
- **mono ↔ multi sucursal**: transición y defaults (§2). El más delicado.
- **Despido inmediato** vs baja amistosa: el «suspender» del dueño debe cortar acceso **al instante**.
- **Una-membresía**: manejo claro del rechazo cuando ya trabaja en otra tienda.
- **Persona con carrito/pedidos que se vuelve empleado**: la identidad unificada lo resuelve (misma cuenta).
- **Pirámide en el front**: no ofrecer editar lo que el backend va a rechazar.

## 6. Preguntas abiertas (para cerrar antes de ejecutar)

**Cerradas (Juan, 2026-07-24):**
- ✅ **Invitar/dar de baja/reasignar/editar permisos** = **permisos configurables**, con default según el
  preset del rol (encargado: default sí). Ver principio #9.
- ✅ **Secciones fuera de la matriz** (Sucursales/Mensajes/Auditoría/Reembolsos): **sí, entran como
  permisos.** La matriz cubre TODO.
- ✅ **Re-auth: configurable por tienda.** Biometría = equivalente a re-auth (si se puede habilitar fácil).

**Propuesta a aprobar — mono → multi sucursal (Juan no sabía qué responder):**
> Regla base propuesta: al crear la **2.ª sucursal**, **todos los empleados existentes quedan asignados a
> la sucursal original** (la que ya existía) y aparece el **selector de sucursal activa** para quien tenga
> varias. El dueño (o el encargado con permiso) **reasigna** desde ahí. **Nadie queda sin sucursal** (para
> no dejar el alcance de datos ambiguo). Al **volver a mono** (borrar/archivar sucursales hasta quedar 1),
> todos caen a la única y el selector desaparece.
> *Motivo:* es el default menos sorpresivo (nada se mueve solo) y seguro (sin datos sin scope). **Falta que
> Juan lo apruebe o ajuste.**

**Sigue abierta:**
- **Capacidades de gestión exactas** a modelar como permisos (lista final): p. ej. Personal → {invitar,
  dar de baja, cambiar rol, editar permisos, asignar sucursales}; Sucursales → {crear, editar, archivar};
  etc. Se define al diseñar la matriz expandida.

## 7. Orden sugerido de ejecución
1. **Backend `GET /empleados/me/permisos`** + navegación por permisos en la app (desbloquea todo; convierte
   el filtro por rol en fallback). — *el paso de mayor impacto y menor alcance.*
2. Modelo de **membresía** + separación identidad/empresa (H1) + **cambio de contexto** en la app.
3. **Invitación/aceptación** + **alta = invitar** + desvinculación de dos vías.
4. **Multi-sucursal**: asignación al invitar, sucursal activa, alcance de datos, reasignación piramidal.
5. **Permisos expandidos** (capacidades de gestión) + **rediseño de la pantalla de permisos**.
6. **Re-auth / biometría** al entrar a modo trabajo.

> Cada punto es grande; se hace uno por rama/PR, verificando en emulador y contra Railway, como el resto.
