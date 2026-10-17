<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import type { Emergencia } from "../services/emergenciaService";
import type { Oferta, RolValidacion } from "../services/ofertaService";
import { adjudicar, opcionesAdjudicacion, type LoteAdjudicable } from "../services/adjudicacionService";
import { capitalizar } from "../utils/tiempo";

// "Adjudicar y notificar": el municipio ve, por lote, solo las ofertas que validó el
// Sistema Nacional y que cubren el lote completo, y elige una por lote.
const props = defineProps<{ emergencia: Emergencia }>();

const emit = defineEmits<{
  volver: [];
  adjudicado: [];
}>();

const lotes = ref<LoteAdjudicable[]>([]);
const avisoCompromiso = ref<string | null>(null);   // si el SN rechazó un compromiso anterior
const coberturaParcial = ref(false);                // el coordinador decidió seguir sin algunos lotes
const elegidas = ref<Record<number, number>>({});   // loteId -> ofertaId
const cargando = ref(true);
const enviando = ref(false);
const error = ref("");
const intentoConfirmar = ref(false);
const confirmacion = ref<HTMLDialogElement | null>(null);

const etiquetaRol: Record<RolValidacion, string> = {
  PRINCIPAL: "Principal",
  APOYO_SECUNDARIO: "Apoyo secundario",
  SIN_HABILITACION_VIGENTE: "Sin habilitación vigente"
};

const lotesSinOpciones = computed(() => lotes.value.filter(l => !l.ofertas.length));
const faltaElegir = computed(() => lotes.value.filter(l => l.ofertas.length && !elegidas.value[l.loteId]));

async function cargar() {
  cargando.value = true;
  error.value = "";
  try {
    const datos = await opcionesAdjudicacion(props.emergencia.id);
    lotes.value = datos.lotes;
    avisoCompromiso.value = datos.emergencia.avisoCompromiso;
    coberturaParcial.value = datos.emergencia.coberturaParcial;
    // Si un lote tiene una sola opción, ya queda elegida
    elegidas.value = Object.fromEntries(datos.lotes
      .filter(l => l.ofertas.length === 1)
      .map(l => [l.loteId, l.ofertas[0]!.id]));
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudieron cargar las ofertas validadas";
  } finally {
    cargando.value = false;
  }
}

onMounted(cargar);

function nombreOferta(oferta: Oferta) {
  return oferta.consorcio ? `Consorcio liderado por ${oferta.liderNombre}` : oferta.liderNombre;
}

function ofertaElegida(lote: LoteAdjudicable) {
  return lote.ofertas.find(o => o.id === elegidas.value[lote.loteId]);
}

function revisar() {
  intentoConfirmar.value = true;
  error.value = "";
  if ((!coberturaParcial.value && lotesSinOpciones.value.length) || faltaElegir.value.length) return;
  confirmacion.value?.showModal();
}

async function confirmar() {
  if (enviando.value) return;
  enviando.value = true;
  error.value = "";
  try {
    // En cobertura parcial, los lotes sin opciones no se mandan: quedan sin cubrir
    await adjudicar(props.emergencia.id, lotes.value
      .filter(l => l.ofertas.length)
      .map(l => ({ loteId: l.loteId, ofertaId: elegidas.value[l.loteId]! })));
    confirmacion.value?.close();
    emit("adjudicado");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudo adjudicar";
  } finally {
    enviando.value = false;
  }
}
</script>

