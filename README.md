# Style & Scent Engine

[![CI](https://github.com/Gabriel-Vallejo/style-scent-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/Gabriel-Vallejo/style-scent-engine/actions/workflows/ci.yml)

Aplicación full-stack que analiza el outfit de un usuario y recomienda el perfume ideal de su colección, basándose en reglas de sinergia (estilo y color) y reglas de exclusión (notas olfativas vetadas). Las prendas se pueden registrar a mano o a partir de una foto, que analiza un microservicio de visión artificial.

Proyecto personal de portfolio, aplicando Diseño de Bases de Datos e Ingeniería del Software.

## Estado actual

- [x] Base de datos MySQL normalizada (Docker), con esquema y datos iniciales versionados
- [x] Backend Java / Spring Boot con capa JPA completa y API REST
- [x] Motor de puntuación (`StyleScentService`) y recomendación del mejor perfume, con tests unitarios
- [x] App móvil React Native (Expo): registro, gestión y recomendación
- [x] Microservicio de visión artificial (Fase 2): sugiere categoría, color y estilos a partir de una foto
- [x] APK nativo para Android ([`style-scent.apk`](style-scent.apk)), compilado con EAS Build

## Funcionalidades de la app

La app tiene tres pestañas:

- **Prenda**: registrar una prenda (nombre, categoría, color y uno o varios estilos). Se puede rellenar con una foto (cámara o galería): la IA preselecciona categoría, color y estilos con su porcentaje de confianza y el usuario los confirma o corrige. Debajo, el armario con opción de borrar.
- **Perfume**: registrar un perfume (nombre, marca, familia olfativa, notas y estado: *En coleccion*, *En camino* o *Lista de deseos*). Debajo, los perfumes agrupados por estado; se cambia de estado con un toque y se pueden borrar.
- **Recomendar**: se elige el outfit y, opcionalmente, un perfume.
  - Con perfume: calcula su Match Score.
  - Sin perfume: puntúa toda la colección y muestra el mejor, con alternativas.

  El resultado muestra la puntuación, un veredicto y el desglose de cada suma o resta.

## Arquitectura

```
                         ┌──────────────────────────┐
  App móvil (Expo) ────▶ │  Backend Spring Boot     │ ────▶ MySQL 8.0 (Docker)
  red local, :8080       │  API REST + motor        │       127.0.0.1:3307
                         └────────────┬─────────────┘
                                      │ foto + nombres del catálogo
                                      ▼
                         vision-service (FastAPI, Docker)
                         127.0.0.1:8001
```

MySQL y el servicio de visión solo escuchan en `127.0.0.1`: el único punto de entrada desde fuera del servidor es el backend.

```
├── backend/                 # API REST en Java + Spring Boot
│   └── src/main/java/com/stylescent/
│       ├── controller/      # Match, prendas, perfumes y catálogos
│       ├── service/         # StyleScentService (motor), PrendaService, PerfumeService, AnalisisPrendaService
│       ├── vision/          # Cliente HTTP del microservicio de visión
│       ├── dto/             # Objetos de transferencia de datos
│       ├── model/           # Entidades JPA
│       ├── repository/      # Spring Data JPA
│       └── exception/       # GlobalExceptionHandler
├── vision-service/          # Microservicio de visión en Python (ver su README)
├── frontend/                # App móvil en React Native (Expo)
│   ├── App.tsx              # Navegación por pestañas
│   ├── app.json / eas.json  # Configuración de la app y del build del APK
│   └── src/
│       ├── api/             # Cliente HTTP hacia el backend
│       ├── components/      # Tarjeta de resultado del match
│       ├── screens/         # Prenda / Perfume / Recomendar
│       └── types.ts         # Tipos compartidos con los DTOs del backend
├── database/                # Esquema (01-schema.sql) y datos iniciales (02-datos.sql)
├── style-scent.apk          # App Android lista para instalar
└── docker-compose.yml       # MySQL + servicio de visión
```

## Base de datos

MySQL 8.0, normalizada en 3FN. El esquema está en [`database/01-schema.sql`](database/01-schema.sql) y los datos iniciales en [`database/02-datos.sql`](database/02-datos.sql).

| Tablas | Contenido |
|---|---|
| `perfumes`, `perfume_nota` | La colección de perfumes y sus notas (M:N) |
| `familias_olfativas`, `notas`, `estados_posesion` | Catálogos de perfumes: 12 familias, 32 notas, 3 estados |
| `prendas`, `prenda_estilo` | El armario; una prenda puede tener varios estilos (M:N) |
| `categorias`, `colores`, `estilos` | Catálogos de prendas: 4 categorías, 43 colores, 22 estilos |
| `sinergias_color`, `sinergias_estilo` | Puntos que suma cada color/estilo con cada familia olfativa (198 reglas, una por par gracias a `UNIQUE`) |
| `filtros_exclusion` | Notas vetadas y cuánto restan si el perfume las contiene |

Las tablas intermedias (`perfume_nota`, `prenda_estilo`) usan `ON DELETE CASCADE`.

## API REST

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/match` | Match Score (0-100) de un conjunto de prendas con un perfume, con el desglose de cada suma/resta. |
| `POST` | `/api/match/recomendar` | Puntúa el outfit contra todos los perfumes en colección y devuelve el ranking de mejor a peor. |
| `GET` | `/api/prendas` | Lista las prendas registradas. |
| `POST` | `/api/prendas` | Registra una prenda validando sus relaciones (categoría, color, estilos). |
| `POST` | `/api/prendas/analizar` | Recibe una foto (`multipart/form-data`, campo `imagen`, máx. 10 MB) y devuelve la categoría, el color y los estilos sugeridos, con su ID y confianza. No guarda nada. |
| `DELETE` | `/api/prendas/{id}` | Borra una prenda (y sus estilos, en cascada). |
| `GET` | `/api/perfumes` | Perfumes en colección (los de *En camino* / *Lista de deseos* no se recomiendan). Con `?todos=true`, todos. |
| `POST` | `/api/perfumes` | Registra un perfume validando familia olfativa, estado y notas. |
| `PATCH` | `/api/perfumes/{id}/estado` | Cambia el estado de posesión. Cuerpo: `{"idEstado": 1}`. |
| `DELETE` | `/api/perfumes/{id}` | Borra un perfume (y sus notas, en cascada). |
| `GET` | `/api/categorias`, `/api/colores`, `/api/estilos` | Catálogos para el formulario de prendas. |
| `GET` | `/api/familias`, `/api/notas`, `/api/estados` | Catálogos para el formulario de perfumes. |

Los errores se centralizan en `GlobalExceptionHandler` y siempre devuelven `{timestamp, status, error, message}`:

| Código | Cuándo |
|---|---|
| `400` | Petición inválida: faltan datos, un texto supera la longitud máxima, JSON mal formado, el archivo no es una imagen... |
| `404` | Algún ID no existe |
| `413` | La foto supera los 10 MB |
| `503` | El servicio de visión no responde (la app permite seguir registrando a mano) |

## Motor de puntuación

`StyleScentService` solo puntúa; el CRUD vive en `PrendaService` y `PerfumeService`.

1. Parte de una base de 50 puntos.
2. Por cada prenda del outfit suma los puntos de `sinergias_color` (por su color) y de `sinergias_estilo` (por cada uno de sus estilos) con la familia olfativa del perfume.
3. Por cada nota del perfume presente en `filtros_exclusion`, resta la penalización configurada.
4. Devuelve `score` (acotado entre 0 y 100), `scoreSinAcotar` y un desglose `detalles` con el tipo (`BASE`, `COLOR`, `ESTILO`, `EXCLUSION`), la descripción (qué prenda aporta cada punto) y los puntos.

`recomendar(prendasIds)` aplica lo mismo a cada perfume *En coleccion* y ordena por `scoreSinAcotar`, para desempatar perfumes que llegan todos a 100.

## Visión artificial (Fase 2)

```
App ──foto──▶ Spring Boot ──foto + nombres del catálogo──▶ vision-service
 ▲                 │                                             │
 └── formulario ◀──┘◀───────── sugerencias + confianza ──────────┘
     prerrellenado     (el backend las traduce a IDs)
```

- **Color**: OpenCV separa la prenda del fondo y k-means (scikit-learn) busca el color dominante en espacio Lab, que se compara con los colores del catálogo.
- **Categoría y estilos**: CLIP en modo zero-shot contra frases generadas a partir de los nombres del catálogo. No hay que entrenar nada: un estilo nuevo en la BD se reconoce en la siguiente petición.
- El servicio no accede a la BD ni guarda las fotos, y la IA solo sugiere: el usuario confirma antes de registrar.

Detalles del algoritmo, contrato de la API y cómo añadir colores o estilos: [`vision-service/README.md`](vision-service/README.md).

## Cómo levantarlo

### 1. MySQL y servicio de visión (Docker)
```bash
cp backend/.env.example backend/.env   # y rellena las contraseñas
docker compose --env-file backend/.env up -d
```
- `backend/.env` no se versiona; lo leen tanto Docker como Spring Boot.
- La primera vez tarda unos minutos: MySQL carga los scripts de `database/` y se construye la imagen de visión, que descarga PyTorch (CPU) y el modelo CLIP (~2,3 GB en total).
- Para reiniciar la base de datos desde cero (borra los datos actuales): `docker compose down -v && docker compose --env-file backend/.env up -d`.
- Para conectarse a MySQL desde otro equipo hace falta un túnel SSH: `ssh -L 3307:localhost:3307 usuario@servidor`.

### 2. Backend (Java 21 / Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
Arranca en el puerto `8080`. La URL del servicio de visión se puede cambiar con la variable `VISION_URL` (por defecto `http://localhost:8001`).

### 3. App móvil (Expo)
```bash
cd frontend
npm install
npx expo start
```
La URL del backend está en `frontend/src/api/client.ts`: debe apuntar a la IP del servidor en la red local, la misma que muestra Metro (`exp://<ip>:8081`).

## Instalar la app en Android (APK)

[`style-scent.apk`](style-scent.apk) es la app nativa: se instala en el móvil y funciona sin Expo Go ni Metro.

1. Descarga el APK en el móvil (desde GitHub: abre el archivo y pulsa *Download raw file*).
2. Ábrelo. Android pedirá permitir "instalar apps de origen desconocido" para el navegador o el gestor de archivos: es normal en apps que no vienen de Google Play.
3. El móvil tiene que estar en la **misma wifi que el servidor**, con el backend encendido.

La URL del backend queda fijada al compilar (`EXPO_PUBLIC_API_URL` en [`frontend/eas.json`](frontend/eas.json), ahora `http://192.168.1.44:8080/api`). Si el router cambia la IP del servidor, hay que cambiarla ahí y recompilar.

### Recompilar el APK

Se compila en la nube con [EAS Build](https://docs.expo.dev/build/introduction/), sin Android Studio:

```bash
cd frontend
npx eas-cli@latest login          # o exportar EXPO_TOKEN con un token de acceso
npx eas-cli@latest build -p android --profile preview
```

Al terminar, EAS da un enlace para descargar el APK. El perfil `preview` genera un APK firmado con una clave que guarda EAS.

## Tests

El CI de GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) los ejecuta en cada push: el backend contra un MySQL cargado con `database/`, el servicio de visión con pytest y la app con `tsc` y ESLint. Dependabot revisa las dependencias cada semana.

```bash
(cd backend && ./mvnw test)                        # 31 tests (necesita MySQL levantado)
(cd vision-service && .venv/bin/python -m pytest)   # 17 tests (ver su README para crear el entorno)
(cd frontend && npx tsc --noEmit && npm run lint)   # tipos y lint de la app
```

## Probar la API

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

```bash
curl -X POST http://localhost:8080/api/prendas/analizar -F "imagen=@chaqueta.jpg"
```

## Stack

- **Base de datos**: MySQL 8.0 (Docker)
- **Backend**: Java 21, Spring Boot 4.1, Spring Data JPA, Lombok
- **App móvil**: TypeScript, React Native, Expo (SDK 57), React Navigation, Expo Vector Icons, Expo Image Picker, EAS Build
- **IA**: Python 3.12, FastAPI, OpenCV, scikit-learn, PyTorch + Hugging Face Transformers (CLIP)
