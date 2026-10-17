<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { Lote } from "../services/emergenciaService";
import {
  actualizarAporte, crearOferta, retirarOferta,
  type CoberturaLote, type Oferta, type Ong
} from "../services/ofertaService";
import HistorialOferta from "./HistorialOferta.vue";

// Todo lo que una ONG puede hacer con UN lote:
//  - no participa  -> ofrecer (sola o invitando ONGs = consorcio)
//  - invitada      -> aceptar con su aporte o rechazar
//  - participa     -> modificar su aporte (nueva versión), retirarse, ver historial
const props = defineProps<{
  lote: Lote;
  cobertura?: CoberturaLote;   // cuánto se ofreció en total para el lote (todas las ONGs)
  oferta?: Oferta;             // oferta donde participa esta ONG para este lote
  ongs: Ong[];                 // ONGs para invitar a un consorcio
}>();

const emit = defineEmits<{ cambio: [] }>();   // avisa a LotesONG que recargue

const cantidad = ref(1);
const cantidadConsorcio = ref(1);
const invitadas = ref<number[]>([]);
const enviando = ref(false);
const mensaje = ref<{ texto: string; ok: boolean } | null>(null);
const verHistorial = ref(false);
const avisoConsorcioIndividual = ref(false);
const creandoConsorcio = ref(false);
const ofertaRetiradaLocal = ref(false);
const historialOfertaIds = ref<number[]>([]);
const editandoAporte = ref(false);
const modalRetiro = ref<HTMLDialogElement | null>(null);

const oferta = computed(() => props.oferta);

watch(() => props.oferta?.id, id => {
  if (id !== undefined && !historialOfertaIds.value.includes(id)) historialOfertaIds.value.push(id);
}, { immediate: true });

// En qué situación está la ONG con este lote
const situacion = computed<"libre" | "invitada" | "participa">(() => {
  if (ofertaRetiradaLocal.value) return "libre";
  const estado = oferta.value?.miAporte?.estado;
  if (estado === "PENDIENTE") return "invitada";
  if (estado === "CONFIRMADO") return "participa";
  return "libre";
});

// Entre todas las ofertas activas no se puede pasar lo que pide el lote.
// Lo que falta cubrir es el tope para una oferta nueva.
const faltante = computed(() => props.cobertura?.faltante ?? props.lote.cantidad);

// Mi aporte en una oferta existente: lo que pide el lote menos lo de las otras ONGs
// de esta oferta y lo de las otras ofertas. Bajar el aporte siempre se puede.
const maximo = computed(() => {
  if (!oferta.value) return faltante.value;
  const miAporte = oferta.value.miAporte?.estado === "CONFIRMADO" ? oferta.value.miAporte.cantidad : 0;
  const deOtrasOfertas = props.cobertura ? props.cobertura.ofrecido - oferta.value.total : 0;
  return Math.max(miAporte, oferta.value.cantidadRequerida - deOtrasOfertas - (oferta.value.total - miAporte));
});

// El líder deja al menos una unidad de lo que falta para las invitadas.
const maximoLiderConsorcio = computed(() => Math.max(0, faltante.value - 1));

// Solo se puede invitar a ONGs que todavía no tengan una participación viva en este lote.
const ongsInvitables = computed(() => {
  const participantes = new Set((props.cobertura?.ofertas ?? []).flatMap(o =>
    o.aportes
      .filter(a => a.estado === "CONFIRMADO" || a.estado === "PENDIENTE")
      .map(a => a.ongId)));
  return props.ongs.filter(o => !participantes.has(o.id));
});

// Aportes que siguen vivos (se ocultan rechazos y retiros)
const aportesVisibles = computed(() =>
  (oferta.value?.aportes ?? []).filter(a => a.estado === "CONFIRMADO" || a.estado === "PENDIENTE"));