<template>
  <section class="emergencia-card adjudicar" :aria-busy="cargando">
    <span class="inicio-etiqueta emergencia-etiqueta">Adjudicar y notificar</span>
    <h1 class="emergencia-titulo">{{ capitalizar(emergencia.tipoDesastre) }} · {{ emergencia.zonaAfectada }}</h1>
    <p class="lote-resumen">Emergencia #{{ emergencia.id }} · Gravedad {{ capitalizar(emergencia.gravedad) }}</p>
    <p class="ong-ayuda">
      Estas son las ofertas que validó el Sistema Nacional y que cubren cada lote completo.
      Elegí una por lote: las ONGs seleccionadas quedan como responsables y al resto se le avisa que no fueron elegidas.
    </p>

    <p v-if="cargando" role="status">Cargando ofertas validadas...</p>
    <p v-else-if="error && !lotes.length" class="emergencia-error" role="alert">
      {{ error }}
      <button type="button" class="bandeja-actualizar" @click="cargar">Reintentar</button>
    </p>

    <template v-else>
      <p v-if="avisoCompromiso" class="emergencia-error" role="alert">{{ avisoCompromiso }}</p>
      <p v-if="coberturaParcial" class="ong-ayuda" role="note">
        El coordinador decidió continuar con cobertura parcial: adjudicás los lotes que tienen ofertas validadas y el resto queda sin cubrir.
      </p>
      <p v-else-if="lotesSinOpciones.length" class="emergencia-error" role="alert">
        No se puede adjudicar: {{ lotesSinOpciones.map(l => l.recurso).join(", ") }}
        {{ lotesSinOpciones.length === 1 ? "no tiene" : "no tienen" }} ofertas validadas que cubran el lote.
      </p>
      <fieldset v-for="lote in lotes" :key="lote.loteId" class="adjudicar-lote"
        :aria-invalid="intentoConfirmar && !elegidas[lote.loteId]">
        <legend>{{ lote.recurso }} · se necesitan {{ lote.requerido }}</legend>
        <p v-if="!lote.ofertas.length" :class="coberturaParcial ? 'ong-ayuda' : 'emergencia-error'">
          {{ coberturaParcial ? "Queda sin cubrir: ninguna oferta validada lo cubre." : "Ninguna oferta validada cubre este lote." }}
        </p>
        <label v-for="oferta in lote.ofertas" :key="oferta.id" class="adjudicar-opcion">
          <input v-model="elegidas[lote.loteId]" type="radio" :name="`lote-${lote.loteId}`" :value="oferta.id" />
          <span class="adjudicar-opcion-texto">
            <strong>{{ nombreOferta(oferta) }}</strong>
            <span v-if="oferta.rolValidacion" class="adjudicar-rol" :class="{ apoyo: oferta.rolValidacion !== 'PRINCIPAL' }">
              {{ etiquetaRol[oferta.rolValidacion] }}
            </span>
            <span class="ong-ayuda">Validado por el Sistema Nacional: {{ oferta.cantidadValidada }} de {{ lote.requerido }}</span>
            <ul v-if="oferta.consorcio" class="ong-aportes">
              <li v-for="aporte in oferta.aportes.filter(a => a.estado === 'CONFIRMADO')" :key="aporte.ongId">
                {{ aporte.ongNombre }}: {{ aporte.cantidad }}
                <template v-if="aporte.rolValidacion"> · {{ etiquetaRol[aporte.rolValidacion] }}</template>
              </li>
            </ul>
            <ul v-if="oferta.observacionesValidacion.length" class="ong-aportes adjudicar-observaciones">
              <li v-for="(observacion, i) in oferta.observacionesValidacion" :key="i">{{ observacion }}</li>
            </ul>
          </span>
        </label>
        <p v-if="intentoConfirmar && lote.ofertas.length && !elegidas[lote.loteId]" class="campo-error">Elegí una oferta para este lote.</p>
      </fieldset>

      <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>

      <div class="lote-acciones">
        <button type="button" class="bandeja-actualizar" @click="emit('volver')">Volver</button>
        <button type="button" class="btn-ingresar emergencia-submit" :disabled="enviando || (!coberturaParcial && lotesSinOpciones.length > 0)" @click="revisar">Adjudicar</button>
      </div>
    </template>

    <dialog ref="confirmacion" class="modal-salir modal-emergencia" aria-labelledby="confirmar-adjudicacion-titulo"
      :aria-busy="enviando" @cancel.prevent="!enviando && confirmacion?.close()">
      <h2 id="confirmar-adjudicacion-titulo">Confirmar adjudicación</h2>
      <p>Se notificará a las ONGs y se comprometerán los recursos en el Sistema Nacional.</p>
      <dl class="confirmacion-resumen">
        <div v-for="lote in lotes" :key="lote.loteId">
          <dt>{{ lote.recurso }} ({{ lote.requerido }})</dt>
          <dd>{{ ofertaElegida(lote) ? nombreOferta(ofertaElegida(lote)!) : "Sin cubrir" }}</dd>
        </div>
      </dl>
      <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="enviando" autofocus @click="confirmacion?.close()">Revisar</button>
        <button type="button" class="btn-ingresar" :disabled="enviando" @click="confirmar">
          {{ enviando ? "Adjudicando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
