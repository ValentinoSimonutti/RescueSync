<script setup lang="ts">
defineEmits<{
  ingresar: [];
}>();
</script>

<template>
  <main class="pantalla-inicio">
    <section class="inicio-card">

      <div class="inicio-logo">
        <h1 class="inicio-titulo">RescueSync</h1>
      </div>

      <span class="inicio-etiqueta">RESPUESTA COORDINADA</span>

      <h2 class="inicio-subtitulo">
        Coordinación regional de la respuesta ante desastres
      </h2>

      <p class="inicio-descripcion">
        Plataforma para coordinar recursos y organizaciones durante
        situaciones de emergencia.
      </p>

      <button class="btn-ingresar" @click="$emit('ingresar')">
        Ingresar <span aria-hidden="true">→</span>
      </button>

    </section>
  </main>
</template>
