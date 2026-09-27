"""Clasificación zero-shot con CLIP: se compara el embedding de la foto con el de
frases que describen cada etiqueta del catálogo, sin entrenar nada. Si mañana se
añade un estilo nuevo a la BD, basta con que llegue en la petición.
"""

from typing import Protocol

import torch
from PIL import Image
from transformers import CLIPModel, CLIPProcessor

MODELO = "openai/clip-vit-base-patch32"
# Revisión fijada: la rama main solo tiene pytorch_model.bin (pickle); esta es la
# de la conversión a safetensors. El Dockerfile descarga exactamente esta.
REVISION = "c237dc49a33fc61debc9276459120b7eac67e7ef"


class Clasificador(Protocol):
    def clasificar(self, imagen: Image.Image, etiquetas: dict[str, list[str]]) -> list[tuple[str, float]]:
        """Probabilidad de cada etiqueta ({nombre: prompts}), de mayor a menor."""
        ...


class ClasificadorClip:
    def __init__(self, modelo: str = MODELO, revision: str = REVISION):
        self.dispositivo = "cuda" if torch.cuda.is_available() else "cpu"
        self.modelo = CLIPModel.from_pretrained(modelo, revision=revision, use_safetensors=True)
        self.modelo = self.modelo.to(self.dispositivo).eval()
        self.procesador = CLIPProcessor.from_pretrained(modelo, revision=revision)
        self.nombre_modelo = modelo
        # Los textos se repiten en cada petición (el catálogo cambia poco): se cachean
        self._cache_textos: dict[str, torch.Tensor] = {}

    @torch.no_grad()
    def _embedding_texto(self, prompt: str) -> torch.Tensor:
        if prompt not in self._cache_textos:
            entrada = self.procesador(text=[prompt], return_tensors="pt", padding=True).to(self.dispositivo)
            # En transformers 5 get_*_features devuelve un objeto; pooler_output es la proyección
            emb = self.modelo.get_text_features(**entrada).pooler_output[0]
            self._cache_textos[prompt] = emb / emb.norm()
        return self._cache_textos[prompt]

    @torch.no_grad()
    def _embedding_imagen(self, imagen: Image.Image) -> torch.Tensor:
        entrada = self.procesador(images=imagen, return_tensors="pt").to(self.dispositivo)
        emb = self.modelo.get_image_features(**entrada).pooler_output[0]
        return emb / emb.norm()

    @torch.no_grad()
    def clasificar(self, imagen: Image.Image, etiquetas: dict[str, list[str]]) -> list[tuple[str, float]]:
        emb_imagen = self._embedding_imagen(imagen)

        nombres = list(etiquetas)
        # Prompt ensembling: media de los embeddings de todas las frases de cada etiqueta
        emb_etiquetas = []
        for nombre in nombres:
            media = torch.stack([self._embedding_texto(p) for p in etiquetas[nombre]]).mean(dim=0)
            emb_etiquetas.append(media / media.norm())

        logits = self.modelo.logit_scale.exp() * torch.stack(emb_etiquetas) @ emb_imagen
        probabilidades = logits.softmax(dim=0).tolist()
        return sorted(zip(nombres, probabilidades), key=lambda x: x[1], reverse=True)
