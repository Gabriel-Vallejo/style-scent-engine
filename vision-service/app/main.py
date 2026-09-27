"""Microservicio de visión de Style & Scent Engine.

Recibe la foto de una prenda junto con los nombres del catálogo (los manda el
backend de Spring, que es quien conoce la BD) y devuelve sugerencias con su
confianza. No guarda nada ni necesita acceso a la base de datos.
"""

from contextlib import asynccontextmanager
from typing import Annotated

import cv2
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from PIL import Image
from pydantic import BaseModel

from . import color as detector_color
from .clasificador import ClasificadorClip
from .etiquetas import prompts_categoria, prompts_color, prompts_estilo

TAMANO_MAXIMO = 10 * 1024 * 1024
# Una prenda puede tener varios estilos (M:N): además del más probable se sugieren
# los que tengan al menos la mitad de su probabilidad. Relativo y no fijo porque con
# 10 estilos la probabilidad se reparte y un umbral fijo casi nunca se alcanzaría.
FRACCION_ESTILO = 0.5
MAX_ESTILOS_SUGERIDOS = 3


class Sugerencia(BaseModel):
    nombre: str
    confianza: float


class Analisis(BaseModel):
    categorias: list[Sugerencia]
    colores: list[Sugerencia]
    estilos: list[Sugerencia]
    estilosSugeridos: list[str]
    colorDominanteHex: str
    # "kmeans" si el color sale de comparar píxeles, "clip" si ningún color
    # del catálogo tenía referencia y hubo que preguntarle al modelo
    metodoColor: str


class Salud(BaseModel):
    estado: str
    modelo: str
    dispositivo: str


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Los tests inyectan un clasificador falso para no cargar CLIP
    if getattr(app.state, "clasificador", None) is None:
        app.state.clasificador = ClasificadorClip()
    yield


app = FastAPI(title="Style & Scent Vision", lifespan=lifespan)


def _sugerencias(ranking: list[tuple[str, float]]) -> list[Sugerencia]:
    return [Sugerencia(nombre=n, confianza=round(c, 4)) for n, c in ranking]


@app.get("/salud", response_model=Salud)
def salud() -> Salud:
    clasificador = app.state.clasificador
    return Salud(
        estado="ok",
        modelo=getattr(clasificador, "nombre_modelo", "desconocido"),
        dispositivo=getattr(clasificador, "dispositivo", "desconocido"),
    )


@app.post("/analizar", response_model=Analisis)
async def analizar(
    imagen: Annotated[UploadFile, File()],
    categorias: Annotated[list[str], Form()],
    colores: Annotated[list[str], Form()],
    estilos: Annotated[list[str], Form()],
) -> Analisis:
    datos = await imagen.read()
    if len(datos) > TAMANO_MAXIMO:
        raise HTTPException(status_code=413, detail="La imagen supera los 10 MB")
    if not datos:
        raise HTTPException(status_code=400, detail="No se ha recibido ninguna imagen")

    try:
        imagen_bgr = detector_color.decodificar(datos)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    imagen_pil = Image.fromarray(cv2.cvtColor(detector_color.sin_transparencia(imagen_bgr), cv2.COLOR_BGR2RGB))
    clasificador = app.state.clasificador

    ranking_categorias = clasificador.clasificar(imagen_pil, {c: prompts_categoria(c) for c in categorias})
    ranking_estilos = clasificador.clasificar(imagen_pil, {e: prompts_estilo(e) for e in estilos})

    rgb, _ = detector_color.color_dominante(imagen_bgr)
    ranking_colores = detector_color.puntuar_colores(rgb, colores)
    metodo_color = "kmeans"
    if ranking_colores is None:
        ranking_colores = clasificador.clasificar(imagen_pil, {c: prompts_color(c) for c in colores})
        metodo_color = "clip"

    prob_mejor = ranking_estilos[0][1]
    sugeridos = [n for n, p in ranking_estilos if p >= prob_mejor * FRACCION_ESTILO][:MAX_ESTILOS_SUGERIDOS]

    return Analisis(
        categorias=_sugerencias(ranking_categorias),
        colores=_sugerencias(ranking_colores),
        estilos=_sugerencias(ranking_estilos),
        estilosSugeridos=sugeridos,
        colorDominanteHex=detector_color.a_hex(rgb),
        metodoColor=metodo_color,
    )
