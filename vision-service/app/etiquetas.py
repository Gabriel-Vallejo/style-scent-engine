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
}

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
}


def prompts_categoria(nombre: str) -> list[str]:
    return PROMPTS_CATEGORIA.get(normalizar(nombre), [f"a photo of {nombre}"])


def prompts_estilo(nombre: str) -> list[str]:
    return PROMPTS_ESTILO.get(normalizar(nombre), [f"a photo of {nombre} style clothing"])


def prompts_color(nombre: str) -> list[str]:
    return [f"a photo of a {nombre} piece of clothing"]
