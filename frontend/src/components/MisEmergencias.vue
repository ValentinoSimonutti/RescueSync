<script setup lang="ts">
import { onMounted, ref } from "vue";
import FiltrosEmergencias from "./FiltrosEmergencias.vue";
import { useFiltrosEmergencias } from "../composables/useFiltrosEmergencias";
import { misEmergencias, type Emergencia } from "../services/emergenciaService";

defineProps<{ mensaje?: string }>();

const emergencias = ref<Emergencia[]>([]);
const { zona, gravedad, tipo, orden, resultados, limpiar } = useFiltrosEmergencias(emergencias, emergencia => emergencia);
const cargando = ref(true);
const error = ref("");

function mostrarTexto(valor: string) {
  return valor
    .replace(/_/g, " ")
    .toLocaleLowerCase("es")
    .replace(/^./u, (letra) => letra.toLocaleUpperCase("es"));
}

async function cargar() {
  cargando.value = true;
  error.value = "";
  try {
    emergencias.value = await misEmergencias();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudieron cargar las emergencias";
  } finally {
    cargando.value = false;
  }
}

onMounted(cargar);
</script>

<template>
  <section class="bandeja" :aria-busy="cargando">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">MUNICIPIO</span>
        <h1 class="emergencia-titulo">Mis Emergencias</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="cargando" @click="cargar">
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

    <p v-if="cargando" role="status">Cargando emergencias...</p>
    <p v-else-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-else-if="emergencias.length === 0" class="bandeja-vacia">
      Tu municipio todavía no registró emergencias.
    </p>

    <p v-else-if="resultados.length === 0" class="bandeja-vacia" role="status">
      No hay resultados que coincidan con los filtros.
    </p>
    <ul v-else class="bandeja-lista">
      <li v-for="emergencia in resultados" :key="emergencia.id" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ mostrarTexto(emergencia.tipoDesastre) }}</span>
          <span class="bandeja-separador" aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ emergencia.zonaAfectada }}</span>
        </div>
        <p v-if="emergencia.descripcion" class="mis-emergencias-descripcion">
          {{ emergencia.descripcion }}
        </p>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ mostrarTexto(emergencia.gravedad) }}</span>
            <span>· Emergencia #{{ emergencia.id }}</span>
          </span>
          <span class="mis-emergencias-estado">Estado: {{ mostrarTexto(emergencia.estado) }}</span>
        </div>
      </li>
    </ul>
  </section>
</template>
