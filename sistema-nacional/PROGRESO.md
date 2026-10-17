# Sistema Nacional — Estado de avance (Parte 3 del TP)

Última actualización: 16/10/2026. Entrega 3: semana del **22/10**.

API externa "Sistema Nacional de Gestión de Recursos y Riesgos": JWT, dockerizada, desplegada en un cloud, con Swagger.
Bonita orquesta: cuando vence el timer, pide las ofertas al backend local, las manda a esta API para validarlas, después compromete los recursos adjudicados y al final los libera.

## Decisiones tomadas
- **Stack:** Spring Boot 4.1.1 + Java 21 (igual que `backend/`). El código está en `RescueSync/sistema-nacional/`.
- **Deploy:** la API en **Render** (Docker, con Root Directory `sistema-nacional`) y Postgres en **Neon**. La Postgres gratis de Render vence a los 30 días y el coloquio es el 26/11.
- **ONG local ↔ nacional:** se vinculan por **CUIT** (11 dígitos, se valida el dígito verificador).
- **Recursos de los lotes:** usan el **código del catálogo nacional** (ej. `PARAMEDICO`).
- **Alcance:** API + integración (backend local, front y conectores de Bonita).
- **Certificaciones:** las otorga el ADMIN; la ONG no se las autodeclara.
- **Forma de trabajo:** por milestones. Se cierra uno, se revisa y recién después se sigue.

## Milestones

| | Milestone | Estado |
|---|---|---|
| M0 | Scaffold (pom, yaml con env vars, Postgres en compose) | ✅ |
| M1 | Dominio + seed, auth JWT, onboarding, catálogo y consultas | ✅ |
| M2 | Validación por niveles, compromiso con locking, liberación y cierre | ✅ |
| M3 | Zona horaria GMT-3, Swagger completo + `openapi.json`, Dockerfile y compose | ✅ (deploy en Render + Neon funcionando) |
| M4 | Integración: backend local (CUIT, catálogo), front, endpoints para Bonita, conectores Bonita, README | ⏳ |

Tests: **23 en verde** (`./mvnw test`, necesita Docker porque usa Testcontainers).

## Cómo correrlo
```bash
cd RescueSync
docker compose up -d postgres-nacional        # Postgres del Sistema Nacional en el puerto 5434
cd sistema-nacional && ./mvnw spring-boot:run  # API en http://localhost:8090
```
O todo en Docker, con la misma imagen que corre en Render: `docker compose up -d --build sistema-nacional` (levanta también `postgres-nacional`).
- Swagger: http://localhost:8090/swagger-ui.html. Primero se hace login, después se toca **Authorize** y se pega el token.
- Health: http://localhost:8090/actuator/health

**Usuarios de prueba** (los crea `DataLoader` con la base vacía):

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `bonita` | `bonita123` | SISTEMA (cuenta que usa Bonita) |
| `ong1`, `ong2`, `brigada-andina`, `red-litoral`, `manos-delta`, `refugio` | `ong123` | ONG |

**ONGs del seed:**

| ONG | CUIT | Para qué sirve |
|---|---|---|
| ONG 1 | 30712345671 | igual que en la app local; AVANZADO en primeros auxilios y logística |
| ONG 2 | 30700000016 | igual que en la app local; BÁSICO en primeros auxilios, AVANZADO en incendios |
| Brigada Andina | 30698765433 | certificación de primeros auxilios **vencida** |
| Red Solidaria del Litoral | 30711223343 | rescate acuático AVANZADO |
| Manos del Delta | 30705556667 | **PENDIENTE** |
| Fundación Refugio | 30709876542 | **SUSPENDIDA** |

## Endpoints implementados (`/api`)

