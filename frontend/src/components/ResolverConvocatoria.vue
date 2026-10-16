<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import type { Tarea } from "../services/tareaService";
import { coberturaDe, type Cobertura } from "../services/ofertaService";
import { capitalizar, cierreDe } from "../utils/tiempo";
import GenerarLotesForm from "./GenerarLotesForm.vue";

const props = defineProps<{ tarea: Tarea }>();
defineEmits<{ volver: [] }>();
const cobertura = ref<Cobertura | null>(null);
const cargando = ref(false);
const error = ref("");
const reabriendo = ref(false);
let activo = true;
const lotesIniciales = computed(() => cobertura.value?.lotes.map(l => ({
  recursoCodigo: l.recursoCodigo ?? "", cantidad: l.requerido
})) ?? []);
const hayAportes = computed(() => cobertura.value?.lotes.some(l => l.ofrecido > 0) ?? false);
const pendientes = computed(() => cobertura.value?.lotes.filter(l => !l.cubierto).length ?? 0);
const cierre = computed(() => {
  const fecha = cierreDe(cobertura.value?.emergencia ?? props.tarea.emergencia);
  return Number.isFinite(fecha) ? new Date(fecha).toLocaleString("es-AR") : "No disponible";
});

async function cargar() {
  if (cargando.value) return;
  cargando.value = true;
  error.value = "";
  try {
    const resultado = await coberturaDe(props.tarea.emergencia.id);
    if (activo) cobertura.value = resultado;
  } catch (e) {
    if (activo) {
      cobertura.value = null;
      error.value = e instanceof Error ? e.message : "No se pudo consultar la cobertura.";
    }
  } finally {
    if (activo) cargando.value = false;
  }
}
onMounted(cargar);
onUnmounted(() => { activo = false; });
</script>

<template>
  <GenerarLotesForm v-if="reabriendo" :tarea="tarea" :lotes-iniciales="lotesIniciales"
    reapertura @volver="reabriendo = false" />
  <section v-else class="emergencia-card resolver-convocatoria">
    <div class="resolver-encabezado">
      <span class="inicio-etiqueta">CONVOCATORIA VENCIDA</span>
      <button type="button" class="bandeja-actualizar resolver-volver"
        aria-label="Volver a la bandeja" title="Volver a la bandeja" @click="$emit('volver')">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
      </button>
    </div>
    <h1 class="emergencia-titulo">{{ capitalizar(tarea.emergencia.tipoDesastre) }} · {{ tarea.emergencia.zonaAfectada }}</h1>
    <p class="lote-resumen">Emergencia #{{ tarea.emergencia.id }} · Gravedad {{ capitalizar(tarea.emergencia.gravedad) }}</p>
    <p>{{ tarea.emergencia.descripcion }}</p>
    <p class="ong-ayuda">Fin del plazo de ofertas: {{ cierre }}</p>
    <p v-if="cargando" role="status">Consultando necesidades y ofertas...</p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>

    <template v-if="cobertura">
      <p v-if="!cobertura.lotes.length || cobertura.lotesCubiertos || hayAportes" class="resolver-diagnostico" role="status">
        <template v-if="!cobertura.lotes.length">No hay datos de los lotes para explicar el resultado de esta convocatoria.</template>
        <template v-else-if="cobertura.lotesCubiertos">La cobertura consultada ahora está completa, pero la tarea de resolución sigue pendiente. Actualizá la bandeja para comprobar su estado.</template>
        <template v-else-if="hayAportes">El plazo finalizó con cobertura insuficiente. Quedan {{ pendientes }} de {{ cobertura.lotes.length }} lotes sin cubrir por completo.</template>
      </p>
      <ul class="resolver-lotes">
        <li v-for="lote in cobertura.lotes" :key="lote.loteId">
          <h2>{{ lote.recurso }}</h2>
          <dl class="resolver-cantidades">
            <div><dt>Necesario</dt><dd>{{ lote.requerido }}</dd></div>
            <div><dt>Ofrecido</dt><dd>{{ lote.ofrecido }}</dd></div>
            <div><dt>Faltante</dt><dd>{{ lote.faltante }}</dd></div>
          </dl>
          <p v-if="!lote.ofertas.length" class="ong-ayuda">No hay ofertas disponibles para este lote.</p>
          <ul v-else class="ong-aportes">
            <li v-for="oferta in lote.ofertas" :key="oferta.id">
              {{ oferta.liderNombre }} · {{ oferta.total }} · {{ capitalizar(oferta.estado) }}
              <ul>
                <li v-for="aporte in oferta.aportes" :key="aporte.ongId">
                  {{ aporte.ongNombre }}: {{ aporte.cantidad }} · {{ capitalizar(aporte.estado) }}
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </template>

    <div class="resolver-acciones">
      <button type="button" class="btn-ingresar" :disabled="cargando || !lotesIniciales.length || cobertura?.lotesCubiertos"
        @click="reabriendo = true">Reabrir convocatoria</button>
      <button type="button" class="bandeja-actualizar" disabled>Aceptar oferta parcial</button>
    </div>
  </section>
</template>
