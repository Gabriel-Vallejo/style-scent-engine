# Style & Scent Engine

Aplicación full-stack que analiza el outfit de un usuario y recomienda el perfume ideal de su colección, basándose en reglas de sinergia (estilo y color) y reglas de exclusión (notas olfativas vetadas).

Proyecto personal de portfolio, aplicando Diseño de Bases de Datos e Ingeniería del Software.

## Estado actual

- [x] Base de datos MySQL normalizada (Docker)
- [x] Backend Java / Spring Boot con capa JPA completa y endpoints REST (registro de prendas, catálogos y matching)
- [x] Motor de puntuación (`StyleScentService`) con tests unitarios
- [x] Frontend React Native (Expo) con formularios dinámicos y conexión en red local
- [x] Registro de perfumes desde el móvil
- [x] Gestión desde el móvil: listar y borrar prendas/perfumes, cambiar el estado de un perfume
- [x] Microservicio de visión artificial (Fase 2): sugiere categoría, color y estilos a partir de una foto

## Arquitectura
```
├── backend/             # API REST en Java + Spring Boot
│   └── src/main/java/com/stylescent/
│       ├── controller/  # Controladores REST (match, prendas, perfumes, catálogos)
│       ├── vision/      # Cliente HTTP del microservicio de visión
│       ├── dto/         # Objetos de transferencia de datos (DTOs)
│       ├── model/       # Entidades JPA
│       ├── repository/  # Spring Data JPA
│       ├── service/     # Motor de puntuación (StyleScentService), PrendaService y PerfumeService
│       └── exception/   # Manejo global de errores (GlobalExceptionHandler)
├── vision-service/      # Microservicio de visión en Python (FastAPI + OpenCV + scikit-learn + CLIP)
│   ├── app/             # API, detección de color y clasificador CLIP
│   └── tests/           # pytest
├── frontend/            # Aplicación móvil en React Native (Expo)
│   ├── App.tsx          # Navegación por pestañas
│   └── src/
│       ├── api/         # Cliente HTTP hacia el backend
│       ├── components/  # Tarjeta de resultado del match
│       ├── screens/     # Registrar prenda / Registrar perfume / Recomendar perfume
│       └── types.ts     # Tipos compartidos con los DTOs del backend
├── database/            # Esquema (01-schema.sql) y datos iniciales (02-datos.sql)
└── docker-compose.yml   # Contenedor MySQL 8.0
```
## Base de datos

MySQL 8.0, normalizada en 3FN. El esquema está en [`database/01-schema.sql`](database/01-schema.sql) y los catálogos, reglas y colección de ejemplo en [`database/02-datos.sql`](database/02-datos.sql). Tablas principales:

- `perfumes` / `familias_olfativas` / `notas` / `perfume_nota` / `estados_posesion`
- `prendas` / `categorias` / `colores` / `estilos` / `prenda_estilo`
- `sinergias_color` / `sinergias_estilo` — reglas de puntuación por color/estilo de la prenda contra la familia olfativa del perfume (una por par, `UNIQUE`). Catálogo inicial: 29 colores, 10 estilos, 121 reglas
- `filtros_exclusion` — notas que penalizan el score si el perfume las contiene

## Endpoints Principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/match` | Calcula el Match Score (0-100) de un conjunto de prendas con un perfume, con la explicación de cada suma/resta. |
| `POST` | `/api/match/recomendar` | Puntúa el outfit contra todos los perfumes en colección y devuelve el ranking de mejor a peor. |
| `GET` | `/api/prendas` | Lista las prendas registradas. |
| `POST` | `/api/prendas` | Registra una nueva prenda validando sus relaciones (categoría, color, estilos). |
| `DELETE` | `/api/prendas/{id}` | Borra una prenda (y sus estilos, en cascada). |
| `POST` | `/api/prendas/analizar` | Recibe una foto (`multipart/form-data`, campo `imagen`, máx. 10 MB) y devuelve la categoría, el color y los estilos sugeridos con su ID y confianza. No guarda nada. `503` si el servicio de visión no está disponible. |
| `GET` | `/api/perfumes` | Lista los perfumes en colección (los de "Lista de deseos" / "En camino" no se recomiendan). Con `?todos=true` devuelve todos. |
| `POST` | `/api/perfumes` | Registra un perfume validando familia olfativa, estado y notas. |
| `PATCH` | `/api/perfumes/{id}/estado` | Cambia el estado de posesión (ej. de "En camino" a "En coleccion"). Cuerpo: `{"idEstado": 1}`. |
| `DELETE` | `/api/perfumes/{id}` | Borra un perfume (y sus notas, en cascada). |
| `GET` | `/api/categorias`, `/api/colores`, `/api/estilos` | Catálogos para el formulario de prendas. |
| `GET` | `/api/familias`, `/api/notas`, `/api/estados` | Catálogos para el formulario de perfumes. |

