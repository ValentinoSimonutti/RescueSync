# Sistema Nacional — Estado de avance (Parte 3 del TP)

Última actualización: 15/10/2026. Entrega 3: semana del **22/10**.

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
| M3 | Swagger completo + `openapi.json`, Dockerfile, deploy Render + Neon | ⏳ siguiente |
| M4 | Integración: backend local (CUIT, catálogo), front, endpoints para Bonita, conectores Bonita, README | ⏳ |

Tests: **21 en verde** (`./mvnw test`, necesita Docker porque usa Testcontainers).

## Cómo correrlo
```bash
cd RescueSync
docker compose up -d postgres-nacional        # Postgres del Sistema Nacional en el puerto 5434
cd sistema-nacional && ./mvnw spring-boot:run  # API en http://localhost:8090
```
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

## Próximo: M3 (Entrega 3)
1. **Swagger:** ejemplos en los DTOs (`@Schema(example=...)`), descripción de los códigos de respuesta y exportar `openapi.json` al repo.
2. **Dockerfile** multi-stage (`maven:3-eclipse-temurin-21` → `eclipse-temurin:21-jre`), usuario no root, que tome el `PORT` de Render. Agregar `.dockerignore` y sumar el servicio `sistema-nacional` al `docker-compose.yml`, con healthcheck y depends_on de `postgres-nacional`.
3. **Neon** (lo hacen ustedes con su cuenta): crear la base y armar `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`, más `DB_USER` y `DB_PASSWORD`.
4. **Render:** Web Service con Docker y Root Directory `sistema-nacional`. Variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32 caracteres o más) y `SEED_*_PASSWORD` (opcionales). Health check en `/actuator/health`.
5. Probar el flujo login → validar → comprometer → cierre contra la URL pública y anotar las URLs para la entrega.

## Después: M4 (integración)
- **Backend local:** agregar `ONG.cuit` (seed con los CUIT de arriba). `Lote.recurso` pasa a guardar el código del catálogo. Sumar un endpoint proxy `GET /api/catalogo/tipos-recurso`, que llama a la API con la cuenta `bonita` y cachea.
- **Front:** en `GenerarLotesForm.vue`, un select de tipos de recurso.
- **DTOs que lee Bonita** (`CoberturaDTO`, `OfertaDTO`, `AporteDTO`): que incluyan cuit, código de recurso, gravedad y emergenciaRef, con la misma forma que `ValidacionRequest`.
- **`BonitaIntegracionController`** (con el X-Api-Key de siempre): `POST /api/bonita/emergencias/{id}/validaciones`, para que el municipio vea solo las ofertas validadas, y avisos de compromiso y cierre.
- **Bonita Studio** (lo hacen ustedes, yo preparo los scripts):
  - Variables: `nacionalUrl`, usuario y contraseña, y `jwtToken`.
  - Conectores REST en "Evaluar Ofertas" (login + validar + POST al backend local), en "Comprometer Recursos" (manejar el 409) y en el cierre.
  - Exportar el `.bos` a `RescueSync/bonita/`.
- **README** de `sistema-nacional` y del repo raíz.

## Pendientes y notas
- Hay que revisar la zona horaria de los `LocalDateTime` en la base: en una prueba local, `fechaAlta` apareció con hora UTC. Conviene fijar `TZ=America/Argentina/Buenos_Aires` en Docker y Render, o `spring.jpa.properties.hibernate.jdbc.time_zone`.
- `ddl-auto: update` + `DataLoader`, igual que el backend. Si cambia el modelo, conviene borrar el volumen `postgres_nacional_data`.
- El `docker-compose.yml` raíz ya tiene `postgres-nacional`, pero falta sumar el servicio de la API (M3).
- No hay commits todavía: los hacen ustedes.
