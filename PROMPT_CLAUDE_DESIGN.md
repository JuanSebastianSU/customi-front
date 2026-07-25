# Prompts para Claude Design

> ## ⚠️ Lo que buscamos es la ESTRUCTURA, no la piel
>
> El valor de este ejercicio está en **qué bloques tiene cada pantalla, en qué orden, y qué contiene
> cada bloque**. El color, la tipografía y las transiciones son envoltorio: se pueden cambiar después
> sin costo. La arquitectura de la pantalla, no.
>
> Por eso, al evaluar lo que devuelva Claude Design, la pregunta no es "¿se ve lindo?" sino:
> - ¿Están todos los bloques que la pantalla necesita?
> - ¿El orden lleva al usuario a la acción sin que tenga que pensar?
> - ¿Cada bloque muestra los datos que hacen falta para decidir?
> - ¿La acción principal está abajo y dice qué hace?
>
> **Cómo usarlos.**
> 1. En Claude Design, plantilla **Wireframe** (estructura) o **Mobile app design** si además querés ver
>    el acabado. Para lo que necesitamos, *Wireframe* alcanza y es más rápido de evaluar.
> 2. El **Prompt 1** (sistema de diseño) es **opcional**: solo si querés que las pantallas salgan
>    vestidas. Se puede saltar sin perder nada de lo importante.
> 3. Pegá el **Prompt 2 (base)** seguido de una tanda — **una tanda por vez**. Si pedís 21 pantallas
>    juntas, la calidad baja.
>
> El detalle de qué lleva cada pantalla está en `ESPECIFICACION_UI.md`.

---

# PROMPT 1 · Sistema de diseño

