# Costumi — Especificación de interfaz por pantalla y por rol

> **Qué es este documento.** El inventario completo de la app: qué hace cada pantalla hoy, qué le falta y
> qué bloques debería tener, para cada rol. Es la base del rediseño y la fuente del prompt de diseño.
>
> **Método.** Todo lo que dice "hoy" está verificado en el código, no supuesto.
>
> **Cómo se lee cada ficha:** *Objetivo* (qué viene a hacer el usuario) → *Hoy* → *Falta* → *Estructura*
> (bloques de arriba hacia abajo, con la acción principal siempre al final = al alcance del pulgar).

---

## Parte I — Los roles

| Rol | Quién es | Dónde opera |
|---|---|---|
| **CLIENTE** | Persona que alquila o compra | Todas las tiendas del marketplace |
| **MOSTRADOR** | Atiende en el local | Solo sus sucursales asignadas |
| **ATENCION** | Atiende clientes y postventa | Solo sus sucursales |
| **BODEGA** | Maneja inventario y stock | Solo sus sucursales |
| **ENCARGADO** | Jefe de sucursal | Su empresa |
| **DUENO** | Dueño de la tienda | Su empresa completa |
| **SUPERADMIN** | Plataforma | Todas las empresas |

### Hallazgos críticos de roles

**H1 — Un empleado no puede comprar en ninguna tienda.**
En el modelo, el rol y la empresa son un mismo campo excluyente: quien tiene empresa es *personal*, quien
no la tiene es *cliente*. Cuando el carrito recibe un token con empresa, asume que estás vendiéndole a
alguien, nunca que estás comprando.

Pero en la vida real un empleado **también es una persona**: puede querer alquilar un disfraz en otra
tienda con su propia cuenta. Y un dueño, igual.

> **Lo correcto:** separar *quién sos* (persona, siempre cliente) de *dónde trabajás* (una o más
> membresías con rol y sucursales). La app tendría un cambio de contexto explícito: **"Comprando"** ↔
> **"Trabajando en Disfraces El Carrusel"**. Es la decisión que condiciona toda la navegación.

**H2 — El menú "Más" ofrece a todos las mismas 11 secciones.**
`MasFragment` lista Rentas, Devoluciones, Pagos, Reembolsos, Reportes, Empleados, Sucursales,
Configuración, Notificaciones, Mensajes y Auditoría **sin filtrar por rol**. Un empleado de bodega ve
"Reportes" y "Auditoría", entra, y recibe un error del servidor.
> **Regla:** lo que un rol no puede hacer **no se muestra**. Un permiso denegado nunca debe descubrirse
> tocando.

**H3 — La barra inferior sí filtra, pero deja roles casi vacíos.**
BODEGA ve solo *Inventario* y *Más*; MOSTRADOR ve *Ventas*, *Clientes* y *Más*. Sus pantallas más usadas
(registrar una venta, revisar stock) quedan a dos toques de profundidad.

---

## Parte II — Cliente

### C1 · Explorar — 🟡 tarjeta rediseñada (maquetada); resto del ideal pendiente de backend
- **Objetivo:** encontrar una tienda o un disfraz que sirva para la ocasión.
- **Antes:** lista vertical pobre; cada tienda un círculo con la inicial + "Ver catálogo".
- **Ahora:** tarjeta de tienda rediseñada — **logo** (o inicial grande), nombre, **ciudad/barrio**,
  **pastilla abierto/cerrado**, y «Ver catálogo ›»; encabezado de bienvenida. Los campos que el backend
  aún no da (logo, ciudad, horario, nº disfraces) están **maquetados y ocultos hasta que lleguen**
  (`item_empresa` + `EmpresaAdapter`), según la regla «front ideal → backend cumple».
- **Falta (backend — ver «Identidad de tienda» en `PROGRESS.md`):** los campos de la vitrina
  (logoUrl, ciudad, abierto, disfracesCount, distancia), el **carrusel de destacados** «Para este fin de
  semana» y las **categorías del marketplace** para los chips, y la **ciudad del usuario** para el saludo.
