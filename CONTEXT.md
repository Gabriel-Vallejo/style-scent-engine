# Contexto del proyecto (para retomar en Claude Code)

## Qué es
Style & Scent Engine: analiza el outfit seleccionado y recomienda el perfume ideal
de la colección real del usuario, con un motor de puntuación basado en reglas.
Las prendas se pueden registrar a partir de una foto (microservicio de visión).
Qué hace cada parte, API y cómo arrancarlo: ver `README.md`.

## Decisiones de diseño importantes (no obvias mirando solo el código)

- **`sinergias_color` y `sinergias_estilo` son tablas separadas**, no una sola
  `sinergias_puntuacion` genérica. Se separaron porque el diseño original mezclaba
  reglas de color y de estilo en una tabla con texto libre (`atributo_ropa`), lo que
  causaba bugs de matching silenciosos.
- **Bug histórico resuelto (1)**: hubo un typo `'Ambar Fougere'` (sin tildes) en
  reglas de sinergia que nunca matcheaba con `'Ámbar Fougère'` en `familias_olfativas`.
  Si aparecen reglas que "no disparan", revisar tildes primero.
- **Bug histórico resuelto (2)**: `sinergias_estilo` tenía cada regla
  triplicada (el script de carga se ejecutó 3 veces) y Streetwear puntuaba x3.
  Ahora hay `UNIQUE (id_estilo, id_familia)` y `UNIQUE (id_color, id_familia)`.
- **Catálogo**: 29 colores y 10 estilos, todos con reglas de sinergia (criterios
  de perfumería habituales, escala 10-30, revisables por el usuario). Para
  añadir más: insertar las reglas buscando por nombre (`JOIN ... ON f.nombre =
  '...'`) y comprobar el número de filas insertadas (por las tildes). Un color
  nuevo necesita además su RGB en `vision-service/app/etiquetas.py`, y un estilo
  nuevo, idealmente, sus prompts ahí mismo (ver `vision-service/README.md`).
- **`prenda_estilo` es M:N**, no 1:1: una prenda puede tener varios estilos a la
  vez (ej. las Ray-Ban Meta son Streetwear + Casual). `Prenda.estilos` es un `Set`,
  no una `List`; ojo con ese tipo al escribir código nuevo.
- **`GlobalExceptionHandler`** centraliza errores: `EntityNotFoundException` → 404,
  `IllegalArgumentException` → 400, `VisionNoDisponibleException` → 503,
  `MaxUploadSizeExceededException` → 413. Cualquier excepción nueva de negocio
  va ahí, no en try/catch ad-hoc en los controllers.
- **Separación de services**: `StyleScentService` es solo el motor de puntuación.
  El CRUD vive en `PrendaService` y `PerfumeService`, y el análisis de fotos en
  `AnalisisPrendaService`. No mezclar responsabilidades.
- **IDs son `Integer` en todo el proyecto** (entidades, repos, DTOs), no `Long`:
  decisión deliberada para que coincida con el `INT AUTO_INCREMENT` de MySQL.
- **`GET /api/perfumes` sin parámetros devuelve solo "En coleccion"** a propósito
  (es lo que usa Recomendar); la gestión usa `?todos=true`.
- **El desglose del match es estructurado** (`detalles` con `tipo` y `puntos`),
  no strings: la app los pinta en verde o rojo según el signo.

## Visión artificial: detalles no obvios

- El backend reenvía la foto al servicio junto con los nombres del catálogo y
  traduce las sugerencias a IDs. El servicio no toca la BD ni guarda fotos.
- `VisionClient` fuerza HTTP/1.1: el HttpClient de Java intenta `h2c` y uvicorn
  descarta el cuerpo de esas peticiones → FastAPI respondía 422.
- El modelo CLIP va fijado a una revisión con `model.safetensors` (la rama main
  del repo de Hugging Face solo tiene `.bin`). El Dockerfile la descarga al construir.
- En el Dockerfile el usuario sin privilegios se crea antes de descargar el
  modelo: un `chown` posterior duplicaba ~600 MB en otra capa.
- El estilo es la sugerencia menos fiable (subjetivo, y con 10 opciones la
  probabilidad se reparte). Se sugieren los que tengan ≥ 50 % de la
  probabilidad del mejor, máximo 3.

## Infraestructura y entorno

- Todo corre en un servidor Linux (Pop!_OS) al que se accede por Remote-SSH /
  JetBrains Gateway. La IP para SSH es `192.168.1.41`, pero la IP que usa Metro
  para que el móvil vea el backend es otra interfaz (`192.168.1.44` a fecha de
  este documento). **Verificar cuál está usando Metro** (`Metro: exp://...`)
  antes de asumir que `client.ts` apunta bien.
- `docker-compose.yml` levanta dos contenedores:
  - `style_scent_db` (MySQL 8.0): `127.0.0.1:3307` → `3306`.
  - `style_scent_vision` (FastAPI): `127.0.0.1:8001`.

  Ninguno es accesible desde la red; para MySQL desde otro equipo, túnel SSH.
  Levantar con `docker compose --env-file backend/.env up -d`.
- En el mismo servidor hay otro proyecto ajeno (`~/n8n-python`) con un
  contenedor `ml-service` en el puerto 8000. No tiene nada que ver con este.
- Credenciales en `backend/.env` (gitignored; plantilla en `backend/.env.example`).
  La contraseña de `app_user` se rotó porque la antigua quedó en el historial
  público de git.
- El esquema y los datos iniciales están en `database/` (montado en
  `/docker-entrypoint-initdb.d`): solo se cargan si el volumen está vacío.
  Si cambias el esquema o el catálogo en la BD, vuelve a exportarlo ahí
  (`mysqldump`), sin la prenda de prueba "Gorra de prueba" si sigue existiendo.
- Backend: Spring importa `backend/.env` directamente (`spring.config.import`),
  basta con `./mvnw spring-boot:run` desde `backend/`. `./mvnw test` necesita
  MySQL levantado (el test `contextLoads` arranca Spring completo).
- Frontend: Expo (managed), no React Native CLI puro. `npx expo start` en `frontend/`.

## Estado actual

- Hecho: backend completo (CRUD de prendas y perfumes, catálogos, motor de
  puntuación y recomendación, análisis de fotos), app con tres pestañas
  (Prenda / Perfume / Recomendar) y microservicio de visión (Fase 2).
- Tests: 24 en el backend (JUnit + Mockito) y 17 en el servicio de visión (pytest).
- Pendiente de verificar en un dispositivo real: la opción de foto en la
  pestaña Prenda (cámara/galería con `expo-image-picker`).
- Ideas abiertas: afinar los prompts de estilo con fotos reales del armario;
  revisar los puntos de las reglas de sinergia de los colores y estilos nuevos.

## Convenciones de commits / repo

- Repo **público** en GitHub: `Gabriel-Vallejo/style-scent-engine`. Nada de
  credenciales, IPs públicas ni datos sensibles en código, docs o commits.
- Rama principal: `main`.
- Commits separados por capa (`feat(backend)`, `feat(frontend)`, `feat(vision)`,
  `fix(db)`, `docs`...), en español.
- `.gitattributes`: el esquema SQL cuenta en la barra de lenguajes de GitHub;
  `02-datos.sql` está marcado como generado para no inflarla.