| Endpoint | Rol |
|---|---|
| `POST /auth/login` | público |
| `POST /ongs/registro` (onboarding, la ONG queda PENDIENTE) | público |
| `GET /ongs?estado=&certificacion=&tipoRecurso=`, `GET /ongs/{cuit}` | autenticado |
| `PATCH /ongs/{cuit}/estado`, `POST /ongs/{cuit}/certificaciones` | ADMIN |
| `GET /ongs/me`, `PUT /ongs/me/inventario/{tipoRecurso}` | ONG |
| `GET /catalogo/certificaciones`, `GET /catalogo/tipos-recurso`, `GET /recursos/disponibilidad?tipoRecurso=` | autenticado |
| `POST /validaciones/ofertas` | SISTEMA, ADMIN |
| `POST /compromisos/verificar` (prueba en seco, no bloquea) | SISTEMA, ADMIN |
| `POST /compromisos` (todo o nada, idempotente) | SISTEMA |
| `GET /compromisos?emergenciaRef=&ongCuit=&estado=` (una ONG solo ve los suyos) | autenticado |
| `GET /proyectos/{ref}` | SISTEMA, ADMIN |
| `POST /proyectos/{ref}/ongs/{cuit}/finalizacion`, `POST /proyectos/{ref}/cierre` | SISTEMA |

Los errores salen como ProblemDetail (`status`, `title`, `detail`). El 409 de un compromiso rechazado trae `items` con el motivo de cada aporte.

## Reglas clave (para el informe y el coloquio)
**Validación por niveles** (`service/EvaluadorCompetencia.java`). No rechaza ni acepta: arma un perfil por aporte.
- Nivel de certificación → riesgo máximo: BÁSICO → BAJA, INTERMEDIO → MEDIA, AVANZADO → ALTA.
- **PRINCIPAL:** tiene la certificación vigente y su nivel cubre la gravedad, o el recurso no requiere certificación.
- **APOYO_SECUNDARIO:** el nivel no alcanza para la gravedad, o la certificación está vencida.
- **SIN_HABILITACION_VIGENTE:** no tiene la certificación, la ONG está pendiente o suspendida, o no está registrada.
- Una oferta de consorcio es PRINCIPAL si al menos una ONG lo es. La cantidad validable es el mínimo entre lo ofrecido y lo disponible.

**Concurrencia** (`service/CompromisoService.java`). El bloqueo es **pesimista** y el `@Version` queda como respaldo optimista.
1. El proyecto se crea con `INSERT ... ON CONFLICT DO NOTHING` y se bloquea con `SELECT ... FOR UPDATE`.
2. Se obtienen solo los ids de las filas de inventario, sin cargarlas, para no leer valores viejos.
3. Se bloquean con `SELECT ... FOR UPDATE ... ORDER BY id`. El orden fijo evita deadlocks.
4. Se valida y se descuenta. Si algo falla, se lanza una excepción y se hace rollback de todo.

La liberación bloquea en el mismo orden. La demo está en `ConcurrenciaCompromisoTest`: 10 emergencias piden en paralelo los 3 botes de ONG 1, ganan exactamente 3 y el resto recibe 409. Si se saca el `FOR UPDATE`, el test falla con `ObjectOptimisticLockingFailureException`.

## M3: lo que quedó hecho
- **Zona horaria:** todo está en GMT-3 (`America/Argentina/Buenos_Aires`).
  - `config/ZonaHorariaConfig` fija la zona de la JVM: los `LocalDateTime.now()` se guardan sin zona y en Docker/Render la JVM arranca en UTC.
  - Surefire corre los tests con `-Duser.timezone=UTC` para simular la nube, y `ZonaHorariaTest` verifica que igual salga GMT-3.
  - `spring.jackson.time-zone` también está en Buenos Aires.
  - En el compose, las dos Postgres tienen `TZ`/`PGTZ`, así `now()` y psql muestran -03.
  - El Dockerfile tiene `ENV TZ`.
- **Swagger:** ejemplos (`@Schema(example)`) en todos los DTOs de request.
  - Los errores comunes los agrega solo un `OperationCustomizer` en `OpenApiConfig`, mirando la firma del método: body → 400, token → 401, `@PreAuthorize` → 403, path variable → 404.
  - Los 409 están anotados a mano en registro, verificar y comprometer.
  - Hay un schema `Error` con la forma del ProblemDetail.
- **`openapi.json`** exportado en `sistema-nacional/`. Para regenerarlo, con la API levantada:
  `curl -s localhost:8090/v3/api-docs | python3 -m json.tool --indent 2 > openapi.json`
