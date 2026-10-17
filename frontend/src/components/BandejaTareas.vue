<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { coberturaDe, type Cobertura } from "../services/ofertaService";
import FiltrosEmergencias from "./FiltrosEmergencias.vue";
import { useFiltrosEmergencias } from "../composables/useFiltrosEmergencias";
import { misTareas, type Tarea } from "../services/tareaService";

defineProps<{ mensaje?: string }>();

// Avisa a App.vue qué tarea eligió el usuario (para abrir su formulario)
defineEmits<{
  seleccionar: [tarea: Tarea];
}>();

const tareas = ref<Tarea[]>([]);
const ampliadas = ref(new Set<number>());
function alternarDetalle(id: number) {
  if (ampliadas.value.has(id)) ampliadas.value.delete(id);
  else ampliadas.value.add(id);
}
const coberturas = ref<Record<number, Cobertura | null>>({});
const { zona, gravedad, tipo, orden, resultados, limpiar } = useFiltrosEmergencias(tareas, tarea => tarea.emergencia);
const vista = ref<"activas" | "vencidas">("activas");
// Esta tarea representa el camino de cobertura insuficiente de Bonita.
function esVencida(tarea: Tarea) {
  return tarea.nombreTarea === "Evaluar curso de acción alternativo";
}
const activas = computed(() => resultados.value.filter(t => !esVencida(t)));
const vencidas = computed(() => resultados.value.filter(esVencida));
const visibles = computed(() => vista.value === "activas" ? activas.value : vencidas.value);
const cargando = ref(true);
const actualizando = ref(false);
let activo = true;
let refresco: ReturnType<typeof setInterval>;
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
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    const nuevas = await misTareas();
    const detalles = await Promise.all([...new Set(nuevas.filter(esVencida).map(t => t.emergencia.id))].map(async id => {
      try { return [id, await coberturaDe(id)] as const; }
      catch { return [id, null] as const; }
    }));
    if (activo) {
      tareas.value = nuevas;
      coberturas.value = Object.fromEntries(detalles);
      error.value = "";
    }
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron cargar las tareas";
  } finally {
    if (activo) { cargando.value = false; actualizando.value = false; }
  }
}

// Se ejecuta apenas aparece la pantalla
function alVolver() { if (!document.hidden) void cargar(); }
onMounted(() => {
  void cargar();
  refresco = setInterval(alVolver, 15000);
  document.addEventListener("visibilitychange", alVolver);
});
onUnmounted(() => {
  activo = false;
  clearInterval(refresco);
  document.removeEventListener("visibilitychange", alVolver);
});
</script>

<template>
  <section class="bandeja">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">BANDEJA</span>
        <h1 class="emergencia-titulo">Tareas pendientes</h1>
      </div>
      <button type="button" class="bandeja-actualizar" @click="cargar" :disabled="actualizando">
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

    <div class="ccr-vistas" role="group" aria-label="Tipo de tarea pendiente">
      <button type="button" :aria-pressed="vista === 'activas'" @click="vista = 'activas'">Activas ({{ activas.length }})</button>
      <button type="button" :aria-pressed="vista === 'vencidas'" @click="vista = 'vencidas'">Vencidas sin cobertura ({{ vencidas.length }})</button>
    </div>

    <p v-if="cargando">Cargando tareas...</p>
    <p v-else-if="error" class="emergencia-error">{{ error }}</p>
    <p v-else-if="tareas.length === 0" class="bandeja-vacia">No tenés tareas pendientes.</p>

    <!-- Una tarjeta por tarea -->
    <p v-else-if="resultados.length === 0" class="bandeja-vacia" role="status">
      No hay resultados que coincidan con los filtros.
    </p>
    <p v-else-if="visibles.length === 0" class="bandeja-vacia">No hay tareas pendientes en esta sección con los filtros actuales.</p>
    <ul v-else class="bandeja-lista">
      <li v-for="tarea in visibles" :key="tarea.tareaId" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ mostrarTipoDesastre(tarea.emergencia.tipoDesastre) }}</span>
          <span class="bandeja-separador" aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ tarea.emergencia.zonaAfectada }}</span>
          <strong v-if="esVencida(tarea)">El plazo para recibir ofertas terminó sin cubrir todas las necesidades.</strong>
          <strong v-else-if="tarea.nombreTarea !== 'Generar lotes'">{{ tarea.nombreTarea }}</strong>
        </div>
        <div v-if="esVencida(tarea)" class="detalle-resumen">
          <p v-if="coberturas[tarea.emergencia.id]?.lotes.length">
            {{ coberturas[tarea.emergencia.id]?.lotes.filter(l => !l.cubierto).length }} de
            {{ coberturas[tarea.emergencia.id]?.lotes.length }} lotes sin cubrir por completo
          </p>
          <p v-else>{{ coberturas[tarea.emergencia.id] ? "No hay lotes disponibles." : "Detalle de necesidades no disponible." }}</p>
          <button type="button" class="detalle-flecha" :aria-expanded="ampliadas.has(tarea.tareaId)"
            :aria-label="ampliadas.has(tarea.tareaId) ? 'Ocultar recursos solicitados y ofrecidos' : 'Mostrar recursos solicitados y ofrecidos'"
            :title="ampliadas.has(tarea.tareaId) ? 'Ocultar recursos' : 'Mostrar recursos'"
            :aria-controls="`recursos-tarea-${tarea.tareaId}`" @click="alternarDetalle(tarea.tareaId)">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
          </button>
        </div>
        <div v-if="esVencida(tarea)" v-show="ampliadas.has(tarea.tareaId)" :id="`recursos-tarea-${tarea.tareaId}`" class="tarea-cobertura">
          <table v-if="coberturas[tarea.emergencia.id]?.lotes.length">
            <caption class="tarea-cobertura-titulo">Resumen de necesidades</caption>
            <thead><tr><th scope="col">Recurso</th><th scope="col">Solicitado</th><th scope="col">Mejor oferta</th><th scope="col">Faltante</th></tr></thead>
            <tbody>
              <tr v-for="lote in coberturas[tarea.emergencia.id]?.lotes" :key="lote.loteId">
                <th scope="row">{{ lote.recurso }}</th>
                <td>{{ lote.requerido }}</td><td>{{ lote.ofrecido }}</td>
                <td :class="{ 'tarea-faltante': lote.faltante > 0 }">{{ lote.faltante }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="ong-ayuda">{{ coberturas[tarea.emergencia.id] ? "No hay lotes disponibles." : "No se pudo cargar el detalle de las necesidades. Entrá en Resolver para volver a consultarlo." }}</p>
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
