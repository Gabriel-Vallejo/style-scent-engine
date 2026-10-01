FROM python:3.14-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    HF_HOME=/modelos

# PyTorch en su versión CPU: la imagen pesa ~1 GB menos que con CUDA y para
# clasificar una foto cada vez va sobrado (~200 ms)
RUN pip install --no-cache-dir torch==2.14.0 --index-url https://download.pytorch.org/whl/cpu
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Usuario sin privilegios creado ANTES de descargar el modelo: un chown posterior
# duplicaría en otra capa los ~600 MB de pesos
RUN useradd --create-home vision && mkdir /modelos && chown vision /modelos
USER vision

# El modelo se descarga al construir la imagen, no al arrancar: el contenedor
# no necesita internet y arranca en segundos. Solo safetensors + configuración
# (el repo trae los mismos pesos también en .bin, .h5 y .msgpack) y en la misma
# revisión fijada que usa app/clasificador.py
COPY app/clasificador.py /tmp/clasificador.py
RUN python -c "import sys; sys.path.insert(0, '/tmp'); \
    from clasificador import MODELO, REVISION; \
    from huggingface_hub import snapshot_download; \
    snapshot_download(MODELO, revision=REVISION, \
                      allow_patterns=['*.json', '*.txt', 'model.safetensors'])"
ENV HF_HUB_OFFLINE=1

COPY app ./app

EXPOSE 8001
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/salud')"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
