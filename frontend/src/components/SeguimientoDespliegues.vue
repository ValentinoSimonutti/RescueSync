<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { despliegues, type Despliegue } from "../services/ejecucionService";
import { capitalizar } from "../utils/tiempo";
import DespliegueDetalle from "./DespliegueDetalle.vue";

// Seguimiento del coordinador: emergencias adjudicadas, en ejecución y cerradas
const lista = ref<Despliegue[]>([]);
const cargando = ref(true);
const actualizando = ref(false);
const error = ref("");
const ampliadas = ref(new Set<number>());
let activo = true;
let refresco: ReturnType<typeof setInterval>;

const etiquetaEstado: Record<string, string> = {
  ADJUDICADA: "Esperando inicio del despliegue",
  EN_EJECUCION: "En ejecución",
  CERRADA: "Cerrada: recursos liberados"
};
const enCurso = computed(() => lista.value.filter(d => d.emergencia.estado !== "CERRADA"));
const cerradas = computed(() => lista.value.filter(d => d.emergencia.estado === "CERRADA"));

function alternar(id: number) {
  if (ampliadas.value.has(id)) ampliadas.value.delete(id);
  else ampliadas.value.add(id);
}

async function cargar() {
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    const datos = await despliegues();
    if (!activo) return;
    lista.value = datos;
    error.value = "";
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron cargar los despliegues";
  } finally {
    if (activo) {
      cargando.value = false;
      actualizando.value = false;
    }
  }
}

onMounted(() => {
  void cargar();
  refresco = setInterval(() => { if (!document.hidden) void cargar(); }, 15000);
});
onUnmounted(() => {
  activo = false;
  clearInterval(refresco);
});
</script>

<template>
  <section class="bandeja" :aria-busy="cargando">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">SEGUIMIENTO</span>
        <h1 class="emergencia-titulo">Despliegues</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="actualizando" @click="cargar">Actualizar</button>
    </div>
    <p class="ong-ayuda">
      Emergencias adjudicadas: qué recursos puso cada ONG y quiénes ya terminaron.
      Cuando todas las ONGs finalizan, el proyecto se cierra en el Sistema Nacional y se liberan los recursos.
    </p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-if="cargando" role="status">Cargando despliegues...</p>
    <p v-else-if="!error && lista.length === 0" class="bandeja-vacia">Todavía no hay emergencias adjudicadas.</p>

    <template v-for="grupo in [{ titulo: 'En curso', items: enCurso }, { titulo: 'Cerradas', items: cerradas }]" :key="grupo.titulo">
      <h2 v-if="grupo.items.length" class="despliegue-grupo">{{ grupo.titulo }}</h2>
      <ul v-if="grupo.items.length" class="bandeja-lista">
        <li v-for="d in grupo.items" :key="d.emergencia.id" class="bandeja-item">
          <div class="bandeja-item-superior">
            <span class="bandeja-tipo">{{ capitalizar(d.emergencia.tipoDesastre) }}</span>
            <span aria-hidden="true">·</span>
            <span class="bandeja-zona">{{ d.emergencia.zonaAfectada }}</span>
          </div>
          <div class="bandeja-item-inferior">
            <span class="bandeja-detalle">
              <span class="bandeja-gravedad">Gravedad {{ capitalizar(d.emergencia.gravedad) }}</span>
              <span>Emergencia #{{ d.emergencia.id }}</span>
            </span>
            <span class="mis-emergencias-estado">{{ etiquetaEstado[d.emergencia.estado] ?? d.emergencia.estado }}</span>
          </div>
          <div class="detalle-resumen">
            <p>{{ d.ongsFinalizadas }} de {{ d.ongsTotal }} {{ d.ongsTotal === 1 ? "ONG finalizó" : "ONGs finalizaron" }} sus actividades</p>
            <button type="button" class="detalle-flecha" :aria-expanded="ampliadas.has(d.emergencia.id)"
              :aria-label="ampliadas.has(d.emergencia.id) ? 'Ocultar detalle del despliegue' : 'Mostrar detalle del despliegue'"
              :aria-controls="`despliegue-${d.emergencia.id}`" @click="alternar(d.emergencia.id)">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
            </button>
          </div>
          <div v-show="ampliadas.has(d.emergencia.id)" :id="`despliegue-${d.emergencia.id}`" class="despliegue-detalle">
            <DespliegueDetalle :despliegue="d" />
          </div>
        </li>
      </ul>
    </template>
  </section>
</template>
