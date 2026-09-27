# Contexto del proyecto (para retomar en Claude Code)

## Qué es
Style & Scent Engine: analiza el outfit seleccionado y recomienda el perfume ideal
de la colección real del usuario, con un motor de puntuación basado en reglas.

## Decisiones de diseño importantes (no obvias mirando solo el código)

- **`sinergias_color` y `sinergias_estilo` son tablas separadas**, no una sola
  `sinergias_puntuacion` genérica. Se separaron porque el diseño original mezclaba
  reglas de color y de estilo en una tabla con texto libre (`atributo_ropa`), lo que
  causaba bugs de matching silenciosos.
- **Bug histórico ya resuelto (2)**: `sinergias_estilo` tenía cada regla
  triplicada (el script de carga se ejecutó 3 veces) y Streetwear puntuaba x3.
  Ahora hay `UNIQUE (id_estilo, id_familia)` y `UNIQUE (id_color, id_familia)`.
- **Catálogo ampliado** a 29 colores y 10 estilos, todos con reglas de
  sinergia (criterios de perfumería habituales, escala 10-30, revisables).
  Para añadir más: insertar reglas buscando por nombre (`JOIN ... ON f.nombre =
  '...'`) y comprobar el número de filas insertadas, por el tema de las tildes.
  Un color nuevo necesita además su RGB en `vision-service/app/etiquetas.py`
  (y un estilo nuevo, idealmente, sus prompts ahí mismo).
- **Bug histórico ya resuelto**: hubo un typo `'Ambar Fougere'` (sin tildes) en
  reglas de sinergia que nunca matcheaba con `'Ámbar Fougère'` en `familias_olfativas`.
  Si aparecen reglas que "no disparan", revisar tildes primero.
- **`prenda_estilo` es M:N**, no 1:1 — una prenda puede tener varios estilos a la
  vez (ej. las Ray-Ban Meta son Streetwear + Casual). `Prenda.estilos` es un `Set`,
  no una `List`; ojo con ese tipo al escribir código nuevo.
- **`GlobalExceptionHandler`** centraliza errores: `EntityNotFoundException` → 404,
  `IllegalArgumentException` → 400. Cualquier excepción nueva de negocio debería
  añadirse ahí, no manejarse con try/catch ad-hoc en los controllers.
- **`StyleScentService` es solo el motor de puntuación.** El CRUD de prendas vive
  en `PrendaService` y el de perfumes en `PerfumeService` — no mezclar
  responsabilidades ahí.
- **IDs son `Integer` en todo el proyecto** (entidades, repos, DTOs), no `Long` —
  decisión deliberada para que coincida con el `INT AUTO_INCREMENT` de MySQL.

## Infraestructura y entorno

- Todo corre en un servidor Linux (Pop!_OS) al que se accede por Remote-SSH /
  JetBrains Gateway. La IP para SSH es `192.168.1.41`, pero la IP que usa Metro
  para que el móvil vea el backend es otra interfaz (`192.168.1.44` a fecha de
  este documento) — **verificar cuál está usando Metro** (`Metro: exp://...`)
  antes de asumir que `client.ts` apunta bien.
- MySQL corre en Docker, puerto `3307` del host → `3306` del contenedor.
  Credenciales en `backend/.env` (gitignored; plantilla en `backend/.env.example`).
  Levantar con `docker compose --env-file backend/.env up -d`.
- El esquema y los datos iniciales están en `database/` (montado en
  `/docker-entrypoint-initdb.d`): solo se cargan si el volumen está vacío.
  Si cambias el esquema en la BD, vuelve a exportarlo ahí.
- Backend: Spring importa `backend/.env` directamente (`spring.config.import`),
  basta con `./mvnw spring-boot:run` desde `backend/`.
- Frontend: Expo (managed), no React Native CLI puro. `npx expo start` en `frontend/`.

## Estado actual (ver también README.md en la raíz)

- Backend: CRUD de prendas completo, listado de perfumes, motor de puntuación
  con tests unitarios, endpoint `/api/match` funcionando.
- Frontend: tres pestañas (Prenda / Perfume / Recomendar), todas conectadas
  al backend real, sin datos mock. Recomendar recarga datos al enfocarse.
  En Recomendar el perfume es opcional: sin perfume elegido llama a
  `/api/match/recomendar` y enseña el ganador + alternativas.
  Las pestañas Prenda y Perfume tienen debajo del formulario la lista para
  borrar (y, en perfumes, cambiar de estado con un toque).
- `GET /api/perfumes` sin parámetros devuelve solo "En coleccion" a propósito
  (es lo que usa Recomendar); la gestión usa `?todos=true`.
- El desglose del match es estructurado (`detalles` con `tipo` y `puntos`),
  no strings: la app los pinta en verde/rojo según el signo.
- Fase 2 hecha: `vision-service/` (FastAPI, puerto 8001 solo en 127.0.0.1).
  El backend le reenvía la foto con los nombres del catálogo
  (`POST /api/prendas/analizar`) y traduce las sugerencias a IDs.
  El servicio no toca la BD ni guarda fotos. Color con GrabCut + k-means en
  Lab; categoría y estilo con CLIP zero-shot (prompts en `app/etiquetas.py`).
- Detalles no obvios de la Fase 2:
  - `VisionClient` fuerza HTTP/1.1: el HttpClient de Java intenta `h2c` y
    uvicorn descarta el cuerpo → FastAPI respondía 422.
  - El modelo CLIP va fijado a una revisión con `model.safetensors` (la rama
    main del repo solo tiene `.bin`). El Dockerfile la descarga al construir.
  - Un color nuevo del catálogo necesita su RGB en `COLORES_RGB`
    (`etiquetas.py`); si ningún color tiene referencia, decide CLIP.
  - El estilo es la sugerencia menos fiable (subjetivo y con 10 opciones
    que se reparten la probabilidad). Se sugieren los que tengan ≥ 50 % de la
    probabilidad del mejor, máximo 3.

## Convenciones de commits / repo

- Repo **público** en GitHub: `Gabriel-Vallejo/style-scent-engine`. Nada de
  credenciales, IPs públicas ni datos sensibles en código, docs o commits.
- Rama principal: `main`.
