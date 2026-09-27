import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.main import app


class ClasificadorFalso:
    """Devuelve probabilidades fijas por nombre, sin cargar CLIP."""

    nombre_modelo = "falso"
    dispositivo = "cpu"

    def __init__(self, probabilidades):
        self.probabilidades = probabilidades
        self.llamadas = []

    def clasificar(self, imagen, etiquetas):
        self.llamadas.append(etiquetas)
        ranking = [(n, self.probabilidades.get(n, 0.0)) for n in etiquetas]
        return sorted(ranking, key=lambda x: x[1], reverse=True)


@pytest.fixture
def cliente():
    falso = ClasificadorFalso({"Torso": 0.8, "Piernas": 0.2, "Streetwear": 0.6, "Casual": 0.4})
    app.state.clasificador = falso
    with TestClient(app) as c:
        c.falso = falso
        yield c
    app.state.clasificador = None


def jpg(rgb=(100, 68, 42)):
    img = np.full((200, 200, 3), 245, np.uint8)
    cv2.rectangle(img, (40, 40), (160, 160), rgb[::-1], -1)
    return cv2.imencode(".jpg", img)[1].tobytes()


def formulario(**extra):
    datos = {"categorias": ["Torso", "Piernas"], "colores": ["Crema", "Marrón", "Negro"], "estilos": ["Streetwear", "Casual"]}
    datos.update(extra)
    return datos


def test_analizar_devuelve_sugerencias_ordenadas(cliente):
    r = cliente.post("/analizar", files={"imagen": ("f.jpg", jpg(), "image/jpeg")}, data=formulario())

    assert r.status_code == 200
    cuerpo = r.json()
    assert cuerpo["categorias"][0] == {"nombre": "Torso", "confianza": 0.8}
    assert cuerpo["colores"][0]["nombre"] == "Marrón"
    assert cuerpo["metodoColor"] == "kmeans"
    assert cuerpo["colorDominanteHex"].startswith("#")
    # Streetwear es el más probable y Casual supera el umbral: los dos se sugieren
    assert cuerpo["estilosSugeridos"] == ["Streetwear", "Casual"]


def test_estilo_por_debajo_del_umbral_no_se_sugiere(cliente):
    cliente.falso.probabilidades.update({"Streetwear": 0.9, "Casual": 0.1})

    r = cliente.post("/analizar", files={"imagen": ("f.jpg", jpg(), "image/jpeg")}, data=formulario())

    assert r.json()["estilosSugeridos"] == ["Streetwear"]


def test_si_ningun_color_tiene_referencia_decide_clip(cliente):
    cliente.falso.probabilidades.update({"Tornasolado": 0.7, "Holográfico": 0.3})

    r = cliente.post("/analizar", files={"imagen": ("f.jpg", jpg(), "image/jpeg")},
                     data=formulario(colores=["Tornasolado", "Holográfico"]))

    assert r.json()["metodoColor"] == "clip"
    assert r.json()["colores"][0]["nombre"] == "Tornasolado"


def test_categoria_desconocida_usa_prompt_generico(cliente):
    cliente.post("/analizar", files={"imagen": ("f.jpg", jpg(), "image/jpeg")},
                 data=formulario(categorias=["Torso", "Ropa interior"]))

    prompts_categorias = cliente.falso.llamadas[0]
    assert prompts_categorias["Ropa interior"] == ["a photo of Ropa interior"]
    assert "a photo of a jacket" in prompts_categorias["Torso"]


def test_archivo_que_no_es_imagen_devuelve_400(cliente):
    r = cliente.post("/analizar", files={"imagen": ("f.txt", b"hola", "text/plain")}, data=formulario())
    assert r.status_code == 400


def test_sin_catalogo_devuelve_422(cliente):
    r = cliente.post("/analizar", files={"imagen": ("f.jpg", jpg(), "image/jpeg")}, data={"categorias": ["Torso"]})
    assert r.status_code == 422


def test_salud(cliente):
    assert cliente.get("/salud").json() == {"estado": "ok", "modelo": "falso", "dispositivo": "cpu"}
