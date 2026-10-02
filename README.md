# RescueSync
RescueSync es una plataforma orientada a coordinar la respuesta ante desastres naturales a nivel regional, conectando municipios afectados con organizaciones de rescate, ONGs y entes gubernamentales para optimizar la distribución solidaria de recursos y personal.

# Entrega Etapa 2

## Estructura del proyecto
- `/backend`: Servidor en Spring Boot (IntelliJ).
- `/frontend`: Interfaz en Vue.js (VS Code).
- `/bonita`: Diagrama y proceso exportado (`.bos`).
- `docker-compose.yml`: Archivo de infraestructura para levantar los contenedores.

## Instrucciones para la ejecución

### 1. Infraestructura (Docker)
1. Abrir una terminal en la raíz del proyecto (donde se encuentra el archivo `docker-compose.yml`).
2. Ejecutar el comando para levantar los servicios:
   `docker compose up -d`

### 2. Backend (Spring Boot en IntelliJ)
1. Abrir la carpeta `backend` en IntelliJ IDEA.
2. Verificar que la configuración de la base de datos apunte al contenedor.
3. Ejecutar la clase principal de Spring Boot para encender el servidor.

### 3. Frontend (Vue.js en VS Code)
1. Abrir la terminal en la carpeta `frontend`.
2. Instalar dependencias ejecutando: `npm install`
3. Ejecutar el proyecto con: `npm run dev`

### 4. Proceso BPM (Bonita Open Studio)
1. Abrir Bonita Open Studio.
2. Ir a **Server > Import > Process (.bos)** y seleccionar el archivo ubicado dentro de la carpeta `/bonita`.
3. Una vez importado el proyecto, hacer clic en **Deploy** y luego en **Run** para poner en marcha el proceso y sus formularios asociados.