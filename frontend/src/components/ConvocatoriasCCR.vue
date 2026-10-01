<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import type { Emergencia } from "../services/emergenciaService";
import { coberturaDe, convocatoriasAbiertas, type Cobertura } from "../services/ofertaService";
import { capitalizar, cierreDe, tiempoRestante } from "../utils/tiempo";
import HistorialOferta from "./HistorialOferta.vue";

const props = defineProps<{ vista: "abiertas" | "cubiertas" }>();
type Fila = { emergencia: Emergencia; cobertura: Cobertura | null; error: string };
const filas = ref<Fila[]>([]);
const visibles = computed(() => filas.value.filter(f => props.vista === "abiertas"
  ? f.emergencia.estado === "EN_CONVOCATORIA" && !ventanaCerrada(f.emergencia)
  : ventanaCerrada(f.emergencia) && f.cobertura?.lotesCubiertos)
  .sort((a, b) => props.vista === "abiertas"
    ? (cierreDe(a.emergencia) || 0) - (cierreDe(b.emergencia) || 0)
    : (cierreDe(b.emergencia) || 0) - (cierreDe(a.emergencia) || 0)));
const erroresCobertura = computed(() => filas.value.filter(f => f.error));
const cargando = ref(true);
const actualizando = ref(false);
const error = ref("");
const ahora = ref(Date.now());
const ampliadas = ref(new Set<number>());
function alternarDetalle(id: number) {
  if (ampliadas.value.has(id)) ampliadas.value.delete(id);
  else ampliadas.value.add(id);
}
const historialAbierto = ref<number | null>(null);
let activo = true;
let reloj: ReturnType<typeof setInterval>;
let refresco: ReturnType<typeof setInterval>;

function ventanaCerrada(emergencia: Emergencia) {
  const fin = cierreDe(emergencia);
  return ["EN_EVALUACION", "ADJUDICADA", "EN_EJECUCION", "CERRADA"].includes(emergencia.estado)
    || (emergencia.estado === "EN_CONVOCATORIA" && Number.isFinite(fin) && fin <= ahora.value);
}

function fechaCierre(emergencia: Emergencia) {
  const fin = cierreDe(emergencia);
  return Number.isFinite(fin) ? new Date(fin).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" }) : "No disponible";
}

function lotesCubiertos(cobertura: Cobertura) {
  return cobertura.lotes.filter(l => l.cubierto).length;
}

function alternarHistorial(ofertaId: number) {
  historialAbierto.value = historialAbierto.value === ofertaId ? null : ofertaId;
}

async function cargar() {
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    ahora.value = Date.now();
    const emergencias = await convocatoriasAbiertas();
    const datos = await Promise.all(emergencias.map(async (emergencia): Promise<Fila> => {
      try {
        return { emergencia, cobertura: await coberturaDe(emergencia.id), error: "" };
      } catch (e) {
        return { emergencia, cobertura: null, error: e instanceof Error ? e.message : "No se pudo cargar la cobertura." };
      }
    }));
    if (!activo) return;
    filas.value = datos;
    error.value = "";
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron cargar las convocatorias.";
  } finally {
    if (activo) {
      ahora.value = Date.now();
      cargando.value = false;
      actualizando.value = false;
    }
  }
}