```
Diseñá el sistema de diseño completo de una app Android nativa (Material Design 3) llamada
Costumi.

QUÉ ES COSTUMI
Un marketplace de alquiler y venta de disfraces. Dos mundos conviven en la misma app:

- El CLIENTE explora tiendas de disfraces, arma su disfraz eligiendo cada pieza (sombrero,
  casaca, botas), lo alquila por un rango de fechas o lo compra, y lo retira en el local.
- La TIENDA (dueño y empleados) opera su negocio: inventario, punto de venta, alquileres,
  devoluciones con multas por daño o retraso, caja, clientes y reportes.

La app se usa en dos situaciones opuestas y el diseño tiene que servir a las dos: un cliente
en su sofá eligiendo un disfraz para una fiesta, y un empleado en el mostrador con una persona
esperando delante.

TONO Y MUNDO VISUAL
El mundo del disfraz es teatro y vestuario: camerino, telón, luz cálida de bombilla sobre el
espejo, terciopelo, etiquetas de sastre cosidas a mano. Quiero que se sienta ese mundo, con
elegancia y sin literalidad: nada de calabazas, telarañas ni tipografías de terror. Elegante y
minimalista.

Tiene que verse profesional para un dueño que la mira 8 horas al día, y deseable para un
cliente que está eligiendo cómo se va a ver en una fiesta.

QUÉ EVITAR
No uses los recursos que ya están agotados: crema #F4F1EA con serif y acento terracota; negro
casi puro con un verde ácido; degradado violeta a azul; Inter o Space Grotesk como fuente
"segura"; emojis como íconos de sección; todo centrado. Buscá una combinación que se reconozca
como Costumi y no como "una app genérica".

LAS CUATRO REGLAS QUE MANDAN

1. JERARQUÍA VISUAL
   En cada pantalla, el usuario debe identificar en menos de un segundo cuál es LA acción
   principal, y distinguirla de las secundarias. Solo esa acción lleva el color de acento;
   todo lo demás vive en texto, contorno o superficie. El orden de lectura es unidireccional
   y predecible: contexto arriba, contenido en el medio, acción abajo.

2. FEEDBACK INMEDIATO
   Nada puede procesarse en silencio. Definí para cada control sus cuatro estados: reposo,
   presionado, cargando y completado. El progreso vive DENTRO del control que se tocó (el
   botón muestra su propio spinner y se deshabilita), nunca como una capa que tapa la pantalla.
   Las listas cargan con esqueletos, no con un spinner sobre fondo vacío.

3. CONSISTENCIA
   Un componente se define una vez y se ve igual en las 36 pantallas. En particular: una acción
   destructiva (eliminar, archivar, cancelar) tiene siempre el mismo color, la misma posición y
   el mismo diálogo de confirmación. La unicidad visual evita que el usuario dude.

4. ERGONOMÍA (Ley de Fitts)
   Todo lo que se toca seguido vive en la mitad inferior de la pantalla. La acción principal va
   en una barra fija anclada abajo, por encima de la navegación. La zona superior es para leer
   —título, contexto, estado—, no para tocar. Obligar a estirar el pulgar hacia arriba para una
   acción frecuente es un error estructural.

QUÉ QUIERO QUE ENTREGUES

COLOR
- Fondo, superficie, superficie elevada, borde, texto primario, texto secundario.
- UN color de acento, y nada más. Es el color de "acá se toca".
- Colores semánticos aparte: éxito, advertencia, crítico. Regla dura: el acento NUNCA comunica
  estado, y el color semántico NUNCA se usa para una acción.
- Modo claro y modo oscuro, los dos con el mismo cuidado. No inviertas los valores: recalibrá
  el contraste y verificá que el acento siga funcionando sobre los dos fondos.
- El gris neutro no puede ser un gris puro: dale un sesgo de matiz hacia el mundo de la marca.

TIPOGRAFÍA
- Dos familias: una con carácter para nombres de disfraz, títulos de pantalla y cifras
  destacadas; una neutra y muy legible para operar (formularios, botones, listas densas).
- Escala tipográfica definida, con sus pesos y espaciados.
- Las cifras de dinero usan numeración tabular, para que los precios alineen en columna y se
  comparen de un vistazo. En esta app se leen muchos precios seguidos.

ESPACIO Y FORMA
- Escala de espaciado en múltiplos de 4.
- Radios de esquina y elevaciones definidos, con criterio de cuándo se usa cada uno.
- Densidad: las pantallas del cliente pueden respirar; las de gestión muestran más datos por
  pantalla porque se operan, no se contemplan.

COMPONENTES (definí cada uno con todos sus estados)
- Barra superior con título y contexto.
- Barra de acción inferior fija (contiene el resumen y la acción principal).
- Navegación inferior de 5 pestañas.
- Botones: primario, secundario, terciario y destructivo.
- Campo de texto, campo con error, buscador con foco.
- Chip de filtro (con y sin selección).
- Tarjeta de producto (foto, nombre, precio, disponibilidad).
- Tarjeta de lista con acción secundaria.
- Carrusel horizontal de tarjetas.
- Pastilla de estado (disponible, vencido, pagado, pendiente).
- Alerta accionable con franja de color semántico.
- Estados de lista: esqueleto de carga, vacío con qué hacer, error con reintentar, sin conexión
  mostrando datos guardados.
- Hoja inferior (bottom sheet) para selección.
- Diálogo de confirmación destructiva.
- Selector de cantidad (menos / número / más).
- Selector de rango de fechas.

CONTENIDO
Usá contenido real de este negocio, nunca texto de relleno: disfraces como "Pirata del Caribe",
"Veneciano", "Bruja"; piezas como "Tricornio negro", "Casaca de terciopelo", "Botas altas";
tiendas como "Disfraces El Carrusel"; precios en pesos colombianos con separador de miles
(ej. $80.000 por día).
```

---

# PROMPT 2 · Base para las pantallas

> Este bloque va **antes** de cada tanda. Después pegás la tanda que toque.

