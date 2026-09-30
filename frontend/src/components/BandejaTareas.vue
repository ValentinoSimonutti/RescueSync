<script setup lang="ts">
import { onMounted, ref } from "vue";
import { misTareas, type Tarea } from "../services/tareaService";

// Avisa a App.vue qué tarea eligió el usuario (para abrir su formulario)
defineEmits<{
  seleccionar: [tarea: Tarea];
}>();

const tareas = ref<Tarea[]>([]);
const cargando = ref(true);
const error = ref("");

// Pide la bandeja a Spring (que a su vez le pregunta a Bonita y a Postgres)
async function cargar() {
  cargando.value = true;
  error.value = "";
  try {
    tareas.value = await misTareas();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudieron cargar las tareas";
  } finally {
    cargando.value = false;
  }
}

// Se ejecuta apenas aparece la pantalla
onMounted(cargar);
</script>

<template>
  <section class="bandeja">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">BANDEJA</span>
        <h1 class="emergencia-titulo">Mis tareas pendientes</h1>
      </div>
      <button type="button" class="bandeja-actualizar" @click="cargar" :disabled="cargando">
        Actualizar
      </button>
    </div>

    <p v-if="cargando">Cargando tareas...</p>
    <p v-else-if="error" class="emergencia-error">{{ error }}</p>
    <p v-else-if="tareas.length === 0" class="bandeja-vacia">No tenés tareas pendientes.</p>

    <!-- Una tarjeta por tarea -->
    <ul v-else class="bandeja-lista">
      <li v-for="tarea in tareas" :key="tarea.tareaId" class="bandeja-item">
        <div class="bandeja-info">
          <strong>{{ tarea.nombreTarea }}</strong>
          <span>{{ tarea.emergencia.tipoDesastre }} · {{ tarea.emergencia.zonaAfectada }}</span>
          <span class="bandeja-detalle">
            Gravedad {{ tarea.emergencia.gravedad }} · Emergencia #{{ tarea.emergencia.id }}
          </span>
        </div>
        <button type="button" class="btn-ingresar bandeja-boton" @click="$emit('seleccionar', tarea)">
          Resolver
        </button>
      </li>
    </ul>
  </section>
</template>