- **Estructura:**
  1. Saludo + ciudad/ubicación.
  2. Buscador (busca disfraz, tienda **y** categoría, no solo tienda).
  3. Carrusel **"Para este fin de semana"** — disfraces destacados con foto y precio.
  4. Fila de categorías (Piratas, Época, Terror…) como chips desplazables.
  5. Lista de tiendas: **logo, barrio, distancia, si está abierta, cuántos disfraces tiene**.
  6. *Sin acción principal*: esta pantalla es de descubrimiento.

### C2 · Tienda (catálogo)
- **Objetivo:** ver qué ofrece esta tienda.
- **Hoy:** lista de prendas y disfraces con filtro por categoría.
- **Falta:** **portada, logo, descripción, dirección y horario de la tienda** (hoy el backend ni siquiera
  los guarda: hay que agregarlos). Y separar visualmente disfraces de prendas sueltas.
- **Estructura:**
  1. Portada con logo, nombre, barrio, horario y estado (abierto/cerrado).
  2. Descripción corta + botón de ubicación.
  3. Pestañas: **Disfraces** | **Prendas sueltas**.
  4. Buscador + chips de categoría.
  5. Grilla de 2 columnas con foto, nombre, precio y si está disponible.

### C3 · Detalle de prenda
- **Objetivo:** decidir si la alquila o la compra, y agregarla.
- **Hoy:** foto, nombre, precio, selector renta/venta, fechas, cantidad, sucursal, botón agregar.
  Funciona y está completo.
- **Falta:** galería de más de una foto, talla/color visibles como atributos, y "otras prendas de la
  tienda".
- **Estructura:**
  1. Galería (carrusel de fotos con indicadores).
  2. Nombre + categoría + estado de disponibilidad.
  3. Selector **Rentar | Comprar** (solo lo que la prenda permita).
  4. Fechas (si es renta) y cantidad.
  5. Sucursal de retiro.
  6. **Barra fija abajo:** precio calculado + botón *Agregar al carrito*.

### C4 · Detalle de disfraz — armado ⭐
- **Objetivo:** armar su disfraz eligiendo cada pieza. **Es el corazón del producto.**
- **Hecho (rediseño):** lista de piezas con una fila por pieza (miniatura de lo elegido, o *Elegir ›*);
  progreso *"3 de 4 piezas"* + barra (las opcionales no cuentan); hoja de selección con grilla de 2
  columnas (foto grande, precio, stock real, buscador, marca de elegida, sin-stock atenuado); las
  opciones muestran sus **etiquetas con nombre** ("Talla M · Blanco") para distinguir prendas que solo
  difieren en talla/color; barra fija con total real en vivo y botón que nombra lo que falta.
- **Hecho (rediseño, cont.):** **vista previa gráfica del disfraz armado** — una fila con la foto de cada
  pieza (elegidas a color, pendientes atenuadas), bajo el progreso; aparece al elegir la primera pieza.
  Verificado en emulador (2026-07-24). El filtro por facetas de la hoja también está hecho (ver C4.1).
- **Estructura:**
  1. Galería del disfraz.
  2. Nombre, tienda, tipo (renta/venta).
  3. **"Tu disfraz · 3 de 4 piezas"** — lista de piezas: las elegidas con miniatura y check, las
     pendientes con *Elegir ›*.
  4. Al tocar una pieza: hoja con la ruleta de opciones + **filtro por facetas** (ver abajo).
  5. Fechas, cantidad, sucursal.
  6. **Barra fija abajo:** precio total real (por días) + botón que nombra **lo que falta**
     (*"Elegir botas"*), o *"Agregar al carrito"* cuando está completo.

#### C4.1 · Filtro por facetas en la hoja de selección de pieza
- **Problema:** un slot puede resolver a cientos de opciones; obligar a scrollear (o solo buscar por
  texto) es malo. El cliente debe poder acotar por las etiquetas que las prendas de ESE slot realmente
  tienen. Implementa RF-2.7.2 (`seleccionablePorCliente`), hoy guardado y sin usar.
