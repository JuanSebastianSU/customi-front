"""
Siembra un escenario de prueba REALISTA en la tienda demo de Costumi.

Objetivo: poder evaluar el rediseño con datos que se parezcan a los de una tienda de verdad —precios
en pesos colombianos, prendas con talla y color, disfraces de varias piezas con opciones reales y
stock repartido entre sucursales—. Con dos prendas sueltas y precios de $4 no se puede juzgar nada.

Es idempotente por nombre: si una prenda o disfraz ya existe, no lo duplica.
"""
import json
import sys
import urllib.request

API = "https://just-upliftment-production-cb1f.up.railway.app"
EMPRESA = "8190811c-2f64-4e5c-9f1c-55b0765a79a1"

CAT = {
    "Sombrero": "9467a282-cf81-4fb0-8c26-ff9232c2f3bd",
    "Camisa": "2d3246ac-3c43-4ba1-a7df-22b5c22876b4",
    "Pantalón": "a01af1f0-36b9-4857-bfd2-70c153d2fe7d",
    "Zapatos": "047fef94-4eb3-4eb5-ac36-8ea29f3e1df8",
    "Accesorio": "3d5c300d-b2fa-4119-81ed-5f7c05510c63",
    "Vestido": "e96e92c8-3edc-4d19-a89d-883c056df3d0",
    "Capas de Gala": "4fa82264-dcc0-4db6-bbe4-42cd202f9a61",
}
TIPO_COLOR = "6276d479-2157-494b-9674-97156f6a0ee7"
TIPO_TALLA = "4c6cbd9d-0f15-42c3-ba4b-6222b233e106"
COLOR = {
    "Rojo": "9d72edf2-c290-4509-9a22-9e465f645ee7",
    "Azul": "5bb0b0a8-265d-49e5-b979-43b7d2e91758",
    "Negro": "d44298b8-58c1-48f1-a91d-5e341794744e",
    "Blanco": "a0a00b7c-c4b0-492d-9285-a8f74ba96b1e",
    "Verde": "00fe7749-654d-4157-9586-304ed92a5304",
}
TALLA = {
    "S": "42d500c4-fb1f-4fa8-96f8-fd913f728cf1",
    "M": "0cc6089f-73ec-48d1-958c-db6c9f7d522a",
    "L": "de908465-a30e-4771-91ef-e0bc906d8d2c",
    "XL": "7c994178-8491-4774-8bf0-e28158353fe5",
}
MATRIZ = "d48c1f49-0a0f-4ba3-bf0f-cae3c8dea9ca"
NORTE = "4ea3dacc-e10e-4b16-aec8-61e3b16d5432"

token = ""


def pedir(metodo, ruta, cuerpo=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo, headers={
        "Authorization": "Bearer " + token,
        "Content-Type": "application/json",
        "Accept": "application/json",
    })
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode()
            return json.loads(texto) if texto else None
    except urllib.error.HTTPError as e:
        detalle = e.read().decode()[:300]
        raise RuntimeError(f"{metodo} {ruta} -> {e.code}: {detalle}") from None


