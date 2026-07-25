# Scripts de datos de prueba (tienda demo)

Estaban en la carpeta temporal de la sesión, donde se pierden. Ahora viven acá.

Todos apuntan a la tienda **Disfraces Demo** en Railway y entran con el DUEÑO de prueba. Son
**idempotentes**: correrlos dos veces no duplica.

| Script | Para qué |
|---|---|
| `sembrar_datos.py` | Escenario base: prendas con precio/talla/color/stock en 2 sucursales y 4 disfraces de estructuras distintas (incluye casos borde, como una prenda con stock 0). |
| `subir_fotos.py` | Ilustraciones generadas por categoría y color, para que el catálogo no sean placeholders grises. |
| `parchar_etiquetas.py` | Asigna talla/color a prendas ya creadas (el seed original usaba mal el campo). |
| `sembrar_rentas_estado.py` | Una renta de cada estado (por entregar, activa, **vencida**, por cerrar) para probar `G9 Rentas` y `G10 Devoluciones`. Acepta un sufijo (`python sembrar_rentas_estado.py -v2`) para volver a sembrar cuando las anteriores ya avanzaron de estado. |
| `sembrar_alertas_panel.py` | Deja un reembolso PENDIENTE para ver varias alertas juntas en `G1 Panel`. |

Se ejecutan con `python <script>` desde esta carpeta.