- **De dónde salen las facetas:** las calcula el **mismo endpoint de la ruleta** (no una llamada
  aparte): la respuesta pasa de solo `opciones` a `opciones + facetas`. Cada faceta =
  `{ tipoEtiquetaId, tipoNombre, valores: [ { valorEtiquetaId, valorNombre, cantidad } ] }`.
- **Qué dimensiones se ofrecen:** solo los tipos marcados `seleccionablePorCliente` **y** presentes en
  el conjunto (unión de las etiquetas de las opciones). Si en ese slot ninguna prenda lleva "Material",
  no aparece "Material". Cada slot ofrece sus propias dimensiones.
- **Semántica del filtro:** **OR dentro de una dimensión, AND entre dimensiones**
  — `(Talla ∈ {M,L}) Y (Color ∈ {Blanco})`. El backend agrupa los `valores` recibidos por su tipo. El
  parámetro sigue siendo una lista plana de ids (compatible hacia atrás). *(Reemplaza el `containsAll`
  actual, que era AND estricto y no permitía "M o L".)*
- **Conteos dinámicos:** el conteo de cada dimensión se recalcula aplicando los filtros de **las otras**
  dimensiones (estilo e-commerce). Nunca deja un "callejón sin salida" (chip con conteo > 0 que da
  grilla vacía).
- **Estructura de la hoja:**
  1. Título de la pieza + "N opciones · elegí una".
  2. Fila de chips de filtro por dimensión (reusa el `Chip` existente); togglear repinta grilla +
     conteos. *Limpiar filtros* cuando hay alguno activo.
  3. Buscador por texto (ya existe).
  4. Grilla de opciones.
  5. Estado vacío accionable: *"Ninguna combinación disponible — quitá un filtro"*.

### C5 · Carrito y C6 · Mis carritos
- **Hoy:** el carrito lista líneas con precio y subtotal; *Mis carritos* muestra los abiertos por tienda.
- **Falta:** en el carrito, ver el período de renta por línea (**ya implementado**) y poder editar
  cantidad sin borrar y volver a agregar.
- **Estructura del carrito:**
  1. Cabecera con tienda y sucursal de retiro.
  2. Líneas con foto, nombre, período (si es renta), cantidad editable y subtotal.
  3. Avisos por línea si algo dejó de estar disponible.
  4. **Barra fija:** total + *Confirmar pedido*.

### C7 · Pago — ✅ rediseñada
- **Objetivo:** pagar y obtener el código de retiro.
- **Antes:** resumen, monto y código en tamaño medio, mezclado con el bloque de instrucciones.
- **Ahora:** el **código de retiro es el héroe** — centrado, grande (display), **monoespaciado** para
  dictarlo/leerlo sin error; método de pago con rótulo claro («Elegí cómo pagar» → *Pagar con tarjeta
  ahora* / *Pagar en la tienda (efectivo)*), y el aviso de las 24 h separado. Cambio solo de layout.
- **Nota:** C7 toma el total del historial del cliente (ver lote de backend: «Operación por id para el
  cliente», que hoy obliga a traer todo el historial).

### C8 · Mis pedidos — ✅ rediseñada (fila + filtro); «Por pagar» y paginación pendientes de backend
- **Antes:** filas con tipo/estado/monto en texto plano y fecha ISO.
- **Ahora:** **chips de filtro por estado** (Todos · Por retirar · Activos · Cerrados), **pastilla de
  estado** por color, fecha legible (`comoDiaMes`), monto y artículos (piezas agrupadas por disfraz).
  Acción de reembolso a mano.
- **Pendiente (backend):** el filtro **«Por pagar»** y la acción **«pagar lo pendiente»** necesitan
  `saldoPendiente`/`estadoPago` en `HistorialItem`; y **paginar `GET /clientes/me/historial` + `?filtro=`**
  (hoy el filtro corre en la app sobre la lista completa — patrón Grupo B). Ver lote de backend.

