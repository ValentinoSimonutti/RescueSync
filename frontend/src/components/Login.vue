<script setup lang="ts">
defineEmits<{
  volver: [];
}>();
</script>

<template>
  <main class="pantalla-inicio">
    <section class="inicio-card login-card">
      <div class="inicio-logo">
        <h1 class="inicio-titulo">RescueSync</h1>
      </div>

      <span class="inicio-etiqueta">INGRESAR</span>

      <form class="login-form" @submit.prevent>
        <label class="login-campo">
          <span>Usuario</span>
          <input type="text" name="usuario" autocomplete="username" required />
        </label>

        <label class="login-campo">
          <span>Contraseña</span>
          <input type="password" name="contrasena" autocomplete="current-password" required />
        </label>

        <button class="btn-ingresar login-submit" type="submit">Ingresar</button>
      </form>

      <button class="login-volver" type="button" @click="$emit('volver')">
        Volver al inicio
      </button>
    </section>
  </main>
</template>