function alVolver() {
  if (document.visibilityState === "visible") void cargar();
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
        <span class="inicio-etiqueta">SEGUIMIENTO</span>
        <h1 class="emergencia-titulo">{{ vista === "abiertas" ? "Convocatorias abiertas" : "Convocatorias cubiertas" }}</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="actualizando" @click="cargar">Actualizar</button>
    </div>
    <p class="ong-ayuda">
      {{ vista === "abiertas"
        ? "Convocatorias en recepción de ofertas. Consultá el tiempo restante y la cobertura de cada lote."
        : "Convocatorias finalizadas con todos sus lotes cubiertos por ofertas." }}
      La cobertura no implica adjudicación ni entrega de recursos.
    </p>
    <p v-if="vista === 'cubiertas' && erroresCobertura.length" class="emergencia-error" role="alert">
      No se pudo comprobar la cobertura de {{ erroresCobertura.length }} convocatoria(s). Actualizá para reintentar.
    </p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-if="cargando" role="status">Cargando convocatorias...</p>
    <p v-else-if="!error && visibles.length === 0" class="bandeja-vacia">No hay convocatorias en esta sección.</p>
    <ul v-else class="bandeja-lista">
      <li v-for="fila in visibles" :key="fila.emergencia.id" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ capitalizar(fila.emergencia.tipoDesastre) }}</span>
          <span aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ fila.emergencia.zonaAfectada }}</span>
        </div>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ capitalizar(fila.emergencia.gravedad) }}</span>
            <span>Emergencia #{{ fila.emergencia.id }}</span>
          </span>
          <span v-if="vista === 'abiertas'" class="ong-tiempo">Cierra en {{ tiempoRestante(fila.emergencia, ahora) }}</span>
          <span v-else class="ong-tiempo ccr-cerrada">
            {{ fila.cobertura ? (fila.cobertura.lotesCubiertos ? "Necesidades cubiertas" : "Cobertura incompleta") : "Cobertura no disponible" }}
          </span>
        </div>

        <p class="ccr-resumen">Cierre de convocatoria: {{ fechaCierre(fila.emergencia) }} · Estado de la emergencia: {{ capitalizar(fila.emergencia.estado.replace(/_/g, ' ')) }}</p>
        <p v-if="fila.error" class="emergencia-error ong-error" role="alert">{{ fila.error }}</p>
        <template v-else-if="fila.cobertura">
          <div class="detalle-resumen">
            <p :class="{ 'ccr-resumen-ok': fila.cobertura.lotesCubiertos }">
              {{ lotesCubiertos(fila.cobertura) }} de {{ fila.cobertura.lotes.length }} lotes cubiertos ·
              {{ fila.cobertura.totalOfertas }} {{ fila.cobertura.totalOfertas === 1 ? "oferta" : "ofertas" }}
            </p>
            <button v-if="vista === 'cubiertas'" type="button" class="detalle-flecha"
              :aria-expanded="ampliadas.has(fila.emergencia.id)"
              :aria-label="ampliadas.has(fila.emergencia.id) ? 'Ocultar detalle de la convocatoria' : 'Mostrar detalle de la convocatoria'"
              :title="ampliadas.has(fila.emergencia.id) ? 'Ocultar detalle' : 'Mostrar detalle'"
              :aria-controls="`detalle-convocatoria-${fila.emergencia.id}`" @click="alternarDetalle(fila.emergencia.id)">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
            </button>
          </div>
          <ul v-show="vista === 'abiertas' || ampliadas.has(fila.emergencia.id)" :id="`detalle-convocatoria-${fila.emergencia.id}`" class="ong-lotes">
            <li v-for="lote in fila.cobertura.lotes" :key="lote.loteId" class="ong-lote ong-lote-oferta">
              <div class="ong-lote-cabecera">
                <div>
                  <strong>{{ lote.recurso }}</strong>
                  <p>
                    Ofrecido {{ lote.ofrecido }} de {{ lote.requerido }}
                    <template v-if="lote.faltante > 0"> · faltan {{ lote.faltante }}</template>
                    <template v-else> · cubierto</template>
                  </p>
                </div>
                <div class="cobertura-mini">
                  <div class="cobertura-barra" role="img" :aria-label="`Cubierto ${lote.porcentaje}%`">
                    <span :style="{ width: lote.porcentaje + '%' }" :class="{ completa: lote.cubierto }"></span>
                  </div>
                  <small>{{ lote.porcentaje }}%</small>
                </div>
              </div>
              <p v-if="!lote.ofertas.length" class="ccr-sin-ofertas">Todavía no hay ofertas para este lote.</p>
              <ul v-else class="ccr-ofertas">
                <li v-for="oferta in lote.ofertas" :key="oferta.id" class="ccr-oferta">
                  <div class="ccr-oferta-cabecera">
                    <span>
                      <strong>{{ oferta.liderNombre }}</strong>
                      <span v-if="oferta.consorcio" class="ccr-etiqueta">Consorcio</span>
                    </span>
                    <span>Total {{ oferta.total }} · v{{ oferta.version }}</span>
                  </div>
                  <ul class="ong-aportes">
                    <li v-for="a in oferta.aportes.filter(x => x.estado === 'CONFIRMADO' || x.estado === 'PENDIENTE')" :key="a.ongId">
                      {{ a.ongNombre }}: {{ a.estado === "PENDIENTE" ? "invitación pendiente" : a.cantidad }}
                    </li>
                  </ul>
                  <button type="button" class="enlace-boton" @click="alternarHistorial(oferta.id)">
                    {{ historialAbierto === oferta.id ? "Ocultar historial" : "Ver historial" }}
                  </button>
                  <HistorialOferta v-if="historialAbierto === oferta.id" :oferta-id="oferta.id" :key="oferta.version" />
                </li>
              </ul>
            </li>
          </ul>
        </template>
      </li>
    </ul>
  </section>
</template>
