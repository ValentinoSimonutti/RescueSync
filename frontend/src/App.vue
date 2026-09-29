<script setup lang="ts">
import { onMounted, ref } from "vue";
import PantallaInicio from "./components/PantallaInicio.vue";
import Login from "./components/Login.vue";
import EmergenciaForm from "./components/EmergenciaForm.vue";
import { logout, usuarioActual, type Usuario } from "./services/authService";

const mostrarLogin = ref(false);
const usuario = ref<Usuario | null>(null);   // null = nadie logueado

// Al abrir la página preguntamos a Spring si ya hay una sesión (ej: después de un F5)
onMounted(async () => {
  try {
    usuario.value = await usuarioActual();
  } catch {
    usuario.value = null;   // 401 = no hay sesión, se queda en la pantalla de inicio
  }
});

function alLoguearse(u: Usuario) {
  usuario.value = u;
  mostrarLogin.value = false;
}

async function salir() {
  await logout();
  usuario.value = null;
}
</script>

<template>
  <!-- Logueado: panel simple. Después se reemplaza por un menú según el rol -->
  <main v-if="usuario" class="panel">
    <header class="panel-header">
      <span>{{ usuario.nombre }} · {{ usuario.rol }}
        <template v-if="usuario.organizacionNombre"> · {{ usuario.organizacionNombre }}</template>
      </span>
      <button type="button" @click="salir">Cerrar sesión</button>
    </header>

    <!-- Por ahora: el operador municipal ve el alta de emergencia -->
    <EmergenciaForm v-if="usuario.rol === 'OPERADOR_MUNICIPAL'" />
    <p v-else>Bienvenido/a. Tu pantalla todavía está en construcción.</p>
  </main>

  <!-- No logueado: inicio o login -->
  <PantallaInicio v-else-if="!mostrarLogin" @ingresar="mostrarLogin = true" />
  <Login v-else @volver="mostrarLogin = false" @logueado="alLoguearse" />
</template>
