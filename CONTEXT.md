# Contexto del proyecto (para retomar en Claude Code)

## Qué es
Style & Scent Engine: analiza el outfit seleccionado y recomienda el perfume ideal
de la colección real del usuario, con un motor de puntuación basado en reglas.

## Decisiones de diseño importantes (no obvias mirando solo el código)

- **`sinergias_color` y `sinergias_estilo` son tablas separadas**, no una sola
  `sinergias_puntuacion` genérica. Se separaron porque el diseño original mezclaba
  reglas de color y de estilo en una tabla con texto libre (`atributo_ropa`), lo que
  causaba bugs de matching silenciosos.
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
  Credenciales en `backend/.env` (gitignored), no hardcodeadas en `docker-compose.yml`.
- Backend: `export DB_USERNAME=... DB_PASSWORD=...` antes de `./mvnw spring-boot:run`.
- Frontend: Expo (managed), no React Native CLI puro. `npx expo start` en `frontend/`.

## Estado actual (ver también README.md en la raíz)

- Backend: CRUD de prendas completo, listado de perfumes, motor de puntuación
  con tests unitarios, endpoint `/api/match` funcionando.
- Frontend: tres pestañas (Prenda / Perfume / Recomendar), todas conectadas
  al backend real, sin datos mock. Recomendar recarga datos al enfocarse.
- Pendiente: pulir la pantalla de resultado del match, Fase 2 (IA de visión
  artificial).

## Convenciones de commits / repo

- Repo privado en GitHub: `Gabriel-Vallejo/style-scent-engine`.
- Rama principal: `main`.
