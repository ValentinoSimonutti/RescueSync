<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { misTareas } from "../services/tareaService";
import { lotesDeEmergencia, type Emergencia, type Lote } from "../services/emergenciaService";
import {
  coberturaDe, misOfertas, ongsParaInvitar,
  type Cobertura, type Oferta, type Ong
} from "../services/ofertaService";
import { finalizarActividades } from "../services/ejecucionService";
import { capitalizar, cierreDe, fechaCorta, tiempoRestante } from "../utils/tiempo";
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

// Avisos de adjudicación: ofertas donde participa esta ONG que el municipio ya resolvió
// (agrupadas por emergencia, la más nueva primero)
const resultados = computed(() => ofertas.value
  .filter(o => o.resultado && o.miAporte?.estado === "CONFIRMADO")
  .sort((a, b) => b.emergenciaId - a.emergenciaId || a.id - b.id));
// La finalización es por emergencia: el estado y el botón van en la última oferta adjudicada de cada una
const cierreEnOferta = computed(() => {
  const ultima = new Map<number, number>();
  for (const o of resultados.value) if (o.resultado === "ADJUDICADA") ultima.set(o.emergenciaId, o.id);
  return new Set(ultima.values());
});

const vigentes = computed(() => convocatorias.value.filter(c => {
  const fin = cierreDe(c.emergencia);
  return !Number.isFinite(fin) || fin > ahora.value;
}));

// Una oferta activa por ONG y lote; en otra cantidad/lote puede ofertar por separado.
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

// Ejecución: la ONG marca sus actividades de una emergencia como finalizadas
const finalizando = ref<Oferta | null>(null);
const enviandoFinalizacion = ref(false);
const errorFinalizacion = ref("");
const mensajeFinalizacion = ref("");
const dialogoFinalizar = ref<HTMLDialogElement | null>(null);

function pedirFinalizacion(oferta: Oferta) {
  finalizando.value = oferta;
  errorFinalizacion.value = "";
  dialogoFinalizar.value?.showModal();
}

async function confirmarFinalizacion() {
  if (!finalizando.value || enviandoFinalizacion.value) return;
  enviandoFinalizacion.value = true;
  errorFinalizacion.value = "";
  try {
    const despliegue = await finalizarActividades(finalizando.value.emergenciaId);
    dialogoFinalizar.value?.close();
    mensajeFinalizacion.value = despliegue.ongsFinalizadas === despliegue.ongsTotal
      ? "Actividades finalizadas. Eras la última ONG: se cierra el proyecto y se liberan los recursos."
      : "Actividades finalizadas. El Sistema Nacional liberó tus recursos de esta emergencia.";
    await cargar();
  } catch (e) {
    errorFinalizacion.value = e instanceof Error ? e.message : "No se pudo marcar la finalización";
  } finally {
    enviandoFinalizacion.value = false;
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
      Una oferta individual cubre el lote completo; para aportar una parte, armá un consorcio con otras ONGs.
      Varias ofertas pueden competir por el mismo lote: al cierre, el municipio elige una.
    </p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>

    <p v-if="mensajeFinalizacion" class="emergencia-mensaje" role="status">{{ mensajeFinalizacion }}</p>
    <section v-if="resultados.length" class="ong-resultados" aria-labelledby="ong-resultados-titulo">
      <h2 id="ong-resultados-titulo" class="ong-oferta-titulo">Resultados de adjudicación</h2>
      <ul class="ong-aportes">
        <li v-for="oferta in resultados" :key="oferta.id" :class="oferta.resultado === 'ADJUDICADA' ? 'ong-oferta-ok' : 'ong-ayuda'">
          Emergencia #{{ oferta.emergenciaId }} · {{ oferta.zonaAfectada }} · {{ oferta.recurso }}:
          <strong v-if="oferta.resultado === 'ADJUDICADA'">
            tu oferta fue seleccionada como responsable{{ oferta.consorcio ? " (en consorcio)" : "" }}<template v-if="oferta.miAporte?.rolValidacion === 'APOYO_SECUNDARIO'">, con tu ONG como apoyo secundario</template>.
          </strong>
          <template v-else>el municipio eligió otra oferta.</template>
          <div v-if="cierreEnOferta.has(oferta.id)" class="ong-finalizar">
            <span v-if="oferta.miAporte?.fechaFinalizacion">Marcaste tus actividades de esta emergencia como finalizadas el {{ fechaCorta(oferta.miAporte.fechaFinalizacion) }}.</span>
            <span v-if="oferta.estadoEmergencia === 'ADJUDICADA'" class="ong-ayuda">Esperando que el coordinador inicie el despliegue.</span>
            <span v-else-if="oferta.estadoEmergencia === 'CERRADA'">Proyecto cerrado: tus recursos quedaron liberados.</span>
            <button v-else-if="oferta.estadoEmergencia === 'EN_EJECUCION' && !oferta.miAporte?.fechaFinalizacion"
              type="button" class="btn-ingresar" @click="pedirFinalizacion(oferta)">Marcar actividades como finalizadas</button>
          </div>
        </li>
      </ul>
    </section>

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

    <dialog ref="dialogoFinalizar" class="modal-salir modal-emergencia" aria-labelledby="finalizar-titulo"
      :aria-busy="enviandoFinalizacion" @cancel.prevent="!enviandoFinalizacion && dialogoFinalizar?.close()">
      <h2 id="finalizar-titulo">Finalizar actividades</h2>
      <p v-if="finalizando">
        Emergencia #{{ finalizando.emergenciaId }} · {{ finalizando.zonaAfectada }}. Se le informa al Sistema Nacional,
        que libera los recursos que tu ONG tenía comprometidos en esta emergencia. ¿Confirmás?
      </p>
      <p v-if="errorFinalizacion" class="emergencia-error" role="alert">{{ errorFinalizacion }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="enviandoFinalizacion" autofocus @click="dialogoFinalizar?.close()">Cancelar</button>
        <button type="button" class="btn-ingresar" :disabled="enviandoFinalizacion" @click="confirmarFinalizacion">
          {{ enviandoFinalizacion ? "Informando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