// Valor inicial del campo: mi aporte actual, o lo que falta cubrir, o lo que pide el lote
function reiniciarCantidad() {
  if (situacion.value === "participa" && oferta.value?.miAporte) {
    cantidad.value = oferta.value.miAporte.cantidad;
  } else if (situacion.value === "invitada") {
    cantidad.value = maximo.value;
  } else {
    cantidad.value = Math.max(1, faltante.value);
  }
}
reiniciarCantidad();
// Solo se reinicia si cambió la oferta (no en cada refresco automático, para no pisar lo que escribe)
watch(() => [oferta.value?.id, oferta.value?.version, oferta.value?.miAporte?.estado], reiniciarCantidad);
watch(() => [oferta.value?.id, oferta.value?.version], () => { editandoAporte.value = false; });
let ofertaObservadaId: number | undefined;
let consorcioAnterior = false;
watch(() => oferta.value
  ? [oferta.value.id, oferta.value.version, oferta.value.consorcio, oferta.value.estado] as const
  : null, estadoActual => {
  if (!estadoActual) {
    ofertaObservadaId = undefined;
    consorcioAnterior = false;
    avisoConsorcioIndividual.value = false;
    creandoConsorcio.value = false;
    ofertaRetiradaLocal.value = false;
    invitadas.value = [];
    editandoAporte.value = false;
    return;
  }

  const [id, , consorcio] = estadoActual;
  if (id !== ofertaObservadaId) {
    ofertaObservadaId = id;
    if (!historialOfertaIds.value.includes(id)) historialOfertaIds.value.push(id);
    consorcioAnterior = consorcio;
    avisoConsorcioIndividual.value = false;
    creandoConsorcio.value = false;
    ofertaRetiradaLocal.value = false;
    return;
  }

  if (consorcioAnterior && !consorcio && oferta.value?.estado === "ACTIVA") {
    avisoConsorcioIndividual.value = true;
  }
  consorcioAnterior = consorcio;
}, { immediate: true });

function cantidadValida(valor: number, max: number) {
  if (max < 1) {
    mensaje.value = { texto: "No queda cantidad disponible dentro de esta oferta.", ok: false };
    return false;
  }
  if (!Number.isInteger(valor) || valor < 1 || valor > max) {
    mensaje.value = { texto: `La cantidad debe estar entre 1 y ${max}.`, ok: false };
    return false;
  }
  return true;
}

// Ejecuta una acción contra el backend y muestra el resultado
async function ejecutar(accion: () => Promise<unknown>, exito: string): Promise<boolean> {
  enviando.value = true;
  mensaje.value = null;
  try {
    await accion();
    mensaje.value = exito ? { texto: exito, ok: true } : null;
    emit("cambio");
    return true;
  } catch (e) {
    mensaje.value = { texto: e instanceof Error ? e.message : "No se pudo completar la acción.", ok: false };
    return false;
  } finally {
    enviando.value = false;
  }
}

function ofrecer() {
  const consorcio = invitadas.value.length > 0;
  const maximoOferta = consorcio ? maximoLiderConsorcio.value : faltante.value;
  const cantidadOferta = consorcio ? cantidadConsorcio.value : cantidad.value;
  if (!cantidadValida(cantidadOferta, maximoOferta)) return;
  void ejecutar(
    () => crearOferta(props.lote.id, cantidadOferta, invitadas.value),
    consorcio ? "Oferta en consorcio creada. Las ONGs invitadas tienen que aceptar." : "Oferta enviada."
  ).then(ok => {
    if (!ok) return;
    invitadas.value = [];
    creandoConsorcio.value = false;
  });
}

function configurarConsorcio() {
  invitadas.value = [];
  cantidadConsorcio.value = maximoLiderConsorcio.value;
  creandoConsorcio.value = true;
}

function iniciarConfiguracionConsorcio(event: Event) {
  if (event.currentTarget instanceof HTMLDetailsElement && event.currentTarget.open && ongsInvitables.value.length) {
    configurarConsorcio();
  }
}

function cancelarConfiguracionConsorcio() {
  creandoConsorcio.value = false;
  invitadas.value = [];
  cantidadConsorcio.value = maximoLiderConsorcio.value;
  reiniciarCantidad();
}

function guardarAporte() {
  if (!oferta.value || !cantidadValida(cantidad.value, maximo.value)) return;
  const eraInvitacion = situacion.value === "invitada";
  void ejecutar(
    () => actualizarAporte(oferta.value!.id, cantidad.value),
    eraInvitacion ? "Te sumaste al consorcio." : "Aporte actualizado (nueva versión)."
  ).then(ok => { if (ok && !oferta.value?.consorcio) editandoAporte.value = false; });
}

