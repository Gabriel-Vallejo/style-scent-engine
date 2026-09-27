"""Color dominante de una prenda: OpenCV separa la prenda del fondo (GrabCut) y
k-means (scikit-learn) agrupa sus píxeles; el grupo más grande es el color
dominante, que se compara en espacio Lab con los colores del catálogo.

Lab en vez de RGB porque en Lab la distancia se parece a la diferencia de color
que percibe el ojo (ΔE), así "el más cercano" tiene sentido. Para agrupar píxeles
basta la distancia euclídea (CIE76); para compararlos con el catálogo se usa
CIEDE2000 con el factor textil kL = 2 (ver puntuar_colores).
"""

import math

import cv2
import numpy as np
from sklearn.cluster import KMeans

from .etiquetas import COLORES_RGB, normalizar

LADO_MAXIMO = 256
MARGEN_FONDO = 0.05  # GrabCut asume que el borde exterior de la foto es fondo
MAX_PIXELES_KMEANS = 5000
NUM_CLUSTERS = 3
# Por debajo de este ΔE dos grupos se consideran el mismo color (≈ diferencia apenas perceptible)
UMBRAL_FUSION = 12.0
# Escala de la confianza: con ΔE00 = 7 la similitud cae a ~37 % de la de un match perfecto
ESCALA_DELTA_E = 7.0
# Factor de luminosidad de CIEDE2000 para textiles (ISO 105-J03 / CMC usan 2): la
# luz y las sombras de una foto cambian mucho más la luminosidad que el tono
KL_TEXTIL = 2.0


def decodificar(datos: bytes) -> np.ndarray:
    """Imagen BGR, o BGRA si trae transparencia (PNG recortado): el canal alfa es
    la mejor máscara posible de la prenda y no hay que perderlo."""
    imagen = cv2.imdecode(np.frombuffer(datos, np.uint8), cv2.IMREAD_UNCHANGED)
    if imagen is None:
        raise ValueError("El archivo no es una imagen válida")
    if imagen.dtype != np.uint8:
        imagen = cv2.convertScaleAbs(imagen, alpha=255.0 / np.iinfo(imagen.dtype).max)
    if imagen.ndim == 2:
        return cv2.cvtColor(imagen, cv2.COLOR_GRAY2BGR)
    if imagen.shape[2] == 4 and imagen[:, :, 3].min() == 255:
        return imagen[:, :, :3]  # canal alfa presente pero todo opaco: no aporta nada
    return imagen


def sin_transparencia(imagen: np.ndarray) -> np.ndarray:
    """Para los modelos que esperan BGR: compone la transparencia sobre fondo blanco."""
    if imagen.shape[2] == 3:
        return imagen
    alfa = imagen[:, :, 3:4].astype(np.float32) / 255.0
    return (imagen[:, :, :3] * alfa + 255 * (1 - alfa)).astype(np.uint8)


def _reducir(imagen: np.ndarray) -> np.ndarray:
    alto, ancho = imagen.shape[:2]
    escala = LADO_MAXIMO / max(alto, ancho)
    if escala >= 1:
        return imagen
    return cv2.resize(imagen, (round(ancho * escala), round(alto * escala)), interpolation=cv2.INTER_AREA)


def mascara_prenda(imagen: np.ndarray) -> np.ndarray:
    """Máscara booleana con los píxeles de la prenda: el canal alfa si lo hay,
    si no lo que GrabCut considera primer plano."""
    if imagen.shape[2] == 4:
        return imagen[:, :, 3] > 127
    alto, ancho = imagen.shape[:2]
    mx, my = max(1, int(ancho * MARGEN_FONDO)), max(1, int(alto * MARGEN_FONDO))
    rect = (mx, my, ancho - 2 * mx, alto - 2 * my)

    mascara = np.zeros((alto, ancho), np.uint8)
    fondo_modelo = np.zeros((1, 65), np.float64)
    prenda_modelo = np.zeros((1, 65), np.float64)
    try:
        cv2.grabCut(imagen, mascara, rect, fondo_modelo, prenda_modelo, 3, cv2.GC_INIT_WITH_RECT)
        primer_plano = (mascara == cv2.GC_FGD) | (mascara == cv2.GC_PR_FGD)
    except cv2.error:
        primer_plano = np.zeros((alto, ancho), bool)

    # Si GrabCut apenas encuentra prenda (foto muy recortada, fondo parecido...),
    # nos quedamos con el centro de la imagen, que es donde suele estar
    if primer_plano.mean() < 0.05:
        primer_plano = np.zeros((alto, ancho), bool)
        primer_plano[alto // 5: alto - alto // 5, ancho // 5: ancho - ancho // 5] = True
    return primer_plano


def _a_lab(rgb: np.ndarray) -> np.ndarray:
    """RGB 0-255 (N x 3) -> Lab (N x 3), con L en 0-100 y a/b en -127..127."""
    bgr = rgb[:, ::-1].astype(np.float32).reshape(-1, 1, 3) / 255.0
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2Lab).reshape(-1, 3)


def color_dominante(imagen: np.ndarray) -> tuple[tuple[int, int, int], float]:
    """Devuelve el color dominante de la prenda (RGB) y qué fracción de la prenda ocupa."""
    imagen = _reducir(imagen)
    pixeles_bgr = imagen[mascara_prenda(imagen)][:, :3]
    pixeles_rgb = pixeles_bgr[:, ::-1]

    if len(pixeles_rgb) > MAX_PIXELES_KMEANS:
        indices = np.random.default_rng(0).choice(len(pixeles_rgb), MAX_PIXELES_KMEANS, replace=False)
        pixeles_rgb = pixeles_rgb[indices]

    lab = _a_lab(pixeles_rgb)
    clusters = min(NUM_CLUSTERS, len(np.unique(lab, axis=0)))
    kmeans = KMeans(n_clusters=clusters, n_init=4, random_state=0).fit(lab)

    grupos = _fusionar_similares(kmeans.cluster_centers_, kmeans.labels_)
    tamanos = np.bincount(grupos)
    mayor = int(tamanos.argmax())
    # Media en RGB de los píxeles del grupo (evita convertir el centroide de Lab a RGB)
    rgb_medio = pixeles_rgb[grupos == mayor].mean(axis=0)
    rgb = tuple(int(round(c)) for c in rgb_medio)
    return rgb, float(tamanos[mayor] / tamanos.sum())