```
Diseñá las siguientes pantallas de una app Android llamada Costumi.

LO QUE MÁS ME IMPORTA
La ESTRUCTURA de cada pantalla: qué bloques tiene, en qué orden vertical van, y exactamente qué
datos muestra cada bloque. El acabado visual es secundario; lo que voy a evaluar es la
arquitectura de la información y el recorrido del usuario.

Por eso, para cada pantalla quiero que enumeres los bloques de arriba hacia abajo y que digas,
para cada uno: qué contiene, qué datos concretos muestra, si es tocable o solo informativo, y
qué pasa al tocarlo. Si un bloque puede estar vacío o en error, mostrá también ese caso.

Preferí densidad de información antes que decoración: si un dato ayuda a decidir, va.

RECORDATORIO DEL NEGOCIO
Un DISFRAZ se compone de PIEZAS. Algunas son fijas (vienen sí o sí) y otras las elige el cliente
entre varias opciones, filtrando por talla y color. Un disfraz se alquila por un rango de fechas
o se compra. Al alquilar se cobra un depósito; al devolver, si algo vuelve dañado o tarde, se
descuenta del depósito y si no alcanza se cobra una multa. El cliente retira en el local
mostrando un CÓDIGO DE RETIRO.

ESTRUCTURA OBLIGATORIA DE TODA PANTALLA
- ARRIBA: dónde estoy. Título y contexto (tienda, sucursal, fecha). Se lee, no se toca.
- MEDIO: el contenido, desplazable.
- ABAJO: barra fija con el resumen relevante y LA acción principal, por encima de la navegación.

REGLAS DE REDACCIÓN EN LA INTERFAZ
- La acción nombra su efecto: "Cobrar", "Elegir botas", "Registrar devolución", "Agregar al
  carrito". Nunca "Aceptar", "Continuar" ni "Enviar".
- Los errores dicen qué pasó y cómo resolverlo, sin disculpas ni vaguedades.
- Los estados vacíos dicen qué hacer para llenarlos.

REGLAS DE INTERACCIÓN
- Cualquier lista de más de 10 elementos se elige BUSCANDO, nunca con un desplegable.
- Si el elemento tiene foto, la foto se muestra al elegirlo.
- Lo que un rol no puede hacer no se muestra: nada de opciones que llevan a un error de permisos.

ENTREGA
Mostrá cada pantalla en marco de teléfono. Junto a cada una, una lista numerada de sus bloques
de arriba hacia abajo, diciendo qué contiene cada uno y qué pasa al tocarlo.

No hace falta modo claro y oscuro ni variaciones de estilo: prefiero ver más pantallas y más
estados (vacío, cargando, error) que la misma pantalla vestida de dos maneras.

PANTALLAS:
```

---

## TANDA 1 · El cliente descubre y arma *(empezá por acá)*

```
1. EXPLORAR — la primera pantalla que ve un cliente.
   Saludo con su nombre y su ciudad ("Buenas tardes, Juan · Bogotá · 12 tiendas cerca").
   Buscador que encuentra disfraces, tiendas y categorías a la vez.
   Carrusel horizontal "Para este fin de semana": disfraces destacados con foto grande, nombre,
   precio por día y tienda.
   Fila de chips de categoría desplazable: Piratas, Época, Terror, Superhéroes, Infantil.
   Lista de tiendas: logo, nombre, barrio, distancia en km, pastilla de "Abierto" en verde o
   "Cierra 6:00 p. m.", y cuántos disfraces tiene.
   Navegación inferior de 5 pestañas: Explorar, Buscar, Pedidos, Guardados, Perfil.
   Esta pantalla NO tiene acción principal: es de descubrimiento puro.

2. TIENDA — el catálogo de un local.
   Portada con imagen del local, logo encima, nombre, barrio, horario y estado abierto/cerrado.
   Descripción breve de la tienda y acceso a su ubicación.
   Pestañas: "Disfraces" y "Prendas sueltas".
   Buscador y chips de categoría.
   Grilla de 2 columnas: foto, nombre, precio y si está disponible ahora.

3. DISFRAZ / ARMADO — la pantalla más importante de toda la app.
   Galería de fotos del disfraz, deslizable, con indicadores de posición.
   Nombre del disfraz en la tipografía con carácter, tienda y si es para alquiler, venta o ambos.
   Progreso explícito del armado: "Tu disfraz · 3 de 4 piezas".
   Lista de piezas:
     - las ya elegidas muestran miniatura de la prenda, nombre, talla y un check en color acento;
     - las pendientes muestran un marcador de vacío y un "Elegir ›" claro.
   Selector de fechas de alquiler (rango) y selector de cantidad.
   Sucursal donde va a retirar.
   BARRA FIJA ABAJO: a la izquierda el precio por día y el total real por los días elegidos
   ("$80.000 por día · 3 días = $240.000"); a la derecha el botón, que nombra exactamente lo que
   falta: "Elegir botas". Cuando el disfraz está completo, el botón pasa a "Agregar al carrito".

4. ELEGIR PIEZA — se abre al tocar una pieza pendiente.
   Título con el nombre de la pieza ("Botas") y cuántas opciones hay.
   Buscador arriba.
   Chips para filtrar por talla y por color.
   Grilla de opciones con foto grande, nombre, precio y stock disponible.
   La opción actualmente elegida se marca con el color de acento y un check.
   Un toque selecciona y vuelve a la pantalla anterior.
   Mostrá también el estado vacío: qué ve el usuario si ninguna opción tiene stock.

5. CARRITO.
   Arriba: tienda y sucursal de retiro.
   Líneas del pedido: foto, nombre, período de alquiler cuando corresponde ("12 ago → 15 ago"),
   selector de cantidad con menos y más, y subtotal.
   Si un artículo dejó de estar disponible, la línea lo avisa con color semántico y explica por qué.
   BARRA FIJA ABAJO: total del pedido y botón "Confirmar pedido".
   Mostrá también el estado del carrito vacío.
```