function cancelarEdicion() {
  reiniciarCantidad();
  editandoAporte.value = false;
}

const textoConfirmacionRetiro = computed(() => oferta.value?.soyLider
  ? "¿Cancelar el consorcio? Se retirará para todas las ONGs."
  : situacion.value === "invitada" ? "¿Rechazar la invitación?" : "¿Darse de baja del consorcio?");

function retirarme() {
  if (!oferta.value || enviando.value) return;
  modalRetiro.value?.showModal();
}

async function confirmarRetiro() {
  if (!oferta.value) return;
  const ofertaRetirada = oferta.value;
  const ok = await ejecutar(() => retirarOferta(ofertaRetirada.id), "");
  if (!ok) return;
  if (ofertaRetirada.soyLider) {
    ofertaRetiradaLocal.value = true;
    avisoConsorcioIndividual.value = false;
    creandoConsorcio.value = false;
    invitadas.value = [];
    reiniciarCantidad();
  }
  modalRetiro.value?.close();
}
</script>

<template>
  <li class="ong-lote ong-lote-oferta">
    <div class="ong-lote-cabecera">
      <div>
        <strong>{{ lote.recurso }}</strong>
        <p>Cantidad requerida: {{ lote.cantidad }}</p>
      </div>
      <!-- Cobertura total del lote (todas las ONGs): sirve para colaborar -->
      <div v-if="cobertura" class="cobertura-mini">
        <div class="cobertura-barra" role="img" :aria-label="`Cubierto ${cobertura.porcentaje}%`">
          <span :style="{ width: cobertura.porcentaje + '%' }" :class="{ completa: cobertura.cubierto }"></span>
        </div>
        <small>Ofrecido en total: {{ cobertura.ofrecido }} de {{ cobertura.requerido }}</small>
      </div>
    </div>

    <!-- 1. No participa: ofrecer sola o en consorcio -->
    <div v-if="situacion === 'libre'" class="ong-oferta">
      <p v-if="faltante < 1" class="ong-ayuda">Este lote ya está cubierto por otras ofertas.</p>
      <template v-else-if="!creandoConsorcio">
        <div class="ong-oferta-fila">
          <label class="ong-oferta-campo">
            <span>Puedo aportar</span>
            <input v-model.number="cantidad" type="number" min="1" :max="faltante" :disabled="enviando" />
          </label>
          <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando" @click="ofrecer">
            {{ enviando ? "Enviando..." : "Ofrecer ayuda" }}
          </button>
        </div>
        <!-- Un consorcio necesita que falten al menos 2 unidades: el líder deja una para las invitadas -->
        <details v-if="ongs.length && faltante > 1" class="ong-consorcio" @toggle="iniciarConfiguracionConsorcio">
          <summary>Ofertar en conjunto con otras ONGs (consorcio)</summary>
          <p v-if="!ongsInvitables.length" class="ong-ayuda">Las otras ONGs ya participan en ofertas activas para este lote.</p>
        </details>
      </template>
      <template v-else>
        <p class="ong-oferta-titulo">Configurar oferta en consorcio</p>
        <div class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Mi aporte</span>
          <input v-model.number="cantidadConsorcio" type="number" min="1" :max="maximoLiderConsorcio" :disabled="enviando || maximoLiderConsorcio < 1" />
        </label>
        </div>
        <p class="ong-ayuda">Elegí a quién invitar. Cada ONG invitada confirma su aporte al aceptar. Como líder, podés aportar hasta {{ maximoLiderConsorcio }}: el resto de lo que falta queda para las invitadas.</p>
        <label v-for="o in ongsInvitables" :key="o.id" class="ong-consorcio-opcion">
          <input v-model="invitadas" type="checkbox" :value="o.id" :disabled="enviando || maximoLiderConsorcio < 1" /> {{ o.nombre }}
        </label>
        <div class="ong-oferta-fila">
          <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando || invitadas.length === 0 || maximoLiderConsorcio < 1" @click="ofrecer">
            {{ enviando ? "Enviando..." : "Crear consorcio" }}
          </button>
          <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="cancelarConfiguracionConsorcio">Cancelar</button>
        </div>
      </template>
    </div>
    <!-- 2. La invitaron a un consorcio -->
    <div v-else-if="situacion === 'invitada' && oferta" class="ong-oferta ong-invitacion">
      <p>
        <strong>{{ oferta.liderNombre }}</strong> te invitó a un consorcio para este lote.
        Llevan {{ oferta.total }} de {{ oferta.cantidadRequerida }}.
      </p>
      <ul v-if="oferta.consorcio" class="ong-aportes">
        <li v-for="a in aportesVisibles" :key="a.ongId">
          {{ a.ongNombre }}<template v-if="a.ongId === oferta.liderId"> (líder)</template>:
          {{ a.estado === "PENDIENTE" ? "invitación pendiente" : a.cantidad }}
        </li>
      </ul>
      <div class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Aporto</span>
          <input v-model.number="cantidad" type="number" min="1" :max="maximo" :disabled="enviando || maximo < 1" />
        </label>
        <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando || maximo < 1" @click="guardarAporte">
          Aceptar y aportar
        </button>
        <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="retirarme">Rechazar</button>
      </div>
    </div>

    <!-- 3. Ya participa: ver, modificar (versiona), retirarse, historial -->
    <div v-else-if="oferta" class="ong-oferta ong-mi-oferta">
      <p class="ong-oferta-titulo">
        {{ oferta.consorcio ? "Oferta en consorcio" : "Tu oferta" }} ·
        total {{ oferta.total }} de {{ oferta.cantidadRequerida }} · versión {{ oferta.version }}
      </p>
      <ul v-if="oferta.consorcio" class="ong-aportes">
        <li v-for="a in aportesVisibles" :key="a.ongId">
          {{ a.ongNombre }}<template v-if="a.ongId === oferta.liderId"> (líder)</template>:
          {{ a.estado === "PENDIENTE" ? "invitación pendiente" : a.cantidad }}
        </li>
      </ul>
      <div v-if="!oferta.consorcio" class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Mi aporte</span>
          <input v-model.number="cantidad" type="number" min="1" :max="maximo" :disabled="enviando || !editandoAporte" />
        </label>
        <template v-if="editandoAporte">
          <button type="button" class="btn-ingresar bandeja-boton ong-guardar-cambios" :disabled="enviando" @click="guardarAporte">Guardar cambios</button>
          <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="cancelarEdicion">Cancelar</button>
        </template>
        <template v-else>
          <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando" @click="editandoAporte = true">Editar</button>
          <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="retirarme">Retirar oferta</button>
        </template>
      </div>
      <div v-else class="ong-oferta-fila">
        <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="retirarme">
          {{ oferta.soyLider ? "Cancelar consorcio" : "Darse de baja" }}
        </button>
      </div>
    </div>

    <div v-if="historialOfertaIds.length" class="ong-oferta ong-mi-oferta">
      <p class="ong-oferta-titulo">Historial de ofertas</p>
      <button type="button" class="enlace-boton" @click="verHistorial = !verHistorial">
        {{ verHistorial ? "Ocultar historial" : "Ver historial de cambios" }}
      </button>
      <template v-if="verHistorial">
        <p v-if="avisoConsorcioIndividual" class="ong-oferta-ok" role="status">
          La ONG invitada se dio de baja. La oferta pasó a ser individual.
        </p>
        <HistorialOferta
          :oferta-ids="historialOfertaIds"
          :key="`${historialOfertaIds.join('-')}-${oferta?.version ?? 'cerrada'}`"
        />
      </template>
    </div>

    <p v-if="mensaje" role="status" :class="mensaje.ok ? 'ong-oferta-ok' : 'ong-oferta-error'">{{ mensaje.texto }}</p>
    <dialog
      ref="modalRetiro"
      class="modal-salir"
      aria-labelledby="modal-retiro-titulo"
      aria-describedby="modal-retiro-descripcion"
      :aria-busy="enviando"
      @cancel.prevent="!enviando && modalRetiro?.close()"
    >
      <h2 id="modal-retiro-titulo">Confirmar acción</h2>
      <p id="modal-retiro-descripcion">{{ textoConfirmacionRetiro }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="enviando" autofocus @click="modalRetiro?.close()">Cancelar</button>
        <button type="button" class="btn-ingresar" :disabled="enviando" @click="confirmarRetiro">
          {{ enviando ? "Procesando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </li>
</template>
