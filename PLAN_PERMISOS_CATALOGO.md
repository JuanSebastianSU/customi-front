# Costumi — Catálogo exhaustivo de permisos (Fase B, paso 5)

> **Qué es.** El catálogo **completo** de capacidades configurables de una tienda, derivado de las
> operaciones **reales** de la app (todos los controllers), no inventado. Es la spec del backend (modelo de
> permisos) y de la **pantalla de permisos rediseñada**. Complementa `PLAN_IDENTIDAD_PERMISOS.md`.
>
> **Principio rector (Juan).** *No se puede saber en quién confía cada dueño para cada cosa.* Por eso **cada
> capacidad es un toggle**: el rol es solo el **preset de defaults**, y la verdad es la matriz efectiva por
> persona. Si una capacidad no está en el catálogo, estaríamos horneando una suposición de confianza.

---

## 1. El modelo

- **Unidad = capacidad** (un toggle), identificada por `(seccion, clave)`, agrupada por sección. Reemplaza el
  `VER/ACCION` de 2 valores, que es demasiado grueso para las capacidades de gestión.
- **Preset por rol** = valores iniciales; el dueño (o quien tenga el permiso, en pirámide) los ajusta por
  persona. La **matriz efectiva** = preset ± overrides es la verdad para autorizar.