def _fusionar_similares(centros: np.ndarray, etiquetas: np.ndarray) -> np.ndarray:
    """k-means siempre hace NUM_CLUSTERS grupos, aunque la prenda sea de un solo
    color: con ruido, arrugas o sombras parte ese color en grupos casi iguales y
    el "dominante" se queda con una fracción. Se fusionan los que están a menos
    de UMBRAL_FUSION de ΔE, es decir, los que el ojo ve como el mismo color."""
    grupo_de = list(range(len(centros)))
    for i in range(len(centros)):
        for j in range(i):
            if np.linalg.norm(centros[i] - centros[j]) < UMBRAL_FUSION:
                grupo_de[i] = grupo_de[j]
                break
    return np.array(grupo_de)[etiquetas]


def ciede2000(lab: np.ndarray, referencias: np.ndarray, kL: float = 1.0) -> np.ndarray:
    """ΔE00 entre un color Lab (3,) y varios (N, 3). Fórmula de Sharma, Wu y Dalal
    (2005). Con CIE76 un verde oliva apagado quedaba más cerca de un gris que de
    un verde, porque la luminosidad pesaba tanto como el tono."""
    L1, a1, b1 = lab
    L2, a2, b2 = referencias[:, 0], referencias[:, 1], referencias[:, 2]
    c_media = (np.hypot(a1, b1) + np.hypot(a2, b2)) / 2
    g = 0.5 * (1 - np.sqrt(c_media**7 / (c_media**7 + 25.0**7)))
    a1p, a2p = (1 + g) * a1, (1 + g) * a2
    c1p, c2p = np.hypot(a1p, b1), np.hypot(a2p, b2)
    h1p = np.degrees(np.arctan2(b1, a1p)) % 360
    h2p = np.degrees(np.arctan2(b2, a2p)) % 360

    d_l = L2 - L1
    d_c = c2p - c1p
    dh = h2p - h1p
    sin_croma = c1p * c2p == 0
    dh = np.where(sin_croma, 0, np.where(dh > 180, dh - 360, np.where(dh < -180, dh + 360, dh)))
    d_h = 2 * np.sqrt(c1p * c2p) * np.sin(np.radians(dh / 2))

    l_media = (L1 + L2) / 2
    cp_media = (c1p + c2p) / 2
    suma_h = h1p + h2p
    h_media = np.where(sin_croma, suma_h,
                       np.where(np.abs(h1p - h2p) <= 180, suma_h / 2,
                                np.where(suma_h < 360, (suma_h + 360) / 2, (suma_h - 360) / 2)))
    t = (1 - 0.17 * np.cos(np.radians(h_media - 30)) + 0.24 * np.cos(np.radians(2 * h_media))
         + 0.32 * np.cos(np.radians(3 * h_media + 6)) - 0.20 * np.cos(np.radians(4 * h_media - 63)))
    d_theta = 30 * np.exp(-(((h_media - 275) / 25) ** 2))
    r_c = 2 * np.sqrt(cp_media**7 / (cp_media**7 + 25.0**7))
    s_l = 1 + (0.015 * (l_media - 50) ** 2) / np.sqrt(20 + (l_media - 50) ** 2)
    s_c = 1 + 0.045 * cp_media
    s_h = 1 + 0.015 * cp_media * t
    r_t = -np.sin(np.radians(2 * d_theta)) * r_c

    return np.sqrt((d_l / (kL * s_l)) ** 2 + (d_c / s_c) ** 2 + (d_h / s_h) ** 2
                   + r_t * (d_c / s_c) * (d_h / s_h))


def puntuar_colores(rgb: tuple[int, int, int], nombres: list[str]) -> list[tuple[str, float]] | None:
    """Confianza de cada color del catálogo según su cercanía (ΔE00 textil) al color
    detectado, ordenado de más a menos probable. None si ningún nombre del
    catálogo tiene color de referencia (entonces decide CLIP)."""
    conocidos = [n for n in nombres if normalizar(n) in COLORES_RGB]
    if not conocidos:
        return None

    detectado = _a_lab(np.array([rgb]))[0]
    referencias = _a_lab(np.array([COLORES_RGB[normalizar(n)] for n in conocidos]))
    distancias = ciede2000(detectado, referencias, kL=KL_TEXTIL)

    similitudes = np.exp(-distancias / ESCALA_DELTA_E)
    total = similitudes.sum()
    if total == 0 or math.isnan(total):
        # Todo lejísimos: gana el más cercano sin confianza real
        confianzas = np.zeros(len(conocidos))
        confianzas[distancias.argmin()] = 1.0
    else:
        confianzas = similitudes / total

    # Los colores sin referencia no se pueden comparar: confianza 0, al final
    desconocidos = [(n, 0.0) for n in nombres if n not in conocidos]
    ranking = sorted(zip(conocidos, confianzas.tolist()), key=lambda x: x[1], reverse=True)
    return ranking + desconocidos


def a_hex(rgb: tuple[int, int, int]) -> str:
    return "#{:02x}{:02x}{:02x}".format(*rgb)
