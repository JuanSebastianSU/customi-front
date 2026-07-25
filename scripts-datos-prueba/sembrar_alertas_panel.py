"""
Deja el panel (G1) con mas de una alerta para poder verlas juntas: pide un reembolso como cliente
sobre una de sus rentas, de modo que la tienda tenga una solicitud PENDIENTE por responder.
"""
import json
import sys
import urllib.request
import urllib.error

API = "https://just-upliftment-production-cb1f.up.railway.app"
DUENO = ("dueno.demo.635935@costumi.test", "Dueno123!")


def entrar(email, password):
    datos = json.dumps({"email": email, "password": password}).encode()
    req = urllib.request.Request(API + "/api/v1/auth/login", data=datos,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)["accessToken"]


def pedir(token, metodo, ruta, cuerpo=None):
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


def main():
    token = entrar(*DUENO)
    pagina = pedir(token, "GET", "/api/v1/rentas?pagina=0&tamano=20")
    rentas = pagina.get("contenido") or []
    # Una renta ya cobrable de la tienda; el reembolso queda PENDIENTE de responder.
    renta = next((r for r in rentas if r.get("importe")), None)
    if renta is None:
        raise SystemExit("No hay rentas para pedir un reembolso")
    ya = pedir(token, "GET", "/api/v1/reembolsos?pagina=0&tamano=100")
    pendientes = [s for s in (ya.get("contenido") or []) if s.get("estado") == "PENDIENTE"]
    if pendientes:
        print(f"Ya hay {len(pendientes)} reembolso(s) pendiente(s); no se crea otro.")
        return
    sol = pedir(token, "POST", "/api/v1/reembolsos", {
        "tipoConcepto": "RENTA",
        "conceptoId": renta["id"],
        "monto": 10,  # el backend exige que no supere lo ya pagado en esa renta
        "motivo": "Prueba del panel: solicitud pendiente de responder",
    })
    print(f"Solicitud {sol['id']} sobre la renta {renta['codigoRetiro']}: {sol['estado']}")


if __name__ == "__main__":
    sys.exit(main())
