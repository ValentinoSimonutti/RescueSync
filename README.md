# RescueSync

RescueSync coordina la respuesta ante desastres naturales a nivel regional: conecta a los municipios afectados con ONGs y organizaciones de rescate para distribuir recursos y personal.

El ciclo de vida de cada emergencia lo orquesta **Bonita**:

1. El municipio registra la emergencia.
2. El centro coordinador arma los lotes de necesidades y abre una convocatoria con plazo.
3. Las ONGs ofertan, solas o en consorcio.
4. Cuando vence el plazo, Bonita valida las ofertas contra la API del **Sistema Nacional**.
5. El municipio adjudica.
6. Bonita compromete los recursos en el Sistema Nacional.
7. Las ONGs ejecutan y marcan sus actividades como finalizadas.
8. Bonita cierra el proyecto y libera los recursos.

## Componentes

| Carpeta | Qué es | Puerto |
|---|---|---|
| `backend/` | App local: Spring Boot 4, Java 21, PostgreSQL | 8081 |
| `frontend/` | Interfaz web: Vue 3 + TypeScript + Vite | 5173 |
| `sistema-nacional/` | API externa del Sistema Nacional: Spring Boot, JWT, PostgreSQL. Detalle en `sistema-nacional/PROGRESO.md` | 8090 |
| `bonita/` | Proceso exportado (`.bos`) para Bonita Studio Community 2023.2 | 8080 |
| `docker-compose.yml` | Las dos bases de datos y la API del Sistema Nacional | 5433, 5434, 8090 |

El Sistema Nacional también está desplegado en la nube: https://rescuesync-5qaa.onrender.com (Swagger en `/swagger-ui.html`). Corre en el plan gratuito de Render, que se duerme después de 15 minutos sin uso: el primer pedido puede tardar alrededor de un minuto.

## Requisitos

- Docker con Docker Compose
- Java 21 (el backend trae `mvnw`, no hace falta instalar Maven)
- Node.js 20 o superior
- Bonita Studio Community 2023.2

## Puesta en marcha

El orden importa. **El backend tiene que estar levantado antes de abrir Studio**: si no, el UI Designer de Studio ocupa el puerto 8081.

### 1. Bases de datos y Sistema Nacional (Docker)

Desde la raíz del proyecto:

```bash
docker compose up -d --build
```

Levanta la base local (5433), la base del Sistema Nacional (5434) y la API del Sistema Nacional (8090). La API crea sus datos de prueba al arrancar. Para verificar que está andando: http://localhost:8090/actuator/health.

### 2. Backend

```bash
cd backend
SISTEMA_NACIONAL_URL=http://localhost:8090 ./mvnw spring-boot:run
```

Sin `SISTEMA_NACIONAL_URL`, el backend usa el Sistema Nacional de Render. Para probar en local conviene usar el de Docker: Bonita valida contra `localhost:8090` y los dos tienen que ver los mismos datos.

Al arrancar, el backend crea los usuarios, el municipio, el centro coordinador y las ONGs (ver [Usuarios](#usuarios)).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda en http://localhost:5173.

### 4. Bonita Studio

1. En Studio, **Preferences → Server settings**: dejá el puerto de Tomcat en **8080**, que es donde lo busca el backend.
2. **File → Import** y elegí el `.bos` de `bonita/`.
3. Desplegá la organización: **Organization → Deploy** → `RescueSync`. Los usuarios tienen la contraseña `bpm`.
4. Abrí el diagrama `TPIntegrador` y hacé **Run**. Eso despliega el proceso RescueSync 1.0 con sus actores (Municipio, Coordinador, ONG) ya mapeados.

> **Cada Run vuelve a desplegar el proceso y borra todos los casos en curso.** Si lo hacés en medio de una prueba, esas emergencias quedan trabadas en su estado y hay que crear una nueva. Guardá el diagrama (Ctrl+S) antes del Run.

## Usuarios

Sistema Nacional (solo si se usa la API directamente, por ejemplo desde Swagger):

| Usuario | Contraseña | Rol |
|---|---|---|
| `bonita` | `bonita123` | SISTEMA: la cuenta que usan Bonita y el backend |
| `admin` | `admin123` | ADMIN |
| `ong1`, `ong2` | `ong123` | ONG |.

## Flujo completo de prueba

1. **`municipio1`** → Registrar Emergencia.
2. **`coord1`** → Tareas pendientes → Generar lote. Para probar rápido, poné una ventana de 1 o 2 minutos.
3. **`ong1` / `ong2`** → ofertan cada lote completo, o arman un consorcio para aportar una parte.
4. Vence el plazo. Bonita pide las ofertas al backend, las valida en el Sistema Nacional y le reenvía el resultado al backend.
   - **Si cubre:** `municipio1` ve **Adjudicar** en Mis Emergencias y elige una oferta validada por lote.
   - **Si no cubre:** `coord1` ve la convocatoria en "Vencidas sin cobertura", con lo que validó el Sistema Nacional y el motivo de cada rechazo.
5. Bonita compromete los recursos en el Sistema Nacional.
   - Si otra emergencia ya los tomó (409), vuelve a "Adjudicar y notificar": el municipio ve el motivo y la oferta rechazada ya no aparece.
6. **`coord1`** → Revisar despliegue → **Iniciar despliegue**. El avance se sigue en "Despliegues".
7. **`ong1` / `ong2`** → **Marcar actividades como finalizadas**. El backend le avisa al Sistema Nacional, que libera los recursos de esa ONG.
8. Cuando finaliza la última ONG, Bonita cierra el proyecto en el Sistema Nacional y la emergencia queda **Cerrada**.

## Integración Bonita ↔ backend ↔ Sistema Nacional

Bonita llama al backend con el header `X-Api-Key: rescuesync-bonita-2026` y al Sistema Nacional con el JWT de la cuenta `bonita`. Todos los conectores son REST (`bonita-connector-rest`).

| Tarea de Bonita | Conectores |
|---|---|
| Obtener ofertas | `GET /api/bonita/emergencias/{id}/ofertas` (backend) → `ofertasJson`, `lotesCubiertos` |
| Evaluar Ofertas | login en el SN → `POST /api/validaciones/ofertas` (SN) → `POST /api/bonita/emergencias/{id}/validaciones` (backend) → `lotesCubiertos` |
| Comprometer Recursos | `GET /api/bonita/emergencias/{id}/compromiso` (backend) → login en el SN → `POST /api/compromisos` (SN, sin fallar en 4xx para recibir el 409) → `POST /api/bonita/emergencias/{id}/compromiso` (backend) → `compromisoOk` |
| Ejecutar y marcar tareas finalizadas (al salir) | login en el SN → `POST /api/proyectos/EMG-{id}/cierre` (SN) → `POST /api/bonita/emergencias/{id}/cierre` (backend) |

Las tareas humanas (Generar lotes, Cargar o editar ofertas, Adjudicar y notificar, Monitorear estado de despliegue, Ejecutar y marcar tareas finalizadas) se resuelven desde la app. El backend las completa con `POST /API/bpm/userTask/{id}/execution`, usando la sesión de Bonita del usuario logueado.

Las referencias que viajan al Sistema Nacional son `EMG-{id}`, `OF-{id}` y `LT-{id}`: los ids de la base local.
