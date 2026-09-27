# Style & Scent Engine

Aplicación full-stack que analiza el outfit de un usuario y recomienda el perfume ideal de su colección, basándose en reglas de sinergia (estilo y color) y reglas de exclusión (notas olfativas vetadas).

Proyecto personal de portfolio, aplicando Diseño de Bases de Datos e Ingeniería del Software.

## Estado actual

- [x] Base de datos MySQL normalizada (Docker)
- [x] Backend Java / Spring Boot con capa JPA completa y endpoints REST (registro de prendas, catálogos y matching)
- [x] Motor de puntuación (`StyleScentService`) con tests unitarios
- [x] Frontend React Native (Expo) con formularios dinámicos y conexión en red local
- [ ] Microservicio de visión artificial (Fase 2)

## Arquitectura
```
├── backend/            # API REST en Java + Spring Boot
│   └── src/main/java/com/stylescent/
│       ├── controller/  # Controladores REST (Prendas, catálogos)
│       ├── dto/         # Objetos de transferencia de datos (DTOs)
│       ├── model/       # Entidades JPA
│       ├── repository/  # Spring Data JPA
│       ├── service/     # Lógica de negocio (motor de puntuación y registro)[cite: 5]
│       └── exception/   # Manejo global de errores[cite: 5]
├── frontend/           # Aplicación móvil en React Native (Expo)
└── docker-compose.yml   # Contenedor MySQL 8.0[cite: 5]   
```
## Base de datos

MySQL 8.0, normalizada en 3FN[cite: 5]. Tablas principales:

- `perfumes` / `familias_olfativas` / `notas` / `perfume_nota` / `estados_posesion`[cite: 5]
- `prendas` / `categorias` / `colores` / `estilos` / `prenda_estilo`[cite: 5]
- `sinergias_color` / `sinergias_estilo` — reglas de puntuación por color/estilo de la prenda contra la familia olfativa del perfume[cite: 5]
- `filtros_exclusion` — notas que penalizan el score si el perfume las contiene[cite: 5]

## Endpoints Principales

- **POST /api/match**: Calcula el Match Score (0-100) combinando prendas y perfume con sus respectivas explicaciones[cite: 5].
- **POST /api/prendas**: Registra una nueva prenda validando sus relaciones en la base de datos.
- **GET /api/prendas/categorias**, **/colores**, **/estilos**: Proveen los catálogos dinámicos para los selectores de la app móvil.

## Motor de puntuación

`StyleScentService.calculateMatchScore(prendasIds, perfumeId)`:

1. Parte de una base de 50 puntos.
2. Por cada prenda seleccionada, suma los puntos de `sinergias_color` (por su color) y `sinergias_estilo` (por cada estilo que tenga).
3. Por cada nota del perfume presente en `filtros_exclusion`, resta la penalización configurada.
4. Devuelve el score final (acotado entre 0 y 100) junto a un array de mensajes explicando cada suma/resta.

## Cómo levantarlo

### 1. Base de datos
```bash
docker compose up -d
```
### 2. Backend (Java / Spring Boot)
```bash
cd backend
export DB_USERNAME=app_user
export DB_PASSWORD=app_password
./mvnw spring-boot:run
```
3. Frontend (React Native / Expo)
```bash
cd frontend
npx expo start
```

## Probar el endpoint

```bash
curl -X POST http://localhost:8080/api/match \
  -H "Content-Type: application/json" \
  -d '{"prendasIds": [1, 2], "perfumeId": 17}'
```

## Stack
- **Base de datos**: MySQL 8.0 (Docker)[cite: 5]

- **Backend**: Java 21, Spring Boot 4.1, Spring Data JPA, Lombok[cite: 5]

- **Frontend**: TypeScript, React Native, Expo, React Native Picker

- **IA (Fase 2, próximamente)**: Python, FastAPI, OpenCV, scikit-learn[cite: 5]

