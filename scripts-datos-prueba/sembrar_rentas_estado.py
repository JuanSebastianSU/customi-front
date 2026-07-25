"""
Siembra rentas en los estados que la lista de gestion (G9) tiene que distinguir: por entregar,
activa en fecha, activa VENCIDA y devuelta por cerrar. Sin una vencida no se puede comprobar que lo
urgente se vea como urgente, que es el punto del rediseno de esa pantalla.

Idempotente: usa una clave de idempotencia fija por caso, asi que correrlo dos veces no duplica.
"""
import json
import sys
import urllib.request
import urllib.error
from datetime import date, timedelta

API = "https://just-upliftment-production-cb1f.up.railway.app"
MATRIZ = "d48c1f49-0a0f-4ba3-bf0f-cae3c8dea9ca"

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
        raise RuntimeError(f"{metodo} {ruta} -> {e.code}: {e.read().decode()[:300]}") from None


def entrar():
    global token
    datos = json.dumps({"email": "dueno.demo.635935@costumi.test", "password": "Dueno123!"}).encode()
    req = urllib.request.Request(API + "/api/v1/auth/login", data=datos,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        token = json.load(r)["accessToken"]


def prenda_con_stock():
    """La primera prenda rentable con unidades libres en Casa Matriz."""
    catalogo = pedir("GET", "/api/v1/prendas/catalogo")
    for p in catalogo:
        if p.get("tipoArticulo") in ("RENTA", "AMBOS") and (p.get("unidadesDisponibles") or 0) >= 4:
            return p
    raise SystemExit("No hay ninguna prenda de renta con stock suficiente")


def cliente():
    pagina = pedir("GET", "/api/v1/clientes?pagina=0&tamano=1")
    contenido = pagina.get("contenido") or []
    if contenido:
        return contenido[0]["id"]
    return pedir("POST", "/api/v1/clientes", {"nombre": "Cliente de prueba G9"})["id"]


def crear(prenda, cli, retiro, devolucion, clave):
    return pedir("POST", "/api/v1/rentas", {
        "sucursalId": MATRIZ,
        "clienteId": cli,
        "prendaId": prenda["id"],
        "precioPorDia": float(prenda.get("precioRenta") or 20000),
        "fechaRetiro": retiro.isoformat(),
        "fechaDevolucion": devolucion.isoformat(),
        "deposito": 50000,
        "claveIdempotencia": clave,
    })


def main():
    entrar()
    p = prenda_con_stock()
    cli = cliente()
    hoy = date.today()
    print(f"Prenda: {p['nombre']}  ({p.get('unidadesDisponibles')} disponibles)")

    casos = [
        ("por entregar", hoy + timedelta(days=2), hoy + timedelta(days=5), None),
        ("activa en fecha", hoy - timedelta(days=1), hoy + timedelta(days=6), "entregar"),
        ("VENCIDA", hoy - timedelta(days=9), hoy - timedelta(days=3), "entregar"),
        ("por cerrar", hoy - timedelta(days=8), hoy - timedelta(days=6), "devolver"),
    ]
    for nombre, retiro, devolucion, paso in casos:
        # Sufijo opcional (argv[1]) para volver a sembrar el escenario cuando las rentas anteriores
        # ya avanzaron de estado por las pruebas.
        sufijo = sys.argv[1] if len(sys.argv) > 1 else ""
        clave = f"G9-{nombre.lower().replace(' ', '-')}{sufijo}"
        try:
            renta = crear(p, cli, retiro, devolucion, clave)
        except RuntimeError as e:
            # Si esa prenda ya no tiene unidades libres en esas fechas, se salta el caso en vez de
            # abortar el resto del escenario.
            print(f"  {nombre:16} sin disponibilidad, se salta")
            continue
        estado = renta["estado"]
        if paso and estado == "RESERVADA":
            renta = pedir("POST", f"/api/v1/rentas/{renta['id']}/entregar")
            if paso == "devolver":
                renta = pedir("POST", f"/api/v1/rentas/{renta['id']}/devolver")
        print(f"  {nombre:16} {renta['codigoRetiro']}  {retiro} -> {devolucion}  {renta['estado']}")


if __name__ == "__main__":
    sys.exit(main())
