<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { misTareas } from "../services/tareaService";
import { lotesDeEmergencia, type Emergencia, type Lote } from "../services/emergenciaService";
import {
  coberturaDe, misOfertas, ongsParaInvitar,
  type Cobertura, type Oferta, type Ong
} from "../services/ofertaService";
import { capitalizar, cierreDe, tiempoRestante } from "../utils/tiempo";
import OfertaLote from "./OfertaLote.vue";

type Convocatoria = { emergencia: Emergencia; lotes: Lote[]; cobertura: Cobertura | null; error: string };
const convocatorias = ref<Convocatoria[]>([]);
const ofertas = ref<Oferta[]>([]);      // ofertas donde participa (o está invitada) esta ONG
const ongs = ref<Ong[]>([]);            // para invitar a un consorcio (se carga una sola vez)
const cargando = ref(true);
const actualizando = ref(false);
const error = ref("");
const ahora = ref(Date.now());
let activo = true;
let reloj: ReturnType<typeof setInterval>;
let refresco: ReturnType<typeof setInterval>;

const vigentes = computed(() => convocatorias.value.filter(c => {
  const fin = cierreDe(c.emergencia);
  return !Number.isFinite(fin) || fin > ahora.value;
}));

// La oferta de esta ONG para un lote (una ONG participa en una sola oferta activa por lote)
function ofertaDe(lote: Lote) {
  return ofertas.value.find(o => o.loteId === lote.id);
}

function coberturaDeLote(convocatoria: Convocatoria, lote: Lote) {
  return convocatoria.cobertura?.lotes.find(l => l.loteId === lote.id);
}

async function cargar() {
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    const [tareas, mias] = await Promise.all([misTareas(), misOfertas()]);
    if (!ongs.value.length) ongs.value = await ongsParaInvitar();
    // Una convocatoria por emergencia con la tarea "Cargar o editar ofertas" disponible
    const emergencias = [...new Map(tareas
      .filter(t => t.nombreTarea === "Cargar o editar ofertas")
      .map(t => [t.emergencia.id, t.emergencia])).values()];
    const datos = await Promise.all(emergencias.map(async (emergencia): Promise<Convocatoria> => {
      if (cierreDe(emergencia) <= Date.now()) return { emergencia, lotes: [], cobertura: null, error: "" };
      try {
        const [lotes, cobertura] = await Promise.all([lotesDeEmergencia(emergencia.id), coberturaDe(emergencia.id)]);
        return { emergencia, lotes, cobertura, error: "" };
      } catch (e) {
        return { emergencia, lotes: [], cobertura: null, error: e instanceof Error ? e.message : "No se pudieron cargar los lotes." };
      }
    }));
    if (!activo) return;
    convocatorias.value = datos;
    ofertas.value = mias;
    error.value = "";
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron actualizar las convocatorias.";
  } finally {
    if (activo) {
      ahora.value = Date.now();
      cargando.value = false;
      actualizando.value = false;
    }
  }
}

function alVolver() {
  if (document.visibilityState === "visible") {
    ahora.value = Date.now();
    void cargar();
  }
}

onMounted(() => {
  void cargar();
  reloj = setInterval(() => { ahora.value = Date.now(); }, 1000);
  refresco = setInterval(() => { if (!document.hidden) void cargar(); }, 15000);
  document.addEventListener("visibilitychange", alVolver);
});

onUnmounted(() => {
  activo = false;
  clearInterval(reloj);
  clearInterval(refresco);
  document.removeEventListener("visibilitychange", alVolver);
});
</script>

<template>
  <section class="bandeja">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">CONVOCATORIAS</span>
        <h1 class="emergencia-titulo">Lotes disponibles</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="actualizando" @click="cargar">Actualizar</button>
    </div>
    <p class="ong-ayuda">
      Podés ofrecer todo o una parte de cada lote, sola o en consorcio con otras ONGs.
      Hasta que cierre la ventana podés modificar tu aporte; al cierre se decide quién entrega.
    </p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-if="cargando" role="status">Cargando lotes...</p>
    <p v-else-if="!error && vigentes.length === 0" class="bandeja-vacia">No hay convocatorias vigentes disponibles.</p>
    <ul v-else class="bandeja-lista">
      <li v-for="convocatoria in vigentes" :key="convocatoria.emergencia.id" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ capitalizar(convocatoria.emergencia.tipoDesastre) }}</span>
          <span aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ convocatoria.emergencia.zonaAfectada }}</span>
        </div>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ capitalizar(convocatoria.emergencia.gravedad) }}</span>
            <span>Emergencia #{{ convocatoria.emergencia.id }}</span>
          </span>
          <span class="ong-tiempo">Tiempo restante: {{ tiempoRestante(convocatoria.emergencia, ahora) }}</span>
        </div>
        <p v-if="convocatoria.error" class="emergencia-error ong-error" role="alert">{{ convocatoria.error }}</p>
        <p v-else-if="!convocatoria.lotes.length" class="ong-lote">No hay lotes publicados para esta convocatoria.</p>
        <ul v-else class="ong-lotes">
          <OfertaLote
            v-for="lote in convocatoria.lotes"
            :key="lote.id"
            :lote="lote"
            :cobertura="coberturaDeLote(convocatoria, lote)"
            :oferta="ofertaDe(lote)"
            :ongs="ongs"
            @cambio="cargar"
          />
        </ul>
      </li>
    </ul>
  </section>
</template>