- Cada capacidad lleva **texto de qué habilita y qué consecuencia** (decisión #8: "clarísimo").

### Invariantes (no son toggles, gobiernan todo)
1. **Pirámide**: cada quien ve/edita capacidades **solo de los que están por debajo**; nunca hacia arriba.
2. **El dueño es tope de la tienda**: puede todo menos lo que es jurisdicción del **superadmin** → no es
   restringible dentro de su tienda.
3. **No podés conceder lo que no tenés**: nadie otorga una capacidad que él mismo no posee (evita auto-escalar).

Preset: **D**ueño · **E**ncargado · **M**ostrador · **A**tención · **B**odega. (● = concedido por defecto)

---

## 2. Catálogo

### INVENTARIO
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver prendas, stock y avisos de stock bajo | ● | ● | ● | ● | ● |
| prenda_gestionar | Crear/editar prendas y su foto | ● | ● | | | ● |
| prenda_archivar | Archivar/activar una prenda | ● | ● | | | ● |
| stock_entrada | Registrar entrada de mercadería | ● | ● | | | ● |
| **stock_ajustar** | Corregir el conteo — *puede tapar faltantes* | ● | ● | | | ● |
| stock_mover | Mover stock (reorganizar) | ● | ● | | | ● |
| stock_transferir | Transferir stock **entre sucursales** | ● | ● | | | ● |
| grupo_eliminar | Borrar una combinación de stock | ● | ● | | | |

### CATÁLOGO (categorías y etiquetas) 🆕
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver categorías (de prendas y de disfraces) y etiquetas | ● | ● | | | ● |
| categorias_gestionar | Crear/editar/archivar categorías (prendas y disfraces) | ● | ● | | | |
| etiquetas_gestionar | Definir tipos de etiqueta y sus valores (variantes) | ● | ● | | | |

### DISFRACES
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver disfraces y disponibilidad | ● | ● | ● | ● | ● |
| gestionar | Crear/editar disfraz y su foto | ● | ● | | | |
| archivar | Archivar/activar un disfraz | ● | ● | | | |

### VENTAS
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver ventas y totales | ● | ● | ● | ● | |
| registrar | Registrar una venta | ● | ● | ● | ● | |
| **descuento** | Aplicar descuento/precio especial — *plata que se resigna* | ● | ● | | | |
| devolver | Devolver una venta (reembolso de stock/plata) | ● | ● | ● | ● | |

### RENTAS
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver rentas y su resumen | ● | ● | ● | ● | |
| registrar | Crear una renta | ● | ● | ● | ● | |
| entregar | Marcar entrega | ● | ● | ● | ● | |
| devolver | Registrar devolución (calcula multa por retraso) | ● | ● | ● | ● | |
| cerrar | Cerrar la renta y **decidir el depósito** (devolver/retener) | ● | ● | ● | ● | |
| **cancelar** | Cancelar una renta | ● | ● | | | |
| **extender** | Extender el plazo — *afecta disponibilidad y cobro* | ● | ● | | | |

### DEVOLUCIONES
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver devoluciones | ● | ● | ● | ● | |
| registrar | Registrar una devolución | ● | ● | ● | ● | |

### PAGOS
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver pagos, saldos y comprobantes | ● | ● | ● | ● | |
| registrar | Registrar un pago (incluye pago mixto) | ● | ● | ● | ● | |
| cobrar_en_linea | Generar cobro por pasarela | ● | ● | ● | ● | |

### CAJA
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver turnos y movimientos | ● | ● | ● | ● | |
| abrir_turno | Abrir un turno de caja | ● | ● | ● | ● | |
| movimiento | Registrar un movimiento | ● | ● | ● | ● | |
| **cerrar_turno** | Cerrar turno (arqueo/conciliación) | ● | ● | ● | ● | |

### REEMBOLSOS 🆕
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver reembolsos | ● | ● | ● | ● | |
| solicitar | Solicitar/registrar un reembolso | ● | ● | ● | ● | |
| **aprobar** | Aprobar — *plata que sale*, alta confianza | ● | ● | | | |
| **rechazar** | Rechazar un reembolso | ● | ● | | | |

### CLIENTES
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver clientes, historial y estado de cuenta (deudas) | ● | ● | ● | ● | |
| crear | Registrar un cliente | ● | ● | ● | ● | |
| editar | Editar datos del cliente | ● | ● | ● | ● | |
| archivar | Archivar/activar un cliente | ● | ● | | | |
| **lista_negra** | Poner/quitar de lista negra — *bloquea al cliente* | ● | ● | | | |

### REPORTES
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver reportes (ventas/rentas/estado de cuenta/rankings) | ● | ● | | | |

### AUDITORÍA 🆕 (separada de reportes)
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver quién hizo qué — trazabilidad sensible | ● | ● | | | |

### CONFIGURACIÓN
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver la configuración del local | ● | ● | | | |
| **editar** | Cambiar interruptores (multas, multi-sucursal, impuesto, reembolsos, recargo…) — *cambia reglas de todo el local* | ● | ● | | | |
| importar_exportar | Importar/exportar la configuración | ● | ● | | | |

### SUCURSALES (de la empresa) 🆕
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver las sucursales | ● | ● | | | |
| gestionar | Crear/editar/archivar sucursal (incluye su foto) | ● | ● | | | |

### IDENTIDAD DE TIENDA 🆕
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| editar | Datos (nombre/descripción/ciudad), logo/portada, horario — *cara pública en el marketplace* | ● | ● | | | |

### NOTIFICACIONES / MENSAJES AUTOMÁTICOS
| Clave | Qué habilita | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Ver estado de canales y plantillas | ● | ● | | | |
| plantillas_editar | Editar las plantillas de mensajes | ● | ● | | | |
| enviar | Enviar una notificación manual | ● | ● | | ● | |
| disparar_avisos | Disparar recordatorios/avisos (vencidas, próximas, stock bajo) | ● | ● | | | |
| probar_push | Probar el envío push | ● | ● | | | |

### EMPLEADOS / GESTIÓN DE PERSONAL (la más crítica)
| Clave | Qué habilita / consecuencia | D | E | M | A | B |
|---|---|:-:|:-:|:-:|:-:|:-:|
| ver | Lista de personal y su actividad | ● | ● | | | |
| invitar | Invitar gente a la tienda | ● | ● | | | |
| invitacion_cancelar | Cancelar una invitación pendiente | ● | ● | | | |
| suspender | Suspender/reactivar una membresía (reversible) | ● | ● | | | |
| dar_de_baja | Despedir (baja definitiva) | ● | ● | | | |
| cuenta_estado | Desactivar/activar la **cuenta** (corta hasta el login) | ● | ● | | | |
| cambiar_rol | Cambiar el rol (preset) de otro | ● | ● | | | |
| **★ editar_permisos** | Editar los permisos de otros — reparte confianza (sujeta a "no dar lo que no tenés") | ● | ● | | | |
| asignar_sucursales | Asignar/reasignar a qué sucursales opera cada uno (el encargado, solo entre las suyas) | ● | ● | | | |

---

## 3. Fuera del catálogo (a propósito)
- **Plataforma (superadmin)**: aprobar/rechazar/suspender empresas — jurisdicción del superadmin, no de la tienda.
- **Self-service**: perfil propio, cambio de contraseña, favoritos, carrito propio, cambio de contexto,
  desvincularme — se resuelven por el token del propio usuario, no son capacidades sobre terceros.
- **Marketplace / público**: catálogo público, destacados, etc. — sin permiso.

## 4. Notas de implementación (backend)
- La autorización pasa de "por rol" (`hasAnyRole` en `SecurityConfig`) a **por capacidad efectiva** (preset ±
  override), respetando pirámide. Hoy `permiso_empleado.seccion varchar(20)` / `accion varchar(10)`: revisar
  longitudes al nombrar claves.
- Definir el default de **cada** capacidad por rol en `PlantillaDeRol` (arriba). Cuidado con no abrir huecos:
  lo nuevo, apagado por defecto para operativos salvo lo listado.
- El dueño no es restringible (tope de la tienda). El interceptor de permisos hoy salta a SUPERADMIN/DUENO.
