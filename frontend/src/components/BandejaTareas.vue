<script setup lang="ts">
import { onMounted, ref } from "vue";
import FiltrosEmergencias from "./FiltrosEmergencias.vue";
import { useFiltrosEmergencias } from "../composables/useFiltrosEmergencias";
import { misTareas, type Tarea } from "../services/tareaService";

defineProps<{ mensaje?: string }>();

// Avisa a App.vue qué tarea eligió el usuario (para abrir su formulario)
defineEmits<{
  seleccionar: [tarea: Tarea];
}>();

const tareas = ref<Tarea[]>([]);
const { zona, gravedad, tipo, orden, resultados, limpiar } = useFiltrosEmergencias(tareas, tarea => tarea.emergencia);
const cargando = ref(true);
const error = ref("");

function mostrarGravedad(gravedad: string) {
  return gravedad
    .toLocaleLowerCase("es")
    .replace(/^./u, (letra) => letra.toLocaleUpperCase("es"));
}

function mostrarTipoDesastre(tipo: string) {
  return tipo
    .toLocaleLowerCase("es")
    .replace(/^./u, (letra) => letra.toLocaleUpperCase("es"));
}

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

    <p v-if="mensaje" class="emergencia-mensaje" role="status">{{ mensaje }}</p>

    <FiltrosEmergencias
      v-model:zona="zona"
      v-model:gravedad="gravedad"
      v-model:tipo="tipo"
      v-model:orden="orden"
      @limpiar="limpiar"
    />

    <p v-if="cargando">Cargando tareas...</p>
    <p v-else-if="error" class="emergencia-error">{{ error }}</p>
    <p v-else-if="tareas.length === 0" class="bandeja-vacia">No tenés tareas pendientes.</p>

    <!-- Una tarjeta por tarea -->
    <p v-else-if="resultados.length === 0" class="bandeja-vacia" role="status">
      No hay resultados que coincidan con los filtros.
    </p>
    <ul v-else class="bandeja-lista">
      <li v-for="tarea in resultados" :key="tarea.tareaId" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ mostrarTipoDesastre(tarea.emergencia.tipoDesastre) }}</span>
          <span class="bandeja-separador" aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ tarea.emergencia.zonaAfectada }}</span>
          <strong v-if="tarea.nombreTarea !== 'Generar lotes'">{{ tarea.nombreTarea }}</strong>
        </div>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ mostrarGravedad(tarea.emergencia.gravedad) }}</span>
            <span>· Emergencia #{{ tarea.emergencia.id }}</span>
          </span>
          <button type="button" class="btn-ingresar bandeja-boton" @click="$emit('seleccionar', tarea)">
            {{ tarea.nombreTarea === "Generar lotes" ? "Generar lote" : "Resolver" }}
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
