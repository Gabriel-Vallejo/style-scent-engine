# Style & Scent Engine

Aplicación full-stack que analiza el outfit de un usuario y recomienda el perfume ideal de su colección, basándose en reglas de sinergia (estilo y color) y reglas de exclusión (notas olfativas vetadas).

Proyecto personal de portfolio, aplicando Diseño de Bases de Datos e Ingeniería del Software.

## Estado actual

- [x] Base de datos MySQL normalizada (Docker)
- [x] Backend Java / Spring Boot con capa JPA completa y endpoints REST (registro de prendas, catálogos y matching)
- [x] Motor de puntuación (`StyleScentService`) con tests unitarios
- [x] Frontend React Native (Expo) con formularios dinámicos y conexión en red local
- [x] Registro de perfumes desde el móvil
- [ ] Microservicio de visión artificial (Fase 2)

## Arquitectura
```
├── backend/             # API REST en Java + Spring Boot
│   └── src/main/java/com/stylescent/
│       ├── controller/  # Controladores REST (match, prendas, perfumes, catálogos)
│       ├── dto/         # Objetos de transferencia de datos (DTOs)
│       ├── model/       # Entidades JPA
│       ├── repository/  # Spring Data JPA
│       ├── service/     # Motor de puntuación (StyleScentService), PrendaService y PerfumeService
│       └── exception/   # Manejo global de errores (GlobalExceptionHandler)
├── frontend/            # Aplicación móvil en React Native (Expo)
│   ├── App.tsx          # Navegación por pestañas
│   └── src/
│       ├── api/         # Cliente HTTP hacia el backend
│       ├── components/  # Tarjeta de resultado del match
│       ├── screens/     # Registrar prenda / Registrar perfume / Recomendar perfume
│       └── types.ts     # Tipos compartidos con los DTOs del backend
└── docker-compose.yml   # Contenedor MySQL 8.0
```
## Base de datos

MySQL 8.0, normalizada en 3FN. Tablas principales:

- `perfumes` / `familias_olfativas` / `notas` / `perfume_nota` / `estados_posesion`
- `prendas` / `categorias` / `colores` / `estilos` / `prenda_estilo`
- `sinergias_color` / `sinergias_estilo` — reglas de puntuación por color/estilo de la prenda contra la familia olfativa del perfume
- `filtros_exclusion` — notas que penalizan el score si el perfume las contiene

## Endpoints Principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/match` | Calcula el Match Score (0-100) de un conjunto de prendas con un perfume, con la explicación de cada suma/resta. |
| `POST` | `/api/match/recomendar` | Puntúa el outfit contra todos los perfumes en colección y devuelve el ranking de mejor a peor. |
| `GET` | `/api/prendas` | Lista las prendas registradas. |
| `POST` | `/api/prendas` | Registra una nueva prenda validando sus relaciones (categoría, color, estilos). |
| `GET` | `/api/perfumes` | Lista los perfumes en colección (los de "Lista de deseos" / "En camino" no se recomiendan). |
| `POST` | `/api/perfumes` | Registra un perfume validando familia olfativa, estado y notas. |
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

## Cómo levantarlo

### 1. Base de datos
```bash
docker compose up -d
```
MySQL queda expuesto en el puerto `3307` del host. Las credenciales se leen de `backend/.env` (no versionado).

### 2. Backend (Java / Spring Boot)
```bash
cd backend
export DB_USERNAME=<usuario>   # ver backend/.env
export DB_PASSWORD=<contraseña>
./mvnw spring-boot:run
```
### 3. Frontend (React Native / Expo)
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
- **Frontend**: TypeScript, React Native, Expo, React Navigation (bottom tabs), React Native Picker
- **IA (Fase 2, próximamente)**: Python, FastAPI, OpenCV, scikit-learn