### C9 · Mis multas — ✅ rediseñada; «Pagar ahora» online pendiente de backend
- **Ahora:** **cabecera destacada** con el total a pagar (contenedor de error) y el aviso de pagar en la
  tienda; una tarjeta por deuda con **pastilla Pendiente/Pagado** y el desglose que explica el cargo.
- **Pendiente (backend):** el pago **online** de la multa — hoy `POST /pagos/intento/cliente` exige
  `sucursalId` que `MiDeudaResponse` no trae, y falta que el backend acepte cobrar el saldo de una renta
  con multa. Mientras tanto la pantalla dice **«pagá en la tienda mostrando tu código»** (honesto, sin
  botón muerto). Ver lote de backend.

### C10 · Perfil — ✅ rediseñada (layout); foto/direcciones/preferencias/contexto pendientes de backend
- **Ahora:** cabecera con **avatar de inicial** + nombre + correo; secciones **Tus datos** (nombre/teléfono
  + guardar) y **Cuenta** (multas, cambiar contraseña, registrar tienda); **cerrar sesión separado** por un
  divisor y en tono de alerta.
- **Pendiente (backend):** foto de perfil, direcciones guardadas, preferencias de notificación y el
  **cambio de contexto** (H1). Ver lote de backend.

---

## Parte III — Gestión

### G1 · Panel (DUENO, ENCARGADO) — ✅ rediseñado; gráfico e ingresos del día pendientes de backend
- **Objetivo:** saber en 5 segundos cómo va el negocio y qué requiere atención.
- **Antes:** ocho tarjetas de números del mismo tamaño (la mitad, ceros perpetuos) y un único aviso de
  stock bajo arriba. Nada decía qué hacer hoy.
- **Ahora:**
  1. Nombre de tienda + fecha de hoy.
  2. **Requiere atención**: una fila por alerta real — *rentas vencidas* (rojo, con cuánto se pasó la
     más atrasada), *variantes con stock bajo*, *reembolsos por responder* — y cada una **lleva a la
     lista que la resuelve**. Sin alertas dice *«Todo al dia»*. Se recalcula al volver al panel.
  3. **Ingresos acumulados** (nombrados así a propósito: `/reportes/ingresos` es histórico).
  4. Inventario: arriba lo que se mira a diario (*Disponibles para alquilar* · *Rentadas ahora*), luego
     valor y utilización, y una línea final que nombra **solo lo que no es cero** («357 unidades en
     total · fuera de circulacion: 2 danadas»).
  5. **Barra fija:** *Nueva venta* (la acción más frecuente del día).
- **Pendiente (lote de backend):** ingresos **del día**, **serie de 7 días** y variación vs. ayer;
  alerta de *devoluciones por cerrar* (no hay forma de contar las rentas en estado DEVUELTA).

### G2 · Inventario (DUENO, ENCARGADO, BODEGA)
- **Hoy:** lista paginada con buscador y filtro por categoría.
- **Falta:** ver stock por sucursal de un vistazo, y filtro por "stock bajo".
- **Estructura:** buscador + chips (categoría, stock bajo, archivadas) → filas con foto, nombre, tipo,
  precio y **stock por sucursal** → **FAB:** *Nueva prenda*.

### G3 · Ficha de prenda — ✅ rediseñada
- **Antes:** una columna de nueve campos, con la **foto al final** y los **cinco precios siempre
  visibles** (te pedía «precio de renta por día» de una prenda que solo se vende), y dos campos de multas
  sin explicar para qué servían.
- **Ahora:** foto arriba → **Datos** (nombre, categoría) → **«Se puede...»** *Rentar · Vender · Las dos*
  de un toque, que **decide qué precios se piden** → **Precios** (el costo de adquisición aclara que es
  interno) → **«Si vuelve mal o no vuelve»** con la explicación de que esos valores son los que la app
  propone cobrar en la devolución → **Etiquetas**, diciendo que con ellas filtra el cliente →
  **barra fija:** *Guardar*.
