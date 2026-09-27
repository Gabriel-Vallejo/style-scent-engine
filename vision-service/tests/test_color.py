import cv2
import numpy as np
import pytest

from app import color
from app.etiquetas import COLORES_RGB

CATALOGO = ["Crema", "Marrón", "Negro"]


def foto_sintetica(rgb_prenda, rgb_fondo=(245, 245, 245), ruido=12):
    """Prenda (elipse) de un color sobre un fondo, con ruido para que no sea trivial."""
    rng = np.random.default_rng(42)
    img = np.full((400, 300, 3), rgb_fondo[::-1], np.uint8)
    cv2.ellipse(img, (150, 200), (100, 150), 0, 0, 360, rgb_prenda[::-1], -1)
    img = img.astype(np.int16) + rng.integers(-ruido, ruido, img.shape)
    return np.clip(img, 0, 255).astype(np.uint8)


@pytest.mark.parametrize(
    "rgb_prenda, rgb_fondo, esperado",
    [
        ((100, 68, 42), (245, 245, 245), "Marrón"),   # chaqueta marrón sobre fondo blanco
        ((22, 22, 25), (245, 245, 245), "Negro"),      # pantalón negro sobre fondo blanco
        ((232, 222, 196), (40, 40, 45), "Crema"),      # gafas crema sobre fondo oscuro
        ((232, 222, 196), (200, 200, 205), "Crema"),   # crema sobre gris claro (poco contraste)
    ],
)
def test_detecta_el_color_de_la_prenda_y_no_el_del_fondo(rgb_prenda, rgb_fondo, esperado):
    rgb, proporcion = color.color_dominante(foto_sintetica(rgb_prenda, rgb_fondo))

    ranking = color.puntuar_colores(rgb, CATALOGO)

    assert ranking[0][0] == esperado
    assert proporcion > 0.5
    assert sum(c for _, c in ranking) == pytest.approx(1.0)


def test_puntuar_colores_ignora_tildes_y_mayusculas():
    ranking = color.puntuar_colores(COLORES_RGB["marron"], ["MARRON", "negro"])
    assert ranking[0][0] == "MARRON"


def test_puntuar_colores_sin_referencias_devuelve_none():
    assert color.puntuar_colores((10, 10, 10), ["Tornasolado", "Holográfico"]) is None


def test_color_sin_referencia_va_al_final_con_confianza_cero():
    ranking = color.puntuar_colores((20, 20, 20), ["Negro", "Tornasolado"])
    assert ranking[-1] == ("Tornasolado", 0.0)
    assert ranking[0][0] == "Negro"


def test_decodificar_rechaza_datos_que_no_son_imagen():
    with pytest.raises(ValueError):
        color.decodificar(b"esto no es una imagen")


def test_png_transparente_usa_el_alfa_como_mascara():
    # Gorra negra recortada; los píxeles transparentes son blancos "por debajo"
    img = np.full((200, 200, 4), (255, 255, 255, 0), np.uint8)
    cv2.circle(img, (100, 100), 60, (22, 22, 22, 255), -1)
    datos = cv2.imencode(".png", img)[1].tobytes()

    imagen = color.decodificar(datos)
    rgb, proporcion = color.color_dominante(imagen)

    assert imagen.shape[2] == 4
    assert color.puntuar_colores(rgb, CATALOGO)[0][0] == "Negro"
    assert proporcion > 0.95
    assert color.sin_transparencia(imagen).shape[2] == 3


@pytest.mark.parametrize(
    "lab1, lab2, esperado",
    [
        ((50, 2.6772, -79.7751), (50, 0, -82.7485), 2.0425),
        ((50, 2.5, 0), (50, 0, -2.5), 4.3065),
        ((60.2574, -34.0099, 36.2677), (60.4626, -34.1751, 39.4387), 1.2644),
        ((22.7233, 20.0904, -46.6940), (23.0331, 14.9730, -42.5619), 2.0373),
    ],
)
def test_ciede2000_coincide_con_los_pares_de_referencia(lab1, lab2, esperado):
    # Pares publicados por Sharma, Wu y Dalal (2005) para validar implementaciones
    assert color.ciede2000(np.array(lab1), np.array([lab2]))[0] == pytest.approx(esperado, abs=1e-3)


@pytest.mark.parametrize("rgb_prenda", [(95, 100, 50), (100, 102, 70), (105, 105, 80)])
def test_verde_oliva_apagado_no_se_confunde_con_gris(rgb_prenda):
    # Con CIE76 los olivas poco saturados quedaban más cerca de un gris
    catalogo = ["Verde oliva", "Caqui", "Gris", "Gris oscuro", "Gris claro", "Negro", "Blanco"]
    rgb, _ = color.color_dominante(foto_sintetica(rgb_prenda))

    ranking = color.puntuar_colores(rgb, catalogo)

    assert ranking[0][0] in ("Verde oliva", "Caqui")
    assert not ranking[0][0].startswith("Gris")
