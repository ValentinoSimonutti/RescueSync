<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import type { Tarea } from "../services/tareaService";
import { coberturaDe, type Cobertura, type RolValidacion } from "../services/ofertaService";
import { continuarConCoberturaParcial } from "../services/emergenciaService";
import { capitalizar, cierreDe } from "../utils/tiempo";
import GenerarLotesForm from "./GenerarLotesForm.vue";

const props = defineProps<{ tarea: Tarea }>();
// resuelto: el coordinador decidió (reabrir o cobertura parcial) -> App.vue vuelve a la bandeja con el mensaje
const emit = defineEmits<{ volver: []; resuelto: [mensaje: string] }>();
const cobertura = ref<Cobertura | null>(null);
const cargando = ref(false);
const error = ref("");
const reabriendo = ref(false);
const enviandoParcial = ref(false);
const errorParcial = ref("");
const dialogoParcial = ref<HTMLDialogElement | null>(null);
// Cobertura parcial: los lotes con una oferta validada se adjudican; el resto queda sin cubrir
const lotesConOferta = computed(() => cobertura.value?.lotes.filter(l => l.habilitado) ?? []);
const lotesSinCubrir = computed(() => cobertura.value?.lotes.filter(l => !l.habilitado) ?? []);

async function confirmarParcial() {
  if (enviandoParcial.value) return;
  enviandoParcial.value = true;
  errorParcial.value = "";
  try {
    await continuarConCoberturaParcial(props.tarea.emergencia.id);
    dialogoParcial.value?.close();
    emit("resuelto", "Se continúa con cobertura parcial: el municipio ya puede adjudicar los lotes cubiertos.");
  } catch (e) {
    errorParcial.value = e instanceof Error ? e.message : "No se pudo continuar con cobertura parcial";
  } finally {
    enviandoParcial.value = false;
  }
}
let activo = true;
const lotesIniciales = computed(() => cobertura.value?.lotes.map(l => ({
  recursoCodigo: l.recursoCodigo ?? "", cantidad: l.requerido
})) ?? []);
const hayAportes = computed(() => cobertura.value?.lotes.some(l => l.ofrecido > 0) ?? false);
// Si el Sistema Nacional ya validó, lo que cuenta es lo validado (no lo ofrecido)
const validada = computed(() => cobertura.value?.lotes.some(l => l.validado !== null) ?? false);
const pendientes = computed(() => cobertura.value?.lotes.filter(l => validada.value ? !l.habilitado : !l.cubierto).length ?? 0);
const etiquetaRol: Record<RolValidacion, string> = {
  PRINCIPAL: "Principal",
  APOYO_SECUNDARIO: "Apoyo secundario",
  SIN_HABILITACION_VIGENTE: "Sin habilitación vigente"
};
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
    reapertura @volver="reabriendo = false"
    @publicado="emit('resuelto', 'Convocatoria reabierta. Las ONGs ya pueden volver a ofertar.')" />
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
        <template v-else-if="validada">El Sistema Nacional no habilitó ofertas suficientes: {{ pendientes }} de {{ cobertura.lotes.length }} lotes no tienen una oferta validada que los cubra. Abajo está el motivo de cada rechazo.</template>
        <template v-else-if="cobertura.lotesCubiertos">La cobertura consultada ahora está completa, pero la tarea de resolución sigue pendiente. Actualizá la bandeja para comprobar su estado.</template>
        <template v-else-if="hayAportes">El plazo finalizó con cobertura insuficiente. Quedan {{ pendientes }} de {{ cobertura.lotes.length }} lotes sin cubrir por completo.</template>
      </p>
      <ul class="resolver-lotes">
        <li v-for="lote in cobertura.lotes" :key="lote.loteId">
          <h2>{{ lote.recurso }}</h2>
          <dl class="resolver-cantidades">
            <div><dt>Necesario</dt><dd>{{ lote.requerido }}</dd></div>
            <div><dt>Mejor oferta</dt><dd>{{ lote.ofrecido }}</dd></div>
            <div><dt>Faltante</dt><dd>{{ lote.faltante }}</dd></div>
            <div v-if="validada"><dt>Validado por el SN</dt><dd :class="lote.habilitado ? 'despliegue-fin' : 'tarea-faltante'">{{ lote.validado ?? 0 }}</dd></div>
          </dl>
          <p v-if="!lote.ofertas.length" class="ong-ayuda">No hay ofertas disponibles para este lote.</p>
          <ul v-else class="ong-aportes">
            <li v-for="oferta in lote.ofertas" :key="oferta.id">
              {{ oferta.liderNombre }} · {{ oferta.total }} · {{ capitalizar(oferta.estado) }}
              <template v-if="oferta.rolValidacion">
                · <span class="adjudicar-rol" :class="{ apoyo: oferta.rolValidacion !== 'PRINCIPAL' }">{{ etiquetaRol[oferta.rolValidacion] }}</span>
                validado {{ oferta.cantidadValidada ?? 0 }} de {{ lote.requerido }}
              </template>
              <ul v-if="oferta.observacionesValidacion.length" class="adjudicar-observaciones">
                <li v-for="(observacion, i) in oferta.observacionesValidacion" :key="i">{{ observacion }}</li>
              </ul>
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
      <button type="button" class="btn-ingresar" :disabled="cargando || !lotesIniciales.length || (validada ? pendientes === 0 : cobertura?.lotesCubiertos)"
        @click="reabriendo = true">Reabrir convocatoria</button>
      <button type="button" class="bandeja-actualizar" :disabled="cargando || !lotesConOferta.length"
        :title="lotesConOferta.length ? undefined : 'Ningún lote tiene una oferta validada por el Sistema Nacional'"
        @click="errorParcial = ''; dialogoParcial?.showModal()">Continuar con cobertura parcial</button>
    </div>

    <dialog ref="dialogoParcial" class="modal-salir modal-emergencia" aria-labelledby="parcial-titulo"
      :aria-busy="enviandoParcial" @cancel.prevent="!enviandoParcial && dialogoParcial?.close()">
      <h2 id="parcial-titulo">Continuar con cobertura parcial</h2>
      <p>El municipio va a adjudicar los lotes que tienen una oferta validada. Los demás quedan sin cubrir.</p>
      <dl class="confirmacion-resumen">
        <div v-for="lote in lotesConOferta" :key="lote.loteId">
          <dt>{{ lote.recurso }} ({{ lote.requerido }})</dt><dd>Se adjudica</dd>
        </div>
        <div v-for="lote in lotesSinCubrir" :key="lote.loteId">
          <dt>{{ lote.recurso }} ({{ lote.requerido }})</dt><dd class="tarea-faltante">Queda sin cubrir</dd>
        </div>
      </dl>
      <p v-if="errorParcial" class="emergencia-error" role="alert">{{ errorParcial }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="enviandoParcial" autofocus @click="dialogoParcial?.close()">Cancelar</button>
        <button type="button" class="btn-ingresar" :disabled="enviandoParcial" @click="confirmarParcial">
          {{ enviandoParcial ? "Enviando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