- En edición, categoría y tipo se ven pero no se tocan (el backend no los deja cambiar) y se dice por qué:
  el **tipo** valida los precios y la **categoría** define qué etiquetas valen y en qué pools de disfraz
  entra la prenda; cambiarlos rompería el historial y movería disfraces en silencio.
- **Pendiente (necesita backend): aviso de impacto al archivar.** Antes de confirmar, decir a cuántos
  disfraces afecta y nombrarlos (ya existe ese conteo para categorías y etiquetas).

### G4 · Stock por prenda — ✅ rediseñada (front)
- **Hoy:** grupos de stock con mover, reabastecer, transferir, ajustar, eliminar.
- **Falta:** que se entienda qué es un "grupo de stock" — el concepto no se explica en ningún lado.
- **Estructura:** resumen por sucursal → tarjetas de grupo con combinación (talla/color), disponible,
  rentado, dañado → acciones por grupo.

### G5 · Disfraces (lista)
- **Hoy:** lista con buscador y filtro por categoría.
- **Estructura:** buscador + chips → tarjetas con foto, nombre, piezas, tipo y precio sugerido →
  **FAB:** *Nuevo disfraz*.
- **Pendiente (necesita backend): el disfraz «incompleto».** Si una de sus piezas dejó de estar
  disponible (p. ej. la prenda se archivó), al **cliente el disfraz no se le ofrece**, pero al dueño
  **se le sigue mostrando, atenuado**, con el motivo y las dos salidas: *«No se ofrece: falta "Botas
  altas" (archivada)»* → **Reactivar la prenda** | **Cambiar la pieza**. La regla completa y por qué,
  en el lote de backend de `PROGRESS.md`.

### G6 · Armar disfraz (dueño) ⭐ **el peor punto de la app**
- **Objetivo:** definir de qué piezas se compone un disfraz.
- **Hoy:** formulario donde cada pieza se elige en un **desplegable por nombre**, sin fotos.
  **Su propio cliente tiene una experiencia mejor que él.**
- **Falta:** todo el modo visual.
- **Estructura propuesta:**
  1. Datos: nombre, categoría, tipo (con **"Automático según las piezas"** por defecto), precios.
  2. **Lista de piezas** — cada una es una tarjeta con miniatura de la prenda elegida.
  3. Al agregar/cambiar una pieza → **pantalla dedicada de selección**: grilla con foto, buscador,
     filtro por categoría y etiquetas, indicador de stock. Un toque para elegir.
  4. Para piezas personalizables: elegir **categoría + etiquetas permitidas** y ver en vivo **cuántas
     prendas caen en ese pool**.
  5. Vista previa del disfraz armado.
  6. **Barra fija:** *Guardar disfraz*.

### G7 · Punto de venta ⭐ **el segundo peor**
- **Objetivo:** cobrar rápido con el cliente enfrente.
- **Hoy:** formulario con sucursal, cliente (desplegable), líneas donde el artículo se elige **por nombre
  en un desplegable** y el precio se escribe a mano.
- **Falta:** velocidad. Con 128 prendas, buscar en un desplegable con alguien esperando no es viable.
- **Estructura propuesta:**
  1. **Buscador de producto arriba**, con foco automático. Sugerencias con foto, precio y stock.
  2. Toque = se agrega al ticket. El precio se autocompleta (editable con permiso).
  3. **Ticket** en el centro: líneas con cantidad ± y subtotal.
  4. Cliente: buscador con **"Nuevo cliente"** en línea, que pide **nombre, teléfono y email** sin salir
     de la venta (el empleado registra a quien llega al mostrador).
  5. **Barra fija:** total grande + *Cobrar*.
  6. Tras cobrar: **código de retiro grande** + opción de enviar comprobante.

### G8 · Ventas (lista) — ✅ rediseñada (fila y acciones); filtros de período pendientes de backend
- **Hoy:** lista paginada con buscador por código, total, cliente, estado, código de retiro y menú
  (cobrar, devolver).
- **Falta:** filtros por fecha y estado; totales del período.
- **Estructura:** filtros (hoy / semana / rango) → resumen del período → filas → **FAB:** *Nueva venta*.

