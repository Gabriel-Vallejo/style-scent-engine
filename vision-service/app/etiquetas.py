"""Traducción de los nombres del catálogo (en español, tal y como están en la BD)
a lo que necesitan los modelos: colores de referencia en RGB para el k-means y
prompts en inglés para CLIP, que entiende mucho mejor el inglés.

Todo se busca por nombre normalizado (minúsculas y sin tildes), porque en la BD
ya hubo bugs por tildes ('Ambar Fougere' vs 'Ámbar Fougère').
"""

import unicodedata


def normalizar(nombre: str) -> str:
    sin_tildes = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode("ascii")
    return " ".join(sin_tildes.lower().split())


# Color de referencia (RGB) para cada nombre de color que pueda aparecer en el
# catálogo. Si se añade a la BD un color que no está aquí, se clasifica con CLIP.
COLORES_RGB: dict[str, tuple[int, int, int]] = {
    "negro": (25, 25, 25),
    "blanco": (240, 240, 240),
    "gris": (128, 128, 128),
    "gris claro": (190, 190, 190),
    "gris oscuro": (70, 70, 70),
    "marron": (95, 65, 40),
    "marron claro": (150, 105, 70),
    "camel": (190, 150, 105),
    "beige": (215, 195, 160),
    "crema": (235, 225, 200),
    "caqui": (140, 130, 90),
    "verde oliva": (95, 100, 50),
    "verde": (50, 130, 70),
    "azul": (40, 85, 170),
    "azul claro": (130, 175, 220),
    "azul marino": (25, 35, 70),
    "vaquero": (75, 100, 140),
    "rojo": (185, 35, 40),
    "burdeos": (110, 25, 40),
    "rosa": (230, 160, 175),
    "naranja": (230, 125, 40),
    "amarillo": (235, 205, 60),
    "morado": (100, 55, 135),
    "mostaza": (205, 160, 40),
    "turquesa": (40, 175, 170),
    "lila": (190, 160, 210),
    "coral": (240, 120, 100),
    "verde botella": (20, 70, 45),
    "terracota": (190, 95, 65),
    "fucsia": (215, 40, 135),
    "rosa palo": (225, 190, 190),
    "salmon": (245, 165, 135),
    "granate": (125, 20, 30),
    "berenjena": (70, 30, 60),
    "lavanda": (200, 190, 230),
    "azul electrico": (40, 90, 230),
    "azul indigo": (45, 40, 110),
    "azul petroleo": (25, 85, 100),
    "verde menta": (165, 220, 190),
    "verde lima": (170, 205, 50),
    "amarillo pastel": (245, 235, 160),
    "ocre": (195, 135, 45),
    "chocolate": (65, 40, 25),
}
# Colores descartados por quedar a menos de ΔE 12 de uno existente (se confundirían):
# antracita (gris oscuro), arena (beige), azul cielo (azul claro), cobre y teja
# (terracota), gris perla (gris claro), hueso (crema), topo (gris),
# verde militar (verde oliva), vino (burdeos).

# Prompts en inglés por categoría. Varios por etiqueta ("prompt ensembling"):
# se promedian sus embeddings, lo que da resultados más estables que uno solo.
PROMPTS_CATEGORIA: dict[str, list[str]] = {
    "torso": [
        "a photo of a shirt",
        "a photo of a t-shirt",
        "a photo of a jacket",
        "a photo of a hoodie",
        "a photo of a sweater",
        "a photo of a coat",
    ],
    "piernas": [
        "a photo of pants",
        "a photo of jeans",
        "a photo of trousers",
        "a photo of shorts",
        "a photo of a skirt",
    ],
    "calzado": [
        "a photo of shoes",
        "a photo of sneakers",
        "a photo of boots",
        "a photo of sandals",
    ],
    "accesorio": [
        "a photo of a cap",
        "a photo of a hat",
        "a photo of sunglasses",
        "a photo of glasses",
        "a photo of a watch",
        "a photo of a bag",
        "a photo of a belt",
        "a photo of a necklace",
    ],
}

PROMPTS_ESTILO: dict[str, list[str]] = {
    "streetwear": [
        "a photo of streetwear clothing",
        "urban streetwear fashion",
        "hip hop street style clothing",
    ],
    "casual": [
        "a photo of casual everyday clothing",
        "simple casual outfit",
    ],
    "formal": [
        "a photo of formal elegant clothing",
        "a formal suit outfit",
    ],
    "elegante": [
        "a photo of elegant clothing",
        "classy smart outfit",
    ],
    "deportivo": [
        "a photo of sportswear",
        "athletic gym clothing",
    ],
    "clasico": [
        "a photo of classic timeless clothing",
    ],
    "minimalista": [
        "a photo of minimalist clothing in plain neutral colors",
        "clean simple minimal fashion without logos",
    ],
    "vintage": [
        "a photo of vintage retro clothing",
        "old-fashioned 70s 80s 90s style clothes",
    ],
    "rockero": [
        "a photo of rock style clothing",
        "punk rock leather and studs fashion",
        "heavy metal band clothing",
    ],
    "preppy": [
        "a photo of preppy clothing",
        "ivy league polo shirt chinos and loafers",
    ],
    "workwear": [
        "a photo of workwear clothing",
        "rugged work jacket, canvas pants and work boots",
    ],
    "techwear": [
        "a photo of techwear clothing",
        "technical waterproof black urban outdoor gear",
    ],
    "bohemio": [
        "a photo of bohemian boho clothing",
        "flowy boho hippie outfit with fringe and earthy prints",
    ],
    "gotico": [
        "a photo of gothic clothing",
        "all black goth outfit with dark lace and chains",
    ],
    "skater": [
        "a photo of skater clothing",
        "skateboarding outfit with baggy pants, graphic tee and vans",
    ],
    "militar": [
        "a photo of military style clothing",
        "camouflage cargo army inspired outfit",
    ],
    "surfero": [
        "a photo of surf style clothing",
        "beach surfer outfit with board shorts and hawaiian shirt",
    ],
    "western": [
        "a photo of western cowboy clothing",
        "cowboy boots, denim and western shirt outfit",
    ],
    "y2k": [
        "a photo of y2k fashion",
        "early 2000s style outfit with low rise jeans and baby tee",
    ],
    "gorpcore": [
        "a photo of gorpcore outdoor clothing",
        "hiking fleece, trail shoes and outdoor jacket outfit",
    ],
    "grunge": [
        "a photo of grunge clothing",
        "90s grunge outfit with flannel shirt and ripped jeans",
    ],
    "nautico": [
        "a photo of nautical clothing",
        "navy and white striped sailor boat outfit",
    ],
    "harajuku": [
        "a photo of harajuku japanese street fashion",
        "colorful layered tokyo street style outfit",
    ],
    "romantico": [
        "a photo of romantic clothing",
        "soft pastel outfit with ruffles and lace",
    ],
}
# Estilos descartados por solaparse con uno existente (CLIP los confundiría):
# formal y clásico (elegante), punk (rockero), hip hop (streetwear),
# athleisure (deportivo), old money (preppy).


def prompts_categoria(nombre: str) -> list[str]:
    return PROMPTS_CATEGORIA.get(normalizar(nombre), [f"a photo of {nombre}"])


def prompts_estilo(nombre: str) -> list[str]:
    return PROMPTS_ESTILO.get(normalizar(nombre), [f"a photo of {nombre} style clothing"])


def prompts_color(nombre: str) -> list[str]:
    return [f"a photo of a {nombre} piece of clothing"]
