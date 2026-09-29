<script setup lang="ts">
import { ref } from "vue";
import { login, type Usuario } from "../services/authService";

const emit = defineEmits<{
  volver: [];
  logueado: [usuario: Usuario];   // avisa a App.vue que el login salió bien
}>();

// v-model: lo que el usuario escribe en los inputs
const username = ref("");
const password = ref("");

const error = ref("");
const cargando = ref(false);

async function ingresar() {
  error.value = "";
  cargando.value = true;
  try {
    // Spring valida con Bonita, busca el usuario en Postgres y crea la sesión
    const usuario = await login(username.value, password.value);
    emit("logueado", usuario);
  } catch (e) {
    // El mensaje viene del backend (ej: "Usuario o contraseña inválidos en Bonita")
    error.value = e instanceof Error ? e.message : "No se pudo iniciar sesión";
  } finally {
    cargando.value = false;
  }
}
</script>

<template>
  <main class="pantalla-inicio">
    <section class="inicio-card login-card">
      <div class="inicio-logo">
        <h1 class="inicio-titulo">RescueSync</h1>
      </div>

      <span class="inicio-etiqueta">INGRESAR</span>

      <form class="login-form" @submit.prevent="ingresar">
        <label class="login-campo">
          <span>Usuario</span>
          <input v-model="username" type="text" name="usuario" autocomplete="username" required />
        </label>

        <label class="login-campo">
          <span>Contraseña</span>
          <input v-model="password" type="password" name="contrasena" autocomplete="current-password" required />
        </label>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="btn-ingresar login-submit" type="submit" :disabled="cargando">
          {{ cargando ? "Ingresando..." : "Ingresar" }}
        </button>
      </form>

      <button class="login-volver" type="button" @click="$emit('volver')">
        Volver al inicio
      </button>
    </section>
  </main>
</template>