### G9 · Rentas (lista) — ✅ rediseñada (fila y acciones); pestañas pendientes de backend
- **Antes:** filas con importe, cliente, dos fechas ISO, estado en texto y un menú con las 8 acciones
  del ciclo, ofrecidas todas incluso sobre una renta cerrada.
- **Ahora:** cada fila responde en orden — **quién y qué** (cliente, código de retiro, nº de artículos)
  → **cuándo** (`25 jul → 28 jul · 3 dias`) → **qué urge** (`Vence en 6 dias` / `Vencida hace 3 dias` /
  `Devuelta · falta cerrarla`, en el color del estado) → **qué hacer**: una **acción principal** según el
  estado (`Entregar` · `Registrar devolucion` · `Cerrar renta`) y en `Mas` solo las acciones válidas de
  ese estado; si queda una sola, el botón la ejecuta directo (`Ver contrato`).
- **Decisiones:** *Entregar* confirma mostrando el **código de retiro** y cuántos artículos salen (es
  cuando se coteja con el cliente); *devolver sin revisar* avisa que no cobra daños ni faltantes;
  *extender* usa **calendario** con el pasado bloqueado. Pastilla de estado con color semántico
  (verde al día · ámbar atención · rojo vencida) — ver `ui/common/Pastilla.kt`, reutilizable.
- **Pendiente (lote de backend):** pestañas **Por entregar | Activas | Vencidas | Cerradas** con conteos
  y orden por urgencia. Necesita `?filtro=` y `/rentas/resumen`: con la lista paginada, filtrar en la app
  solo filtraría la página cargada.

### G10 · Devoluciones (registrar) — ✅ rediseñada
- **Antes:** un formulario por pieza (nota obligatoria + desplegable de estado + dos checkboxes que podían
  contradecirse) y el resultado —cuánto se devuelve o se cobra— aparecía **después** de confirmar, en un
  aviso. El empleado cerraba la operación sin saber qué decirle al cliente que tenía enfrente.
- **Ahora:**
  1. **Cuándo volvió**: pactada vs. real y *«3 dias de retraso»* en rojo; calendario para corregirla.
  2. **Revisá las N piezas**: una tarjeta por unidad con **foto**, su valor de daño y **chips de un toque**
     (*Bien · Danada · En limpieza · No llego*). *No llego* ofrece «Cobrar la reposicion» y avisa que sin
     eso la renta queda pendiente. La nota es opcional y está plegada.
  3. **Depósito y cargos**, con los montos **sugeridos solos**: daños = suma del `valorDano` de lo marcado;
     recargo = política de la tienda (`recargoPorRetrasoPorDia` × días, o fijo). Si el usuario escribe el
     suyo, manda el suyo y el helper lo aclara. Si la tienda no tiene recargo configurado, lo dice en vez
     de prometer una sugerencia que no llega; si las multas están apagadas, avisa y no cobra nada.
  4. **Barra fija con la liquidación en vivo**: `Deposito − danos − retraso` y el resultado en grande,
     *A devolver al cliente* (verde) o *El cliente queda debiendo* (rojo), más «N de M piezas resueltas:
     la renta se cerrara / quedaran K pendientes» → *Registrar devolución*.
- **Comprobado contra el servidor:** lo que anticipa la barra coincide con el `remanente` que devuelve el
  backend al registrar.

### G11 · Clientes y ficha — ✅ rediseñada
- **Antes:** los filtros existían pero en jerga (*Pendientes · Vencidas · Multas · Saldos*) y en dos filas
  sueltas; la fila era nombre + contacto + una línea de texto rojo con «ver desglose» pegado al final.
- **Ahora:** buscador → **una fila** de filtros en cristiano (**Debe plata · Renta vencida · Con multas ·
  Algo pendiente**, y *Ver archivados* tras un divisor) → filas con **inicial en círculo**, nombre,
  **teléfono primero**, pastilla de estado y la **deuda como botón** que abre el desglose por renta.
