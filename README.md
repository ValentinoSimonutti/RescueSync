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

El **Auditor / Directivo** sigue todo desde un dashboard que combina las tres fuentes: la base local, la API de Bonita y la API del Sistema Nacional.

## Componentes

| Carpeta | Qué es | Puerto |
|---|---|---|
| `backend/` | App local: Spring Boot 4, Java 21, PostgreSQL | 8081 |
| `frontend/` | Interfaz web: Vue 3 + TypeScript + Vite + Chart.js | 5173 |
| `sistema-nacional/` | API externa del Sistema Nacional: Spring Boot, JWT, PostgreSQL, Docker | 8090 (local) |
| `bonita/` | Proceso exportado (`.bos`) para Bonita Studio Community 2023.2 | 8080 |
| `scripts/` | `generar_datos_demo.py`: recorre el proceso completo por la API para cargar datos de prueba | — |
| `docker-compose.yml` | La base local y, opcionalmente, una copia local del Sistema Nacional | 5433, 5434, 8090 |

El Sistema Nacional está desplegado en la nube: https://rescuesync-5qaa.onrender.com (Swagger en `/swagger-ui.html`), con la base en Neon. **Bonita y el backend usan esta instancia.** Corre en el plan gratuito de Render, que se duerme después de 15 minutos sin uso y tarda alrededor de un minuto en despertar. Para que eso no corte un proceso a la mitad:

- El backend le hace un ping al arrancar y cada 10 minutos mientras haya emergencias en curso (`MantenerDespierto`).
- Los conectores de Bonita esperan hasta 120 s la respuesta (`socket_timeout_ms`).

## Requisitos

- Docker con Docker Compose
- Java 21 (el backend trae `mvnw`, no hace falta instalar Maven)
- Node.js 20 o superior
- Bonita Studio Community 2023.2

## Puesta en marcha

El orden importa. **El backend tiene que estar levantado antes de abrir Studio**: si no, el UI Designer de Studio ocupa el puerto 8081.

### 1. Base de datos local (Docker)

Desde la raíz del proyecto:

```bash
docker compose up -d postgres
```

Levanta la base local en el puerto 5433.

`docker compose up -d --build` levanta además una copia local del Sistema Nacional (base en 5434 y API en 8090), útil para desarrollar la API sin depender de Render.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Por defecto usa el Sistema Nacional de Render, que es el mismo que usa Bonita. `SISTEMA_NACIONAL_URL=http://localhost:8090` lo cambia por la copia local. En ese caso también hay que cambiar las URLs de los conectores en el diagrama: Bonita y el backend tienen que ver los mismos datos.

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
>
> El Run además arranca solo un caso vacío, que queda esperando en "Registrar Emergencia" sin ninguna emergencia asociada. Se puede cancelar desde el portal de Bonita para que no aparezca en el dashboard.

El dashboard consulta todos los casos del proceso con la sesión de Bonita de `auditor1`, así que ese usuario necesita el perfil **Administrator** en Bonita. En Studio ya lo tiene, porque en modo desarrollo todos los usuarios tienen los dos perfiles. En un Bonita de producción hay que asignárselo desde el portal (Organización → Perfiles).

### 5. Datos de prueba (opcional)

Con todo levantado y el proceso recién desplegado:

```bash
pip install requests
python3 scripts/generar_datos_demo.py
```

Corre tres emergencias en paralelo, como lo harían los usuarios desde la interfaz, con una ventana de ofertas de 1 minuto. Tarda unos 5 minutos.

1. **Inundación**: el Sistema Nacional no le valida las raciones a ONG 2, la convocatoria no cubre y el coordinador la reabre. Termina cerrada.
2. **Incendio**: dos ofertas individuales. Termina cerrada.
3. **Terremoto**: un lote que ninguna ONG puede cubrir. El coordinador sigue con cobertura parcial y la emergencia queda en ejecución, con recursos comprometidos.

Las referencias que viajan al Sistema Nacional (`EMG-{id}`) usan el id local. Si se vacía la base local, conviene hacer que los ids arranquen después de los que ya existen en el Sistema Nacional; si no, una emergencia nueva puede chocar con un proyecto viejo ya cerrado:

```sql
ALTER TABLE emergencia ALTER COLUMN id RESTART WITH 100;
```

## Usuarios

Aplicación web (la contraseña la valida Bonita):

