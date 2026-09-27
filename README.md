# Style & Scent Engine

Aplicación que analiza el outfit de un usuario y recomienda el perfume ideal de su colección, basándose en reglas de sinergia (estilo y color) y reglas de exclusión (notas olfativas vetadas).

Proyecto personal de portfolio, aplicando Diseño de Bases de Datos e Ingeniería del Software.

## Estado actual

- [x] Base de datos MySQL normalizada (Docker)
- [x] Backend Java / Spring Boot con capa JPA completa
- [x] Motor de puntuación (`StyleScentService`) con tests unitarios
- [x] Endpoint REST `POST /api/match`
- [ ] Frontend React Native
- [ ] Microservicio de visión artificial (Fase 2)

## Arquitectura

```
├── backend/            # API REST en Java + Spring Boot
│   └── src/main/java/com/stylescent/
│       ├── model/       # Entidades JPA
│       ├── repository/  # Spring Data JPA
│       ├── service/     # Lógica de negocio (motor de puntuación)
│       ├── controller/  # Endpoints REST
│       └── exception/   # Manejo global de errores
├── docker-compose.yml   # Contenedor MySQL 8.0
└── frontend/            # (pendiente) React Native
```

## Base de datos

MySQL 8.0, normalizada en 3FN. Tablas principales:

- `perfumes` / `familias_olfativas` / `notas` / `perfume_nota` / `estados_posesion`
- `prendas` / `categorias` / `colores` / `estilos` / `prenda_estilo`
- `sinergias_color` / `sinergias_estilo` — reglas de puntuación por color/estilo de la prenda contra la familia olfativa del perfume
- `filtros_exclusion` — notas que penalizan el score si el perfume las contiene

## Motor de puntuación

`StyleScentService.calculateMatchScore(prendasIds, perfumeId)`:

1. Parte de una base de 50 puntos.
2. Por cada prenda seleccionada, suma los puntos de `sinergias_color` (por su color) y `sinergias_estilo` (por cada estilo que tenga).
3. Por cada nota del perfume presente en `filtros_exclusion`, resta la penalización configurada.
4. Devuelve el score final (acotado entre 0 y 100) junto a un array de mensajes explicando cada suma/resta.

## Cómo levantarlo

```bash
# 1. Base de datos
docker compose up -d

# 2. Backend (requiere las credenciales del docker-compose como variables de entorno)
cd backend
export DB_USERNAME=app_user
export DB_PASSWORD=app_password
./mvnw spring-boot:run
```

## Probar el endpoint

```bash
curl -X POST http://localhost:8080/api/match \
  -H "Content-Type: application/json" \
  -d '{"prendasIds": [1, 2], "perfumeId": 17}'
```

## Stack

- **Base de datos**: MySQL 8.0 (Docker)
- **Backend**: Java 21, Spring Boot 4.1, Spring Data JPA, Lombok
- **Frontend** (próximamente): TypeScript, React Native
- **IA (Fase 2, próximamente)**: Python, FastAPI, OpenCV, scikit-learn