- **Ficha:** datos → **Su cuenta** (*Estado de cuenta* · *Historial de pedidos*, que antes solo estaban en
  el menú de la lista) → **barra fija:** *Guardar*.
- **Pendiente (necesita backend):** marcar en la fila **quién tiene una renta activa**; `ClienteResponse`
  trae `saldoPendiente` y `multaTotal`, pero no si hay rentas en curso (el filtro del servidor sí lo sabe).

### G12 · Pagos y cobros — ✅ rediseñada (fila); resumen por método pendiente de backend
- **Hoy:** operaciones cobrables con código de retiro; registrar pago por método.
- **Falta:** ver lo pendiente de cobro como total, y filtro por método.
- **Estructura:** resumen (cobrado hoy por método) → pendientes de cobro → historial de pagos.

### G13 · Reembolsos — ✅ rediseñada
- **Hoy:** bandeja con solicitudes, aprobar/rechazar y **registrar devolución** (agregado hoy).
- **Falta:** ver el desglose de qué se reembolsa sin abrir.
- **Estructura:** pestañas *Pendientes | Resueltas* → tarjeta con cliente, monto, motivo, estado del ítem
  → acciones.

### G14 · Caja / turnos — ✅ rediseñada (fila); duración del turno pendiente de backend
- **Hoy:** abrir turno, movimientos, cerrar con cuadre.
- **Falta:** que el estado del turno sea visible **desde el panel** (si está abierto y cuánto lleva).
- **Estructura:** estado del turno → fondo inicial → movimientos → **barra fija:** *Cerrar turno*.

### G15 · Reportes — ✅ rediseñada (rankings con barras); gráficas de serie pendientes de backend
- **Hoy:** ingresos, rankings (prendas y disfraces más vendidos/rentados), ventas por empleado,
  inventario, export PDF/CSV.
- **Falta:** **gráficas**; hoy son listas de números.
- **Estructura:** selector de período y sucursal → tarjetas con gráfico → rankings con foto → exportar.

### G16 · Empleados — ✅ rediseñada (lista)
- **Hoy:** listar, alta, cambiar rol, activar/desactivar, permisos, asignar sucursales.
- **Falta:** ver la actividad del empleado desde su ficha (ya existe el endpoint).
- **Estructura:** lista con nombre, rol y estado → ficha con *Rol y permisos | Sucursales | Actividad*.

### G17 · Sucursales
- **Hoy:** crear, editar, archivar.
- **Falta:** **foto y descripción del local** (lo que pediste), horario y ubicación en mapa.

### G18 · Configuración — ✅ rediseñada
- **Hoy:** switches (conteo de stock, multas, multi-sucursal, pago en línea), impuesto, moneda, recargo,
  política de reembolsos, import/export.
- **Falta:** agrupar por tema y explicar qué hace cada opción. Hoy es una lista de switches sin contexto.

