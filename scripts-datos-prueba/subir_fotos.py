"""
Genera y sube una ilustracion por prenda y por disfraz de la tienda demo.

No son fotos reales: son siluetas planas generadas, con el color real de la prenda y una forma segun
su categoria. Alcanza para evaluar el diseño —que es lo que se estaba pidiendo—: con el marcador gris
de "sin imagen" no se puede juzgar una grilla ni un carrusel.
"""
import io
import json
import mimetypes
import urllib.request
import uuid

from PIL import Image, ImageDraw

API = "https://just-upliftment-production-cb1f.up.railway.app"

# Paleta por color de la prenda: (fondo suave, color del objeto)
PALETA = {
    "Negro":  ((238, 236, 240), (52, 48, 58)),
    "Rojo":   ((248, 235, 235), (150, 45, 55)),
    "Blanco": ((242, 242, 245), (238, 238, 242)),
    "Azul":   ((233, 240, 248), (44, 78, 128)),
    "Verde":  ((236, 243, 234), (74, 96, 58)),
    "Dorado": ((250, 244, 230), (196, 148, 60)),
}
token = ""


def entrar():
    global token
    datos = json.dumps({"email": "dueno.demo.635935@costumi.test", "password": "Dueno123!"}).encode()
    req = urllib.request.Request(API + "/api/v1/auth/login", data=datos,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        token = json.load(r)["accessToken"]


def get(ruta):
    req = urllib.request.Request(API + ruta, headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def dibujar(forma, color_nombre, etiqueta):
    """Silueta plana de 600x600 segun la categoria de la prenda."""
    fondo, tinta = PALETA.get(color_nombre, PALETA["Negro"])
    img = Image.new("RGB", (600, 600), fondo)
    d = ImageDraw.Draw(img)
    borde = (30, 28, 34)

    if forma == "sombrero":
        d.ellipse([90, 330, 510, 470], fill=tinta, outline=borde, width=3)      # ala
        d.rounded_rectangle([215, 150, 385, 380], 26, fill=tinta, outline=borde, width=3)  # copa
        d.rectangle([200, 330, 400, 372], fill=(200, 170, 90))                  # cinta
    elif forma == "camisa":
        d.polygon([(180, 150), (300, 205), (420, 150), (470, 235), (415, 270),
                   (415, 470), (185, 470), (185, 270), (130, 235)],
                  fill=tinta, outline=borde)
        d.line([(300, 210), (300, 470)], fill=borde, width=3)
    elif forma == "pantalon":
        d.polygon([(200, 140), (400, 140), (400, 480), (320, 480), (300, 280),
                   (280, 480), (200, 480)], fill=tinta, outline=borde)
    elif forma == "zapato":
        d.polygon([(160, 430), (160, 200), (270, 200), (280, 350), (460, 400),
                   (470, 430)], fill=tinta, outline=borde)
        d.rectangle([155, 425, 475, 455], fill=borde)
    elif forma == "vestido":
        d.polygon([(240, 150), (360, 150), (390, 250), (350, 270), (450, 480),
                   (150, 480), (250, 270), (210, 250)], fill=tinta, outline=borde)
    elif forma == "capa":
        d.polygon([(300, 140), (430, 210), (470, 480), (130, 480), (170, 210)],
                  fill=tinta, outline=borde)
        d.ellipse([275, 130, 325, 175], fill=(200, 170, 90), outline=borde, width=2)
    elif forma == "mascara":
        d.ellipse([140, 210, 460, 420], fill=tinta, outline=borde, width=3)
        d.ellipse([200, 275, 265, 320], fill=fondo)
        d.ellipse([335, 275, 400, 320], fill=fondo)
        for x in (300, 250, 350):
            d.polygon([(x, 205), (x - 22, 120), (x + 22, 120)], fill=(196, 148, 60), outline=borde)
    else:  # accesorio
        d.rounded_rectangle([170, 250, 430, 350], 22, fill=tinta, outline=borde, width=3)
        d.rounded_rectangle([275, 235, 325, 365], 10, fill=(200, 170, 90), outline=borde, width=2)

    # Etiqueta discreta abajo, como una etiqueta de sastre cosida.
    d.rounded_rectangle([40, 520, 560, 570], 12, fill=(255, 255, 255))
    d.text((60, 537), etiqueta[:42], fill=(60, 55, 66))
    return img


def forma_de(categoria, nombre):
    n = nombre.lower()
    if "máscara" in n or "mascara" in n:
        return "mascara"
    return {
        "Sombrero": "sombrero", "Camisa": "camisa", "Pantalón": "pantalon",
        "Zapatos": "zapato", "Vestido": "vestido", "Capas de Gala": "capa",
    }.get(categoria, "accesorio")


def color_de(prenda, valores):
    for v in prenda.get("valoresEtiqueta") or []:
        nombre = valores.get(v)
        if nombre in PALETA:
            return nombre
    return "Negro"


def subir(ruta, imagen, nombre_archivo):
    """multipart/form-data a mano: el part se llama 'archivo'."""
    buf = io.BytesIO()
    imagen.save(buf, format="PNG")
    contenido = buf.getvalue()
    limite = "----costumi" + uuid.uuid4().hex
    cuerpo = (
        f"--{limite}\r\n"
        f'Content-Disposition: form-data; name="archivo"; filename="{nombre_archivo}"\r\n'
        f"Content-Type: image/png\r\n\r\n"
    ).encode() + contenido + f"\r\n--{limite}--\r\n".encode()
    req = urllib.request.Request(API + ruta, data=cuerpo, headers={
        "Authorization": "Bearer " + token,
        "Content-Type": f"multipart/form-data; boundary={limite}",
    })
    try:
        with urllib.request.urlopen(req) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return f"{e.code}: {e.read().decode()[:160]}"


if __name__ == "__main__":
    entrar()

    # nombre de cada valor de etiqueta, para saber el color de la prenda
    valores = {}
    for tipo in get("/api/v1/tipos-etiqueta"):
        for v in get(f"/api/v1/tipos-etiqueta/{tipo['id']}/valores"):
            valores[v["id"]] = v["valor"]
    categorias = {c["id"]: c["nombre"] for c in get("/api/v1/categorias")}

    # --- prendas ---
    prendas = get("/api/v1/prendas?tamano=300")["contenido"]
    ok = fallo = saltadas = 0
    for p in prendas:
        if p.get("fotoUrl"):
            saltadas += 1
            continue
        cat = categorias.get(p.get("categoriaId"), "")
        img = dibujar(forma_de(cat, p["nombre"]), color_de(p, valores), p["nombre"])
        r = subir(f"/api/v1/prendas/{p['id']}/foto", img, "prenda.png")
        if r == 200:
            ok += 1
        else:
            fallo += 1
            if fallo <= 2:
                print("   fallo:", p["nombre"], r)
    print(f"  prendas: {ok} con foto nueva, {saltadas} ya tenian, {fallo} fallaron")

    # --- disfraces: se ilustran con la forma de su pieza principal ---
    disfraces = get("/api/v1/disfraces?tamano=200")["contenido"]
    FORMA_DISFRAZ = {
        "Pirata del Caribe": ("sombrero", "Negro"),
        "Bucanero de Tortuga": ("camisa", "Blanco"),
        "Baile Veneciano": ("mascara", "Dorado"),
        "Cortesano de Versalles": ("capa", "Rojo"),
    }
    ok = fallo = saltados = 0
    for d in disfraces:
        if d.get("fotoUrl") or d["nombre"] not in FORMA_DISFRAZ:
            saltados += 1
            continue
        forma, color = FORMA_DISFRAZ[d["nombre"]]
        r = subir(f"/api/v1/disfraces/{d['id']}/foto", dibujar(forma, color, d["nombre"]), "disfraz.png")
        if r == 200:
            ok += 1
        else:
            fallo += 1
            print("   fallo:", d["nombre"], r)
    print(f"  disfraces: {ok} con foto nueva, {saltados} sin cambios, {fallo} fallaron")
