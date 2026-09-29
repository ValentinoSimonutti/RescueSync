<script setup lang="ts">
import { ref } from "vue";
import PantallaInicio from "./components/PantallaInicio.vue";
import Login from "./components/Login.vue";

const mostrarLogin = ref(false);
</script>

<template>
  <PantallaInicio v-if="!mostrarLogin" @ingresar="mostrarLogin = true" />
  <Login v-else @volver="mostrarLogin = false" />
</template>