def entrar():
    global token
    datos = json.dumps({"email": "dueno.demo.635935@costumi.test", "password": "Dueno123!"}).encode()
    req = urllib.request.Request(API + "/api/v1/auth/login", data=datos,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        token = json.load(r)["accessToken"]


# ---------------------------------------------------------------- prendas

# (nombre, categoria, tipo, precioRenta, precioVenta, color, talla, dano, reposicion, stock_matriz, stock_norte)
# Precios en pesos colombianos, con la proporcion habitual del rubro: la renta diaria ronda el 15-20%
# del valor de venta, y el valor de reposicion es mayor que el de venta (reponer al proveedor cuesta mas).
PRENDAS = [
    # --- Mundo pirata ---
    ("Tricornio negro",          "Sombrero",  "AMBOS", 18000, 95000,  "Negro",  "M",  40000,  110000, 6, 3),
    ("Sombrero pirata rojo",     "Sombrero",  "AMBOS", 16000, 85000,  "Rojo",   "M",  35000,  100000, 4, 2),
    ("Pañuelo bucanero",         "Sombrero",  "AMBOS",  8000, 30000,  "Rojo",   "M",  12000,   38000, 9, 5),
    ("Casaca de terciopelo vino","Camisa",    "AMBOS", 45000, 260000, "Rojo",   "M",  90000,  310000, 3, 2),
    ("Casaca de terciopelo vino L","Camisa",  "AMBOS", 45000, 260000, "Rojo",   "L",  90000,  310000, 2, 1),
    ("Camisa blanca de bucanero","Camisa",    "AMBOS", 22000, 120000, "Blanco", "M",  45000,  140000, 7, 4),
    ("Camisa blanca de bucanero L","Camisa",  "AMBOS", 22000, 120000, "Blanco", "L",  45000,  140000, 5, 3),
    ("Pantalón bombacho negro",  "Pantalón",  "AMBOS", 20000, 110000, "Negro",  "M",  40000,  130000, 6, 3),
    ("Pantalón bombacho negro L","Pantalón",  "AMBOS", 20000, 110000, "Negro",  "L",  40000,  130000, 4, 2),
    ("Botas bucaneras",          "Zapatos",   "AMBOS", 28000, 180000, "Negro",  "L",  60000,  210000, 5, 2),
    ("Botas altas de cuero",     "Zapatos",   "AMBOS", 25000, 165000, "Negro",  "M",  55000,  195000, 4, 2),
    ("Botines marrones",         "Zapatos",   "AMBOS", 18000, 120000, "Verde",  "M",  40000,  140000, 3, 1),
    ("Botas de abordaje",        "Zapatos",   "RENTA", 30000, None,   "Negro",  "XL", 65000,  220000, 0, 0),
    ("Cinturón de cuero ancho",  "Accesorio", "AMBOS", 10000, 55000,  "Negro",  "M",  20000,   65000, 8, 4),
    ("Garfio y parche",          "Accesorio", "AMBOS",  7000, 25000,  "Negro",  "M",  10000,   30000, 10, 6),

    # --- Mundo veneciano / gala ---
    ("Máscara veneciana dorada", "Accesorio", "AMBOS", 26000, 145000, "Blanco", "M",  50000,  170000, 5, 3),
    ("Máscara veneciana negra",  "Accesorio", "AMBOS", 26000, 145000, "Negro",  "M",  50000,  170000, 4, 2),
    ("Máscara de plumas azul",   "Accesorio", "AMBOS", 30000, 160000, "Azul",   "M",  55000,  185000, 3, 1),
    ("Vestido de gala rojo",     "Vestido",   "AMBOS", 60000, 380000, "Rojo",   "M", 120000,  450000, 3, 1),
    ("Vestido de gala negro",    "Vestido",   "AMBOS", 60000, 380000, "Negro",  "L", 120000,  450000, 2, 1),
    ("Capa de terciopelo negra", "Capas de Gala","AMBOS",40000,240000, "Negro",  "M",  80000,  280000, 4, 2),
    ("Capa de terciopelo roja",  "Capas de Gala","AMBOS",40000,240000, "Rojo",   "M",  80000,  280000, 3, 2),
    ("Guantes largos de raso",   "Accesorio", "AMBOS", 12000, 60000,  "Blanco", "S",  22000,   70000, 6, 3),

    # --- Solo venta (para probar la coherencia de tipo del disfraz) ---
    ("Peluca rizada blanca",     "Accesorio", "VENTA", None,  95000,  "Blanco", "M",  0,       95000, 5, 2),
]


def sembrar_prendas():
    existentes = {}
    pag = pedir("GET", "/api/v1/prendas?tamano=300")
    for p in pag.get("contenido", []):
        existentes[p["nombre"]] = p["id"]

    creadas, saltadas = 0, 0
    ids = {}
    for (nombre, cat, tipo, renta, venta, color, talla, dano, repo, s1, s2) in PRENDAS:
        if nombre in existentes:
            ids[nombre] = existentes[nombre]
            saltadas += 1
            continue
        cuerpo = {
            "categoriaId": CAT[cat],
            "nombre": nombre,
            "tipoArticulo": tipo,
            # La API espera pares {tipoEtiquetaId, valorEtiquetaId}, no una lista plana de valores; con el
            # campo mal nombrado las etiquetas se ignoraban y la ruleta no podia mostrar talla ni color.
            "etiquetas": [
                {"tipoEtiquetaId": TIPO_COLOR, "valorEtiquetaId": COLOR[color]},
                {"tipoEtiquetaId": TIPO_TALLA, "valorEtiquetaId": TALLA[talla]},
            ],
            "costoAdquisicion": int((venta or repo) * 0.45),
            "valorDano": dano,
            "valorReposicion": repo,
        }
        if renta is not None:
            cuerpo["precioRenta"] = renta
        if venta is not None:
            cuerpo["precioVenta"] = venta
        prenda = pedir("POST", "/api/v1/prendas", cuerpo)
        ids[nombre] = prenda["id"]
        creadas += 1

        # Stock por sucursal. "Botas de abordaje" queda en 0 a proposito: sirve para ver como se
        # comporta la ruleta con una opcion sin disponibilidad.
        for suc, cant in ((MATRIZ, s1), (NORTE, s2)):
            if cant > 0:
                pedir("POST", f"/api/v1/prendas/{prenda['id']}/grupos-stock",
                      {"sucursalId": suc, "combinacion": [], "cantidadInicial": cant})
    print(f"  prendas: {creadas} creadas, {saltadas} ya existian")
    return ids


# ---------------------------------------------------------------- disfraces

def slot_fijo(orden, nombre, prenda_id, opcional=False):
    return {"orden": orden, "nombre": nombre, "ejePrenda": "FIJA",
            "prendaFijaId": prenda_id, "opcional": opcional}


def slot_opciones(orden, nombre, prenda_ids, opcional=False):
    """Pieza que el cliente elige entre opciones concretas."""
    return {"orden": orden, "nombre": nombre, "ejePrenda": "PERSONALIZABLE",
            "prendasOpcion": prenda_ids, "opcional": opcional}


def slot_pool(orden, nombre, categoria_id, etiquetas=None, opcional=False):
    """Pieza abierta: cualquier prenda de la categoria (opcionalmente filtrada por etiquetas)."""
    pool = {"categoriaId": categoria_id, "etiquetasPermitidas": etiquetas or []}
    return {"orden": orden, "nombre": nombre, "ejePrenda": "PERSONALIZABLE",
            "pool": pool, "opcional": opcional}


def sembrar_disfraces(p, cat_disfraz):
    existentes = {d["nombre"] for d in pedir("GET", "/api/v1/disfraces?tamano=200").get("contenido", [])}

    disfraces = [
        # El caso completo: 5 piezas, con opciones, pool y una pieza opcional. Es el que sirve para
        # evaluar el armado de verdad.
        {
            "nombre": "Pirata del Caribe",
            "categoriaId": cat_disfraz.get("Piratas"),
            "tipo": "RENTA",
            "precioRentaGeneral": 95000,
            "slots": [
                slot_opciones(1, "Sombrero", [p["Tricornio negro"], p["Sombrero pirata rojo"], p["Pañuelo bucanero"]]),
                slot_opciones(2, "Casaca", [p["Casaca de terciopelo vino"], p["Casaca de terciopelo vino L"]]),
                slot_opciones(3, "Pantalón", [p["Pantalón bombacho negro"], p["Pantalón bombacho negro L"]]),
                slot_pool(4, "Botas", CAT["Zapatos"]),
                slot_opciones(5, "Accesorio", [p["Garfio y parche"], p["Cinturón de cuero ancho"]], opcional=True),
            ],
        },
        # Mas simple, para contrastar.
        {
            "nombre": "Bucanero de Tortuga",
            "categoriaId": cat_disfraz.get("Piratas"),
            "tipo": "RENTA",
            "precioRentaGeneral": 62000,
            "slots": [
                slot_opciones(1, "Camisa", [p["Camisa blanca de bucanero"], p["Camisa blanca de bucanero L"]]),
                slot_opciones(2, "Pantalón", [p["Pantalón bombacho negro"], p["Pantalón bombacho negro L"]]),
                slot_fijo(3, "Cinturón", p["Cinturón de cuero ancho"]),
            ],
        },
        # Renta y venta: todas sus piezas sirven para las dos cosas.
        {
            "nombre": "Baile Veneciano",
            "categoriaId": cat_disfraz.get("Época"),
            "tipo": "AMBOS",
            "precioRentaGeneral": 120000,
            "precioVentaGeneral": 720000,
            "slots": [
                slot_opciones(1, "Máscara", [p["Máscara veneciana dorada"], p["Máscara veneciana negra"],
                                             p["Máscara de plumas azul"]]),
                slot_opciones(2, "Vestido", [p["Vestido de gala rojo"], p["Vestido de gala negro"]]),
                slot_opciones(3, "Capa", [p["Capa de terciopelo negra"], p["Capa de terciopelo roja"]]),
                slot_fijo(4, "Guantes", p["Guantes largos de raso"], opcional=True),
            ],
        },
        # Sin tipo: el backend lo deriva de las piezas (deberia quedar en VENTA por la peluca).
        {
            "nombre": "Cortesano de Versalles",
            "categoriaId": cat_disfraz.get("Época"),
            "precioVentaGeneral": 480000,
            "slots": [
                slot_fijo(1, "Peluca", p["Peluca rizada blanca"]),
                slot_opciones(2, "Capa", [p["Capa de terciopelo negra"], p["Capa de terciopelo roja"]]),
            ],
        },
    ]

    creados, saltados = 0, 0
    for d in disfraces:
        if d["nombre"] in existentes:
            saltados += 1
            continue
        d = {k: v for k, v in d.items() if v is not None}
        pedir("POST", "/api/v1/disfraces", d)
        creados += 1
    print(f"  disfraces: {creados} creados, {saltados} ya existian")


def sembrar_categorias_disfraz():
    existentes = {c["nombre"]: c["id"] for c in pedir("GET", "/api/v1/disfraces/categorias")}
    for nombre in ("Piratas", "Época", "Terror", "Infantil"):
        if nombre not in existentes:
            existentes[nombre] = pedir("POST", "/api/v1/disfraces/categorias", {"nombre": nombre})["id"]
    print(f"  categorias de disfraz: {len(existentes)}")
    return existentes


# ---------------------------------------------------------------- clientes

CLIENTES = [
    ("Laura Gómez",     "1020304050", "3001234567", "laura.gomez@correo.co",   "Cra 13 #85-32, Chapinero"),
    ("Andrés Martínez", "1122334455", "3109876543", "andres.m@correo.co",      "Cll 100 #19-54, Usaquén"),
    ("Valentina Ríos",  "1098765432", "3155551212", "valen.rios@correo.co",    "Cra 7 #45-10, Chapinero"),
    ("Carlos Peña",     "80123456",   "3012223344", "carlos.pena@correo.co",   "Cll 63 #24-15, Teusaquillo"),
    ("Mariana Duarte",  "1015998877", "3186667788", "mariana.d@correo.co",     "Av 68 #40-22, Salitre"),
]


def sembrar_clientes():
    existentes = {c["nombre"] for c in pedir("GET", "/api/v1/clientes?tamano=200").get("contenido", [])}
    creados = 0
    for (nombre, doc, tel, email, dir_) in CLIENTES:
        if nombre in existentes:
            continue
        pedir("POST", "/api/v1/clientes", {"nombre": nombre, "documento": doc, "telefono": tel,
                                           "email": email, "direccion": dir_})
        creados += 1
    print(f"  clientes: {creados} creados, {len(CLIENTES) - creados} ya existian")


if __name__ == "__main__":
    entrar()
    print("Sembrando escenario de prueba en la tienda demo...")
    cat_disfraz = sembrar_categorias_disfraz()
    prendas = sembrar_prendas()
    sembrar_disfraces(prendas, cat_disfraz)
    sembrar_clientes()
    print("Listo.")