| Usuario | Contraseña | Rol |
|---|---|---|
| `municipio1` | `bpm` | Operador Municipal |
| `coord1` | `bpm` | Centro Coordinador Regional |
| `ong1`, `ong2` | `bpm` | Representante de ONG (ONG 1 y ONG 2) |
| `auditor1` | `bpm` | Auditor / Directivo: dashboard de indicadores |

Sistema Nacional (solo si se usa la API directamente, por ejemplo desde Swagger):

| Usuario | Contraseña | Rol |
|---|---|---|
| `bonita` | `bonita123` | SISTEMA: la cuenta que usan Bonita y el backend |
| `admin` | `admin123` | ADMIN |
| `ong1`, `ong2` | `ong123` | ONG |

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
9. **`auditor1`** → **Indicadores**: tiempos de cada etapa, suministros movilizados, consorcios y recursos a nivel nacional.

## Dashboard (Auditor / Directivo)

El front pide cada fuente a un endpoint distinto del backend, solo accesible para el rol `AUDITOR`. Si una fuente no responde, solo se cae su panel.

| Endpoint | Fuente | Indicadores |
|---|---|---|
| `GET /api/dashboard/bonita` | API REST de Bonita (`/API/bpm/case`, `archivedCase`, `archivedFlowNode`, `humanTask`) | Tiempo promedio, mínimo y máximo de cada una de las 7 etapas; tiempo por tarea; casos en curso y finalizados; tareas esperando ahora |
| `GET /api/dashboard/local` | Base de datos local | Suministros movilizados por mes y recurso; % de lotes resueltos por consorcio; cobertura; participación de cada ONG; resultado de la validación; proyectos finalizados con su duración |
| `GET /api/dashboard/sistema-nacional` | API del Sistema Nacional | Inventario, comprometido y disponible por recurso; compromisos activos y liberados; ONGs por estado |

`desde` y `hasta` (`yyyy-MM-dd`, opcionales) filtran por la fecha de registro de la emergencia y por la fecha de inicio del caso.

Los tiempos de Bonita salen de los registros archivados de cada tarea: Bonita guarda uno por cada estado (`initializing` → `ready` → `completed`/`aborted`). La duración va desde el primer estado hasta el estado final. La etapa "Convocatoria y ofertas" mide la ventana real, porque el timer interrumpe "Cargar o editar ofertas".

Cada gráfico se puede ver como tabla y exportar a CSV, y el botón **Exportar todo** baja un JSON con las tres fuentes.

## Integración Bonita ↔ backend ↔ Sistema Nacional

Bonita llama al backend con el header `X-Api-Key: rescuesync-bonita-2026` y al Sistema Nacional con el JWT de la cuenta `bonita`. Todos los conectores son REST (`bonita-connector-rest`).

| Tarea de Bonita | Conectores |
|---|---|
| Obtener ofertas | `GET /api/bonita/emergencias/{id}/ofertas` (backend) → `ofertasJson`, `lotesCubiertos` |
| Evaluar Ofertas | login en el SN → `POST /api/validaciones/ofertas` (SN) → `POST /api/bonita/emergencias/{id}/validaciones` (backend) → `lotesCubiertos` |
| Comprometer Recursos | `GET /api/bonita/emergencias/{id}/compromiso` (backend) → login en el SN → `POST /api/compromisos` (SN, sin fallar en 4xx para recibir el 409) → `POST /api/bonita/emergencias/{id}/compromiso` (backend) → `compromisoOk` |
| Ejecutar y marcar tareas finalizadas (al salir) | login en el SN → `POST /api/proyectos/EMG-{id}/cierre` (SN) → `POST /api/bonita/emergencias/{id}/cierre` (backend) |

Los conectores al Sistema Nacional apuntan a `https://rescuesync-5qaa.onrender.com`; los conectores al backend, a `http://localhost:8081`.

Las tareas humanas (Generar lotes, Cargar o editar ofertas, Adjudicar y notificar, Monitorear estado de despliegue, Ejecutar y marcar tareas finalizadas) se resuelven desde la app. El backend las completa con `POST /API/bpm/userTask/{id}/execution`, usando la sesión de Bonita del usuario logueado.

Las referencias que viajan al Sistema Nacional son `EMG-{id}`, `OF-{id}` y `LT-{id}`: los ids de la base local.
