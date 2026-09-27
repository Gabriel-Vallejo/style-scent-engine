# vision-service

Microservicio de visión artificial de Style & Scent Engine. Recibe la foto de una prenda junto con los nombres del catálogo y sugiere su **categoría**, **color** y **estilos**, cada uno con su confianza.

- No accede a la base de datos: el backend de Spring le manda el catálogo en cada petición, así que un valor nuevo en la BD se tiene en cuenta al momento.
- No guarda las fotos.
- Solo lo llama el backend (`POST /api/prendas/analizar`); en Docker escucha únicamente en `127.0.0.1:8001`.

## Cómo funciona

**Color (OpenCV + scikit-learn)** — `app/color.py`

1. Separa la prenda del fondo con GrabCut, suponiendo que el borde de la foto es fondo. Si el PNG trae transparencia, usa el canal alfa, que es más exacto. Si GrabCut apenas encuentra prenda, se queda con el centro de la imagen.
2. Agrupa los píxeles de la prenda con k-means (3 grupos) en espacio Lab, donde la distancia euclídea se parece a la diferencia de color que percibe el ojo (ΔE).
3. Fusiona los grupos a menos de ΔE 12: con ruido, arrugas o sombras, k-means parte un mismo color en varios.
4. El grupo mayor es el color dominante. Se compara con el RGB de referencia de cada color del catálogo usando **CIEDE2000 con el factor textil kL = 2**, que resta peso a la luminosidad: la luz de una foto cambia mucho más la luminosidad que el tono, y con la distancia euclídea (CIE76) un verde oliva apagado quedaba más cerca de un gris. La confianza sale de la distancia.

**Categoría y estilos (CLIP zero-shot)** — `app/clasificador.py`

- Modelo `openai/clip-vit-base-patch32`, fijado a la revisión que tiene los pesos en `safetensors`.
- Cada nombre del catálogo se traduce a varias frases en inglés (`app/etiquetas.py`), cuyos embeddings se promedian. Si un nombre no tiene frases, se usa una genérica (`"a photo of {nombre}"`).
- Estilos sugeridos: el más probable y los que tengan al menos la mitad de su probabilidad, con un máximo de 3 (una prenda puede tener varios estilos).
- Si ningún color del catálogo tiene RGB de referencia, el color también lo decide CLIP (`metodoColor: "clip"`).

## API

### `POST /analizar`

`multipart/form-data`:

| Campo | Tipo | Descripción |
|---|---|---|
| `imagen` | fichero | Foto de la prenda (JPEG, PNG...; máx. 10 MB) |
| `categorias` | texto, repetible | Nombres de las categorías del catálogo |
| `colores` | texto, repetible | Nombres de los colores del catálogo |
| `estilos` | texto, repetible | Nombres de los estilos del catálogo |

Respuesta (las listas van de mayor a menor confianza):

```json
{
  "categorias": [{"nombre": "Calzado", "confianza": 0.995}, "..."],
  "colores": [{"nombre": "Negro", "confianza": 0.42}, "..."],
  "estilos": [{"nombre": "Rockero", "confianza": 0.49}, "..."],
  "estilosSugeridos": ["Rockero"],
  "colorDominanteHex": "#15140e",
  "metodoColor": "kmeans"
}
```

Errores: `400` si el fichero no es una imagen, `413` si supera los 10 MB y `422` si falta algún campo.

### `GET /salud`

```json
{"estado": "ok", "modelo": "openai/clip-vit-base-patch32", "dispositivo": "cpu"}
```

## Añadir colores o estilos

Basta con insertarlos en la BD (con sus reglas de sinergia, ver `CONTEXT.md`). Para que la IA los detecte bien:

- **Color**: añade su RGB de referencia a `COLORES_RGB` en `app/etiquetas.py`, con la clave en minúsculas y sin tildes. Conviene que no quede a menos de ΔE ~12 de otro color existente; si no, se confundirán.
- **Estilo**: añade 2 o 3 frases en inglés a `PROMPTS_ESTILO` que lo diferencien de los demás. Sin ellas se usa una frase genérica, que funciona peor.

Después hay que reconstruir la imagen: `docker compose --env-file backend/.env up -d --build vision`.

## Desarrollo

```bash
python3 -m venv .venv
.venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cpu
.venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest
```

Los tests no cargan CLIP. Los de color usan imágenes sintéticas (una prenda sobre un fondo, con ruido) y los de la API, un clasificador falso.

Para arrancarlo en local (descarga el modelo la primera vez, ~600 MB):

```bash
.venv/bin/uvicorn app.main:app --port 8001
```

## Docker

- PyTorch en su versión CPU: ~200 ms por foto y ~425 MB de RAM, sin necesidad de GPU.
- El modelo se descarga al construir la imagen (solo `safetensors` y configuración), así que el contenedor arranca sin internet (`HF_HUB_OFFLINE=1`).
- Se ejecuta con un usuario sin privilegios y tiene un healthcheck contra `/salud`.

## Limitaciones conocidas

- **El estilo es la sugerencia menos fiable:** es subjetivo, y con 22 estilos la probabilidad se reparte. Hay que tomarlo como una pista.
- **El color depende de la luz de la foto:** una prenda blanca en sombra puede salir *Gris claro*. La segunda opción suele ser la correcta.
- **Con 43 colores, las confianzas son bajas (15-35 %)** aunque el primer resultado sea correcto: se reparten entre muchas opciones. Por eso la app ofrece también los 2 colores siguientes con un toque.