---

## TANDA 2 · La tienda opera

```
1. PANEL DEL DUEÑO — lo primero que ve al abrir la app.
   Arriba: nombre de la tienda, sucursal activa y fecha de hoy.
   ALERTAS ACCIONABLES primero, cada una con franja de color semántico y llevando a su lista ya
   filtrada: "3 rentas vencen hoy", "2 devoluciones pendientes", "5 prendas con stock bajo".
   Tarjeta de ingresos del día: cifra grande, variación contra ayer, y gráfico de barras de los
   últimos 7 días con el día de hoy destacado.
   Fila de cifras secundarias: rentas activas, ventas de hoy, clientes nuevos.
   BARRA FIJA ABAJO: "Nueva venta" (la acción más frecuente de la jornada).

2. PUNTO DE VENTA — pantalla de mostrador, con el cliente esperando enfrente. La velocidad es
   todo: hoy esta pantalla usa desplegables por nombre y hay que rehacerla por completo.
   Buscador de producto arriba, con el foco puesto al abrir. Al escribir, sugerencias con foto,
   nombre, precio y stock; un toque lo agrega al ticket.
   TICKET en el centro: cada línea con foto, nombre, cantidad ajustable con menos y más, precio
   unitario y subtotal. Se puede quitar una línea.
   Selector de cliente por búsqueda (nombre o teléfono), con la opción "Nuevo cliente" en línea:
   abre un formulario mínimo de nombre, teléfono y email SIN salir de la venta, porque el
   empleado registra a la persona que acaba de llegar al mostrador.
   BARRA FIJA ABAJO: total en cifra grande y botón "Cobrar".

3. VENTA COBRADA — confirmación.
   CÓDIGO DE RETIRO muy destacado: es lo que el cliente va a mostrar cuando venga a retirar.
   Resumen de la venta y método de pago.
   Acciones: enviar comprobante y hacer otra venta.

4. ALQUILERES — lista de trabajo del día.
   Pestañas por estado: Por entregar, Activos, Vencidos, Cerrados.
   Cada fila: foto del artículo, cliente, código de retiro, fechas y días restantes.
   Los vencidos se distinguen con color semántico crítico.
   La acción de cada fila depende de su estado: entregar, registrar devolución, cerrar.

5. DEVOLUCIÓN — el momento más delicado, porque puede terminar en un cobro.
   Arriba: qué alquiler se está devolviendo, con cliente y fechas.
   Lista de piezas, cada una con su estado seleccionable: buena, dañada, faltante.
   Campos de cargo por daño y por retraso.
   CÁLCULO EN VIVO mientras se completa, siempre visible: depósito menos cargos, y si el
   resultado es a favor del cliente o una multa a cobrar. El usuario nunca descubre el número
   recién al final.
   BARRA FIJA ABAJO: "Registrar devolución".

6. INVENTARIO.
   Buscador y chips de filtro: categoría, stock bajo, archivadas.
   Filas con foto, nombre, tipo (alquiler / venta / ambos), precio y stock por sucursal.
   Botón flotante: "Nueva prenda".
```

---

## TANDA 3 · Configurar y analizar