Errores: los recursos inexistentes devuelven `404` y las peticiones inválidas `400` (centralizado en `GlobalExceptionHandler`).

## Motor de puntuación

`StyleScentService` (solo puntúa; el CRUD vive en `PrendaService` / `PerfumeService`):

1. Parte de una base de 50 puntos.
2. Por cada prenda seleccionada, suma los puntos de `sinergias_color` (por su color) y `sinergias_estilo` (por cada estilo que tenga).
3. Por cada nota del perfume presente en `filtros_exclusion`, resta la penalización configurada.
4. Devuelve `score` (acotado entre 0 y 100), `scoreSinAcotar` y un desglose `detalles` con el tipo (`BASE`, `COLOR`, `ESTILO`, `EXCLUSION`), la descripción y los puntos de cada suma/resta.

`recomendar(prendasIds)` aplica lo mismo a cada perfume "En coleccion" y ordena por `scoreSinAcotar`, para desempatar perfumes que llegan todos a 100.

## Visión artificial (Fase 2)

```
Móvil ──foto──▶ Spring Boot ──foto + nombres del catálogo──▶ vision-service (FastAPI, :8001)
  ▲                  │                                              │
  └── formulario ◀───┘◀────────── sugerencias + confianza ──────────┘
      prerrellenado      (el backend las traduce a IDs)
```

- **Color**: OpenCV separa la prenda del fondo (GrabCut, o el canal alfa si el PNG viene recortado) y k-means de scikit-learn agrupa sus píxeles en espacio Lab. Se fusionan los grupos que el ojo ve como el mismo color (ΔE < 12) y el mayor se compara con los colores del catálogo.
- **Categoría y estilos**: CLIP (`openai/clip-vit-base-patch32`) en modo zero-shot contra frases en inglés generadas a partir de los nombres del catálogo. No hay que entrenar nada: un estilo nuevo en la BD se reconoce en la siguiente petición.
- El servicio no accede a la BD: el backend le manda el catálogo en cada petición. Tampoco guarda las fotos.
- La IA solo sugiere; el usuario confirma o corrige en el formulario antes de registrar.

## Cómo levantarlo

### 1. Base de datos
```bash
cp backend/.env.example backend/.env   # y rellena las contraseñas
docker compose --env-file backend/.env up -d
```
MySQL queda expuesto en el puerto `3307` del host. `backend/.env` no se versiona; lo leen tanto Docker como Spring Boot.

La primera vez que se crea el volumen, MySQL carga automáticamente los scripts de `database/`. Para reiniciar la base de datos desde cero: `docker compose down -v && docker compose --env-file backend/.env up -d` (borra los datos actuales).

### 2. Backend (Java / Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
### 3. Servicio de visión (Python)
```bash
docker compose --env-file backend/.env up -d --build vision
```
La imagen descarga el modelo al construirse (~2,3 GB en total, CPU). Tests:
```bash
cd vision-service
python3 -m venv .venv && .venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cpu && .venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest
```

### 4. Frontend (React Native / Expo)
```bash
cd frontend
npx expo start
```
La URL del backend está en `frontend/src/api/client.ts`: debe apuntar a la IP de la máquina que muestra Metro (`exp://<ip>:8081`) para que el móvil llegue al backend.

## Probar el endpoint

```bash
curl -X POST http://localhost:8080/api/match \
  -H "Content-Type: application/json" \
  -d '{"prendasIds": [1, 2], "perfumeId": 17}'
```

```bash
curl -X POST http://localhost:8080/api/match/recomendar \
  -H "Content-Type: application/json" \
  -d '{"prendasIds": [1, 2]}'
```

## Stack
- **Base de datos**: MySQL 8.0 (Docker)
- **Backend**: Java 21, Spring Boot 4.1, Spring Data JPA, Lombok
- **Frontend**: TypeScript, React Native, Expo, React Navigation (bottom tabs), Expo Vector Icons, Expo Image Picker
- **IA**: Python 3.12, FastAPI, OpenCV, scikit-learn, PyTorch + Hugging Face Transformers (CLIP)