- **Docker:** `Dockerfile` multi-stage (con las dependencias cacheadas en una capa), JRE 21, usuario no root y `MaxRAMPercentage=75`, más `.dockerignore`. En el compose: servicio `sistema-nacional` con healthcheck en `/actuator/health` y `depends_on` de `postgres-nacional` con `service_healthy`.

## Pendiente de M3: deploy (lo hacen ustedes con sus cuentas)
**Estado al 16/10:** Neon ✅. El proyecto `rough-wildflower-34275101` (branch `production`, base `neondb`) ya tiene corrido el `ALTER DATABASE ... timezone` y la connection string está copiada.
Render ✅ (16/10): https://rescuesync-5qaa.onrender.com — Swagger en `/swagger-ui.html`. Probado: health UP, login `bonita` y catálogo OK. Env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (las `SEED_*` quedan en los defaults).
Antes: no dejaba elegir el repo RescueSync. Lo más probable es que la GitHub App de Render no tenga acceso a ese repo. Se arregla en GitHub → Settings → Applications → Render → Configure → Repository access, o con "Configure account" desde Render. Si el repo es de una organización, la org también tiene que aprobar la app.
Del onboarding que propone Neon (`neon init` / `neon deploy` / `.env.local`) **no hace falta nada**: está pensado para apps de Node y acá alcanza con las env vars `DB_*`.

1. **Neon:** crear el proyecto (región `sa-east-1` / São Paulo si está disponible) y en el SQL Editor correr:
   `ALTER DATABASE <db> SET timezone TO 'America/Argentina/Buenos_Aires';`
   Después armar `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`, `DB_USER` y `DB_PASSWORD`. Con el host *pooled* (`-pooler`) también funciona; si hay problemas, usar el directo.
2. **Render:** New → Web Service, conectar el repo, Runtime **Docker**, Root Directory `sistema-nacional`, Health Check Path `/actuator/health`.
   Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32 caracteres o más, ej. `openssl rand -base64 48`) y opcionalmente `SEED_ADMIN_PASSWORD`, `SEED_SISTEMA_PASSWORD` y `SEED_ONG_PASSWORD`. `PORT` lo pone Render.
3. El plan free se duerme a los 15 minutos sin uso y el primer request tarda alrededor de 1 minuto. Antes de una demo, entrar a `/actuator/health`.
4. Probar contra la URL pública: login → `POST /api/validaciones/ofertas` → `POST /api/compromisos` → `POST /api/proyectos/{ref}/cierre`, y anotar las URLs (API + Swagger) para la entrega.

## Después: M4 (integración)
Plan aprobado el 16/10, en tres milestones:
- ✅ **M4a** (16/10, falta probarlo en el navegador con Bonita): backend local + front (CUIT, catálogo, `SistemaNacionalClient`, select en `GenerarLotesForm`).
  - `backend/.../sistemanacional/`: `SistemaNacionalClient` (login con la cuenta `bonita`, JWT cacheado, reintento si responde 401, catálogo cacheado 10 min; si la API cae, sigue usando el último catálogo; timeout de lectura de 90 s por el arranque en frío de Render) y un handler que devuelve 502 con `mensaje`.
  - Config `sistema-nacional.url/usuario/password` (env `SISTEMA_NACIONAL_*`). Por defecto apunta a Render; para la local, `SISTEMA_NACIONAL_URL=http://localhost:8090`.
  - `ONG.cuit` (seed y backfill por nombre para las bases existentes). `Lote` guarda `recurso` (el nombre para mostrar) y `recursoCodigo` (el código del catálogo); al publicar se valida el código contra el catálogo y el nombre sale de ahí. `LoteDTO` y `CoberturaLoteDTO` exponen `recursoCodigo`.
  - `GET /api/catalogo/tipos-recurso` (con sesión). En el front, `catalogoService.ts` y un select agrupado por categoría en `GenerarLotesForm`, que no deja repetir recursos.
- **M4b:** endpoints para Bonita (`validacionRequest` armado en el GET de ofertas, POST de validaciones, avisos de compromiso y cierre, etiqueta del rol en el front).
- **M4c:** scripts y guía de Bonita en `RescueSync/bonita/`, README.