```
1. ARMAR DISFRAZ (vista del dueño) — hoy es un formulario con desplegables por nombre y es el
   peor punto de la app: el cliente tiene una experiencia mejor que el profesional. Rehacela.
   Datos arriba: nombre, categoría, tipo de disponibilidad con "Automático (según las piezas)"
   como opción por defecto, y precios (mostrando solo los que aplican al tipo).
   LISTA DE PIEZAS: cada pieza es una tarjeta con la miniatura de la prenda elegida, su nombre y
   si es fija o la elige el cliente. Botón para agregar otra pieza.
   Para las piezas que elige el cliente: seleccionar categoría y etiquetas permitidas (talla,
   color), mostrando EN VIVO cuántas prendas caen en ese grupo, para que el dueño sepa si va a
   tener opciones reales.
   Vista previa del disfraz armado.
   BARRA FIJA ABAJO: "Guardar disfraz".

2. ELEGIR PRENDA (vista del dueño) — el equivalente profesional de la pantalla de elegir pieza
   del cliente.
   Grilla con foto grande, nombre, precio y stock por sucursal.
   Buscador arriba y filtros por categoría y etiqueta.
   Un toque elige y vuelve.

3. FICHA DE CLIENTE.
   Arriba: nombre, teléfono, email y documento.
   Pastillas de estado: si debe dinero (con el monto), si tiene un alquiler activo, si está en
   lista negra.
   Pestañas: Datos; Historial de compras y alquileres (con foto de lo que llevó y el código de
   retiro de cada operación); Estado de cuenta con el desglose de lo que debe y por qué.

4. REPORTES.
   Selector de período y de sucursal.
   Tarjetas con gráfico: ingresos por día, y comparación alquiler contra venta.
   Rankings CON FOTO: prendas más alquiladas, disfraces más alquilados, más vendidos.
   Ventas por empleado.
   Acción para exportar en PDF o CSV.

5. CONFIGURACIÓN — hoy es una lista de interruptores sin explicación.
   Opciones agrupadas por tema: Operación, Cobros, Multas, Sucursales.
   Cada opción con una línea que explica qué hace y qué implica activarla o desactivarla.
   Las que tienen consecuencia sobre datos existentes lo advierten antes.
```

---

## TANDA 4 · Cliente, pantallas restantes

```
1. MIS PEDIDOS.
   Filtros por estado: Todos, Por pagar, Por retirar, Activos, Cerrados.
   Tarjetas con foto, nombre de lo que llevó, tienda, código de retiro, fechas, estado y monto.
   Si un pedido tiene algo pendiente (pagar, devolver), la tarjeta lo ofrece como acción.
   Al abrir: desglose completo, con las piezas del disfraz agrupadas bajo el nombre del disfraz
   y no como prendas sueltas.

2. MIS MULTAS.
   Total adeudado como cifra destacada arriba (se oculta si no debe nada).
   Una tarjeta por deuda: tienda, código del alquiler, fechas y el DESGLOSE que explica el
   cargo ("Daños $150.000 − Depósito $50.000 = Multa $100.000"). Una multa sin explicación es
   un reclamo asegurado.
   Las ya pagadas se ven, pero sin la cifra en rojo.
   BARRA FIJA ABAJO: "Pagar ahora" cuando hay saldo pendiente.

3. PAGO.
   Resumen de lo que se paga, con tienda y artículos.
   Método de pago.
   Tras pagar: CÓDIGO DE RETIRO en grande, con dónde y cuándo retirar.

4. PERFIL.
   Foto, nombre, email y teléfono editables.
   Accesos: Mis multas, Direcciones, Notificaciones, Cambiar contraseña.
   "Registrar mi tienda" para quien quiera abrir su local.
   Cerrar sesión, claramente separado del resto.

5. ACCESO — login, registro y recuperar contraseña.
   Login con email y contraseña, entrada con biometría, y enlaces a registro y recuperación.
   El registro pide lo mínimo: nombre, email y contraseña.
   Todos los errores explican qué corregir, campo por campo.
```

---

## Después del diseño

Cuando las pantallas estén aprobadas, se implementan en Android en este orden:

1. **Tokens y componentes** en `themes.xml` y `styles.xml` — la base de la consistencia.
2. **Las dos pantallas críticas**: armar disfraz (dueño) y punto de venta.
3. **El recorrido del cliente**: explorar → tienda → disfraz → carrito.
4. **El resto de gestión**, pantalla por pantalla.

Cada bloque, su rama y su PR.