### G19 · Notificaciones — ✅ rediseñada (fila + paginación); nombre del destinatario pendiente de backend
- **Antes:** bandeja topada en 100 (el aviso #101 era invisible), canal en crudo («In_app»), estado en
  texto plano, fecha ISO, y el destinatario mostraba «General» cuando era un aviso al negocio.
- **Ahora:** **paginación real** (scroll infinito, buscador server-side) vía el nuevo
  `PaginaRemotaPagingSource` genérico; fila con **canal amigable** («Aviso interno»), **pastilla de
  estado** por color (`pintarPastilla`/`Tono`), destinatario **«Tu negocio»** en los avisos internos, y
  **fecha relativa** (`comoFechaHora`: «ayer 12:40»).
- **Pendiente (necesita backend):** `NotificacionResponse.clienteNombre` — hoy el nombre del cliente se
  resuelve contra una lista topada en 100. Ver lote de backend en `PROGRESS.md`.

### G20 · Mensajes automáticos — ✅ rediseñada
- **Hoy:** las 6 plantillas por tipo, cada una con su etiqueta, descripción, preview y switch on/off.
- **Ahora:** al editar, **vista previa en vivo con datos de ejemplo** (`{cliente}` → «Ana Torres»,
  `{dias_restantes}` → «3»…), que era el hueco de la spec: se ve cómo le llega el mensaje al cliente
  mientras se escribe. Los chips de variables siguen insertando en el cursor. Lista fija de 6 → **no
  necesita paginación.**

### G21 · Auditoría — ✅ rediseñada; filtro por usuario pendiente de backend
- **Hoy:** registro paginado con buscador.
- **Falta:** filtros por tipo de acción, usuario y fecha; y que cada entrada se lea en lenguaje natural.

### G22 · Taxonomía — ✅ rediseñada
- **Hoy:** categorías y tipos de etiqueta con valores; conteo de dependencias antes de archivar.
- **Falta:** que se entienda para qué sirve una etiqueta (talla, color) — hoy el concepto es abstracto.

---

## Parte IV — SuperAdmin y acceso

### S1 · Panel de plataforma
- **Hoy:** empresas pendientes (aprobar/rechazar), listado (suspender/reactivar).
- **Falta:** métricas de la plataforma y ficha de cada empresa.

### A1 · Acceso
- **Hoy:** login, registro, olvidé/restablecer contraseña.
- **Falta:** onboarding en el primer ingreso y entrada con biometría.

---

## Parte V — Patrones transversales (aplican a todas)

1. **Una sola acción principal por pantalla**, en barra fija inferior, en el color de acento. Todo lo
   demás es texto o contorno.
2. **La acción nombra su efecto**: *Cobrar*, *Registrar devolución*, *Elegir botas*. Nunca *Aceptar*.
3. **Cuatro estados por lista**: cargando (esqueleto, no spinner en blanco), vacío (con qué hacer),
   error (con reintentar), sin conexión (mostrando lo guardado y avisando).
4. **Feedback dentro del control**: el botón muestra su propio progreso; se deshabilita para evitar doble
   toque; al terminar, confirmación breve.
5. **Lo destructivo siempre igual**: mismo color, misma posición, siempre con confirmación que nombra lo
   que se va a perder.
6. **Búsqueda antes que desplegables**: cualquier lista de más de ~10 elementos se elige buscando, con
   foto cuando el elemento tiene foto.
7. **El dinero se lee alineado**: cifras con `tabular-nums`, mismo formato en toda la app.
8. **Nunca ofrecer lo que el rol no puede hacer** (ver H2).
9. **Cada pantalla dice dónde está parado el usuario**: tienda y sucursal activas siempre visibles en
   gestión.
10. **Cuando algo deja de estar disponible, cada uno ve algo distinto** (decidido el 2026-07-23 al revisar
    qué pasa si se archiva una prenda que usa un disfraz):
    - **Al cliente le desaparece.** Mostrarle atenuado algo que no puede comprar es ofrecerle un producto
      que no existe.
    - **Excepción: lo que él ya eligió.** Si está en su carrito, se queda visible con el aviso que
      bloquea el checkout (patrón de `C5`) — ahí necesita entenderlo y poder quitarlo.
    - **Al dueño no le desaparece nunca**: lo ve **atenuado, con el motivo y con la salida a un toque**.
      Si se esfumara de su lista no sabría por qué dejó de vender.
    - La condición se evalúa **al vuelo** («¿tiene todas sus piezas disponibles?»), nunca como un estado
      guardado: así se arregla sola por cualquiera de los caminos posibles.
11. **Las fechas del ciclo se muestran siempre que existan** (Juan, 2026-07-24). En una operación
    (renta/venta/devolución/pedido) el usuario tiene que poder ver **cuándo pasó cada cosa**: cuándo se
    registró, se entregó, se devolvió y se cerró. Regla: si el backend expone la fecha, se muestra (con
    `comoDiaMes`/período legible); si no la expone, va al lote de backend (ver `PROGRESS.md` → «FECHAS DEL
    CICLO DE VIDA»). Hoy solo reembolsos (creada/decidida) y pagos (fecha) las tienen; rentas exponen las
    fechas *pactadas* pero no los hitos reales, y ventas/devoluciones no exponen ninguna.
