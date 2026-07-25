"""
Parche puntual: las prendas de la tienda demo se sembraron sin etiquetas (el seed usaba un campo mal
nombrado, ya corregido en sembrar_datos.py). Este script les asigna su Color y Talla via PUT, reenviando
sus valores actuales para no perder precios ni multas. Idempotente: si la prenda ya tiene etiquetas, la
salta.

Resuelve los ids de tipo/valor EN VIVO desde la taxonomia (por nombre), no de constantes hardcodeadas:
algunos ids del seed estaban obsoletos y provocaban 400 "el valor no pertenece al tipo".
"""
import json
import urllib.request
import urllib.error

from sembrar_datos import PRENDAS, API

token = ""


def entrar():
    global token
    datos = json.dumps({"email": "dueno.demo.635935@costumi.test", "password": "Dueno123!"}).encode()
    req = urllib.request.Request(API + "/api/v1/auth/login", data=datos,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        token = json.load(r)["accessToken"]


def pedir(metodo, ruta, cuerpo=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo, headers={
        "Authorization": "Bearer " + token, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode()
            return json.loads(texto) if texto else None
    except urllib.error.HTTPError as e:
        return {"__error__": e.code, "detalle": e.read().decode()[:180]}


def taxonomia():
    """Devuelve (tipoColorId, {colorNombre:id}), (tipoTallaId, {tallaNombre:id}) leidos en vivo."""
    tipos = {t["nombre"]: t["id"] for t in pedir("GET", "/api/v1/tipos-etiqueta")}
    color_id, talla_id = tipos.get("Color"), tipos.get("Talla")
    colores = {v["valor"]: v["id"] for v in pedir("GET", f"/api/v1/tipos-etiqueta/{color_id}/valores")}
    tallas = {v["valor"]: v["id"] for v in pedir("GET", f"/api/v1/tipos-etiqueta/{talla_id}/valores")}
    return color_id, colores, talla_id, tallas


if __name__ == "__main__":
    entrar()
    tipo_color, colores, tipo_talla, tallas = taxonomia()
    print(f"Color={tipo_color} valores={list(colores)}")
    print(f"Talla={tipo_talla} valores={list(tallas)}\n")

    color_talla = {p[0]: (p[5], p[6]) for p in PRENDAS}  # nombre -> (color, talla)
    actuales = {p["nombre"]: p for p in pedir("GET", "/api/v1/prendas?tamano=300")["contenido"]}

    parchadas = ya = fallos = 0
    for nombre, prenda in actuales.items():
        if nombre not in color_talla:
            continue
        if prenda.get("etiquetas"):
            ya += 1
            continue
        color, talla = color_talla[nombre]
        etiquetas = []
        if color in colores:
            etiquetas.append({"tipoEtiquetaId": tipo_color, "valorEtiquetaId": colores[color]})
        if talla in tallas:
            etiquetas.append({"tipoEtiquetaId": tipo_talla, "valorEtiquetaId": tallas[talla]})
        cuerpo = {
            "nombre": prenda["nombre"],
            "precioRenta": prenda.get("precioRenta"),
            "precioVenta": prenda.get("precioVenta"),
            "costoAdquisicion": prenda.get("costoAdquisicion"),
            "depositoSugerido": prenda.get("depositoSugerido"),
            "valorReposicion": prenda.get("valorReposicion"),
            "valorDano": prenda.get("valorDano"),
            "etiquetas": etiquetas,
        }
        r = pedir("PUT", f"/api/v1/prendas/{prenda['id']}", cuerpo)
        if isinstance(r, dict) and r.get("__error__"):
            fallos += 1
            print(f"  ! {nombre} ({color}/{talla}): {r['__error__']} {r['detalle']}")
        else:
            parchadas += 1
            print(f"  + {nombre}: {color} / Talla {talla}")
    print(f"\nparchadas: {parchadas}, ya tenian: {ya}, fallos: {fallos}")
