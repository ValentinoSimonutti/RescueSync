<script setup lang="ts">
import { onMounted, ref } from "vue";
import LotesONG from "./components/LotesONG.vue";
import Login from "./components/Login.vue";
import EmergenciaForm from "./components/EmergenciaForm.vue";
import MisEmergencias from "./components/MisEmergencias.vue";
import BandejaTareas from "./components/BandejaTareas.vue";
import GenerarLotesForm from "./components/GenerarLotesForm.vue";
import type { Tarea } from "./services/tareaService";
import { logout, usuarioActual, type Usuario } from "./services/authService";

const modalSalir = ref<HTMLDialogElement | null>(null);
const cerrandoSesion = ref(false);
const errorSalir = ref("");

function pedirConfirmacionSalir() {
  errorSalir.value = "";
  modalSalir.value?.showModal();
}

function cancelarSalida() {
  if (!cerrandoSesion.value) modalSalir.value?.close();
}

const mensajeMunicipio = ref("");
const mensajeCoordinador = ref("");

function alPublicarLotes() {
  mensajeCoordinador.value = "Convocatoria publicada con éxito.";
  tareaSeleccionada.value = null;
}

function seleccionarTarea(tarea: Tarea) {
  mensajeCoordinador.value = "";
  tareaSeleccionada.value = tarea;
}

function cambiarPantallaMunicipio(pantalla: "registrar" | "misEmergencias") {
  mensajeMunicipio.value = "";
  pantallaMunicipio.value = pantalla;
}

function alRegistrarEmergencia() {
  mensajeMunicipio.value = "Emergencia registrada con éxito.";
  pantallaMunicipio.value = "misEmergencias";
}

const pantallaMunicipio = ref<"registrar" | "misEmergencias">("registrar");
const usuario = ref<Usuario | null>(null);   // null = nadie logueado
const tareaSeleccionada = ref<Tarea | null>(null);   // tarea que el coordinador eligió en la bandeja

// Al abrir la página preguntamos a Spring si ya hay una sesión (ej: después de un F5)
onMounted(async () => {
  try {
    usuario.value = await usuarioActual();
  } catch {
    usuario.value = null;   // 401 = no hay sesión, se muestra el login
  }
});

function alLoguearse(u: Usuario) {
  mensajeCoordinador.value = "";
  mensajeMunicipio.value = "";
  pantallaMunicipio.value = "registrar";
  usuario.value = u;
}

async function salir() {
  if (cerrandoSesion.value) return;
  cerrandoSesion.value = true;
  errorSalir.value = "";
  try {
    await logout();
    modalSalir.value?.close();
    usuario.value = null;
    mensajeMunicipio.value = "";
    mensajeCoordinador.value = "";
    tareaSeleccionada.value = null;
    pantallaMunicipio.value = "registrar";
  } catch (e) {
    errorSalir.value = e instanceof Error ? e.message : "No se pudo cerrar la sesión. Intentá nuevamente.";
  } finally {
    cerrandoSesion.value = false;
  }
}

function mostrarRol(rol: Usuario["rol"]) {
  if (rol === "CCR") return "Centro Coordinador Regional";

  return rol
    .toLocaleLowerCase("es")
    .split("_")
    .map((palabra) => palabra.charAt(0).toLocaleUpperCase("es") + palabra.slice(1))
    .join(" ");
}
</script>