- **Backend local:** agregar `ONG.cuit` (seed con los CUIT de arriba). `Lote.recurso` pasa a guardar el código del catálogo. Sumar un endpoint proxy `GET /api/catalogo/tipos-recurso`, que llama a la API con la cuenta `bonita` y cachea.
- **Front:** en `GenerarLotesForm.vue`, un select de tipos de recurso.
- **DTOs que lee Bonita** (`CoberturaDTO`, `OfertaDTO`, `AporteDTO`): que incluyan cuit, código de recurso, gravedad y emergenciaRef, con la misma forma que `ValidacionRequest`.
- **`BonitaIntegracionController`** (con el X-Api-Key de siempre): `POST /api/bonita/emergencias/{id}/validaciones`, para que el municipio vea solo las ofertas validadas, y avisos de compromiso y cierre.
- **Bonita Studio** (lo hacen ustedes, yo preparo los scripts):
  - Variables: `nacionalUrl`, usuario y contraseña, y `jwtToken`.
  - Conectores REST en "Evaluar Ofertas" (login + validar + POST al backend local), en "Comprometer Recursos" (manejar el 409) y en el cierre.
  - Exportar el `.bos` a `RescueSync/bonita/`.
- **README** de `sistema-nacional` y del repo raíz.

## Al retomar (dejado el 16/10)
1. **Probar M4a en el navegador** (no se llegó a probar):
   - `municipio1` crea una emergencia.
   - `coord1` abre "Generar lotes": select del catálogo, aviso de recurso repetido, nombres en el modal, publicar.
   - `ong1` ve los lotes y hace una oferta.
   - Opcional: `select id, recurso, recurso_codigo from lote` para ver el código guardado.
2. Puertos: **Bonita 8080, backend 8081, front 5173**. El 16/10 se había cambiado `server.port` a 8080 por error y se volvió a 8081. Si el login da "Error HTTP: 502" sin mensaje, es Vite sin backend en el 8081.
3. Las contraseñas de los usuarios locales son las de la organización de Bonita (el backend hace login contra Bonita).
4. El usuario va a traer el archivo de Bonita (`.bos`) para orquestar. Con eso se sigue con **M4b** (endpoints y DTOs para Bonita) y **M4c** (conectores y scripts).
5. Commits: los hace el usuario (M4a sin commitear todavía).

## Pendientes y notas
- `ddl-auto: update` + `DataLoader`, igual que el backend. Si cambia el modelo, conviene borrar el volumen `postgres_nacional_data`.
- No hay commits todavía: los hacen ustedes.
- **17/10, flujo de prueba de M4a:** el `.bos` venía de Studio 10.4. Se armó `rescuesync_9.0.bos` (MANIFEST, pom y `bonitaModelVersion` bajados) y se importó en Studio 2023.2 (9.0). El Tomcat de Studio quedó fijo en el 8080. Hay que levantar el backend **antes** que Studio, porque si no el UI Designer agarra el 8081.
  - Fixes de front: el select de lotes deshabilita los recursos que ya se eligieron en otro lote, y el consorcio no se ofrece si el lote pide 1 unidad.
  - Render no respondió el health check: hay que revisar los logs. Mientras tanto, el backend usa la API local (`SISTEMA_NACIONAL_URL=http://localhost:8090`).
- **Tope por lote (17/10, decidido por ahora y tratado como bug):** sumando todas las ofertas activas, lo confirmado no puede pasar lo que pide el lote. Para sumar recursos a la oferta de otra ONG hay que armar un consorcio. Backend: `OfertaService` bloquea el lote (`findByIdParaOfertar`, FOR UPDATE) y limita al faltante al crear y al aceptar o subir un aporte; bajar el aporte siempre se puede. En el front, `OfertaLote` usa `cobertura.faltante` como tope y muestra "ya está cubierto". Queda para revisar con el grupo al final.
- **Mensajes de error:** en Boot 4 la propiedad es `spring.web.error.include-message`; `server.error.*` ya no aplica. Por eso el front mostraba "Error HTTP: 400" sin el detalle.
- **Falta antes del flujo completo:** el municipio no tiene bandeja. "Adjudicar y notificar" queda `ready` en Bonita y no se ve en el front. Va en M4b.