<template>
  <!-- Panel y navegación según el rol del usuario -->
  <main v-if="usuario" class="panel">
    <header class="panel-header">
      <div class="inicio-logo panel-logo">
        <span>RescueSync</span>
      </div>
      <nav
        v-if="usuario.rol !== 'AUDITOR'"
        class="panel-nav"
        aria-label="Navegación principal"
      >
        <template v-if="usuario.rol === 'OPERADOR_MUNICIPAL'">
          <button
            type="button"
            class="panel-nav-enlace"
            :aria-current="pantallaMunicipio === 'registrar' ? 'page' : undefined"
            @click="cambiarPantallaMunicipio('registrar')"
          >
            Registrar Emergencia
          </button>
          <button
            type="button"
            class="panel-nav-enlace"
            :aria-current="pantallaMunicipio === 'misEmergencias' ? 'page' : undefined"
            @click="cambiarPantallaMunicipio('misEmergencias')"
          >
            Mis Emergencias
          </button>
        </template>
        <button
          v-else-if="usuario.rol === 'CCR'"
          type="button"
          class="panel-nav-enlace"
          :aria-current="!tareaSeleccionada ? 'page' : undefined"
          @click="tareaSeleccionada = null"
        >
          Mis Tareas
        </button>
        <button v-else type="button" class="panel-nav-enlace" aria-current="page">Lotes</button>
      </nav>
      <div class="panel-header-usuario">
        <span class="panel-header-nombre">{{ usuario.nombre }}</span>
        <span v-if="usuario.organizacionNombre" class="panel-header-organizacion">
          {{ usuario.organizacionNombre }}
        </span>
        <span class="panel-header-rol">{{ mostrarRol(usuario.rol) }}</span>
      </div>
      <button type="button" @click="pedirConfirmacionSalir">Cerrar sesión</button>
    </header>

    <!-- Municipio: alta y consulta de sus emergencias -->
    <template v-if="usuario.rol === 'OPERADOR_MUNICIPAL'">
      <EmergenciaForm v-show="pantallaMunicipio === 'registrar'" @registrada="alRegistrarEmergencia" />
      <MisEmergencias v-if="pantallaMunicipio === 'misEmergencias'" :mensaje="mensajeMunicipio" />
    </template>

    <!-- Coordinador: su bandeja de tareas (lo que Bonita dice que le toca) -->
    <template v-else-if="usuario.rol === 'CCR'">
      <BandejaTareas v-if="!tareaSeleccionada" :mensaje="mensajeCoordinador" @seleccionar="seleccionarTarea" />

      <!-- Cada tarea abre su propio formulario según su nombre -->
      <GenerarLotesForm
        v-else-if="tareaSeleccionada.nombreTarea === 'Generar lotes'"
        :tarea="tareaSeleccionada"
        @volver="tareaSeleccionada = null"
        @publicado="alPublicarLotes"
      />

      <!-- Tareas que todavía no tienen pantalla (ej: Evaluar curso alternativo) -->
      <section v-else class="emergencia-card">
        <h1 class="emergencia-titulo">{{ tareaSeleccionada.nombreTarea }}</h1>
        <p>Esta tarea todavía no tiene pantalla.</p>
        <button type="button" class="bandeja-actualizar" @click="tareaSeleccionada = null">Volver a la bandeja</button>
      </section>
    </template>

    <LotesONG v-else-if="usuario.rol === 'REPRESENTANTE_ONG'" />
    <p v-else>Bienvenido/a. Tu pantalla todavía está en construcción.</p>
    <dialog
      ref="modalSalir"
      class="modal-salir"
      aria-labelledby="modal-salir-titulo"
      aria-describedby="modal-salir-descripcion"
      :aria-busy="cerrandoSesion"
      @cancel.prevent="cancelarSalida"
    >
      <h2 id="modal-salir-titulo">Cerrar sesión</h2>
      <p id="modal-salir-descripcion">¿Estás seguro de que querés cerrar sesión?</p>
      <p v-if="errorSalir" class="emergencia-error" role="alert">{{ errorSalir }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="cerrandoSesion" autofocus @click="cancelarSalida">
          Cancelar
        </button>
        <button type="button" class="btn-ingresar" :disabled="cerrandoSesion" @click="salir">
          {{ cerrandoSesion ? "Cerrando sesión..." : "Cerrar sesión" }}
        </button>
      </div>
    </dialog>
  </main>

  <!-- Sin sesión: acceso directo al login -->
  <Login v-else @logueado="alLoguearse" />
</template>
