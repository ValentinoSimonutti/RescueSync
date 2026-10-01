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
  oferta?: Oferta;             // la oferta donde participa esta ONG (si hay)
  ongs: Ong[];                 // ONGs para invitar a un consorcio
}>();

const emit = defineEmits<{ cambio: [] }>();   // avisa a LotesONG que recargue

const cantidad = ref(1);
const invitadas = ref<number[]>([]);
const enviando = ref(false);
const mensaje = ref<{ texto: string; ok: boolean } | null>(null);
const verHistorial = ref(false);
const editandoAporte = ref(false);
const modalRetiro = ref<HTMLDialogElement | null>(null);

// En qué situación está la ONG con este lote
const situacion = computed<"libre" | "invitada" | "participa">(() => {
  const estado = props.oferta?.miAporte?.estado;
  if (estado === "PENDIENTE") return "invitada";
  if (estado === "CONFIRMADO") return "participa";
  return "libre";
});

// Máximo que puede poner la ONG sin que la oferta supere lo que pide el lote
const maximo = computed(() => {
  if (!props.oferta) return props.lote.cantidad;
  const mio = props.oferta.miAporte?.estado === "CONFIRMADO" ? props.oferta.miAporte.cantidad : 0;
  return Math.max(1, props.lote.cantidad - (props.oferta.total - mio));
});

// Aportes que siguen vivos (se ocultan rechazos y retiros)
const aportesVisibles = computed(() =>
  (props.oferta?.aportes ?? []).filter(a => a.estado === "CONFIRMADO" || a.estado === "PENDIENTE"));

// Valor inicial del campo: mi aporte actual, o lo que falta cubrir, o lo que pide el lote
function reiniciarCantidad() {
  if (situacion.value === "participa" && props.oferta?.miAporte) {
    cantidad.value = props.oferta.miAporte.cantidad;
  } else if (situacion.value === "invitada") {
    cantidad.value = maximo.value;
  } else {
    const falta = props.cobertura?.faltante ?? 0;
    cantidad.value = falta > 0 ? Math.min(falta, props.lote.cantidad) : props.lote.cantidad;
  }
}
reiniciarCantidad();
// Solo se reinicia si cambió la oferta (no en cada refresco automático, para no pisar lo que escribe)
watch(() => [props.oferta?.id, props.oferta?.version, props.oferta?.miAporte?.estado], reiniciarCantidad);
watch(() => [props.oferta?.id, props.oferta?.version], () => { editandoAporte.value = false; });

function cantidadValida(max: number) {
  if (!Number.isInteger(cantidad.value) || cantidad.value < 1 || cantidad.value > max) {
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
  if (!cantidadValida(props.lote.cantidad)) return;
  const consorcio = invitadas.value.length > 0;
  void ejecutar(
    () => crearOferta(props.lote.id, cantidad.value, invitadas.value),
    consorcio ? "Oferta en consorcio creada. Las ONGs invitadas tienen que aceptar." : "Oferta enviada."
  ).then(() => { invitadas.value = []; });
}

function guardarAporte() {
  if (!props.oferta || !cantidadValida(maximo.value)) return;
  const eraInvitacion = situacion.value === "invitada";
  void ejecutar(
    () => actualizarAporte(props.oferta!.id, cantidad.value),
    eraInvitacion ? "Te sumaste al consorcio." : "Aporte actualizado (nueva versión)."
  ).then(ok => { if (ok && !props.oferta?.consorcio) editandoAporte.value = false; });
}

function cancelarEdicion() {
  reiniciarCantidad();
  editandoAporte.value = false;
}

const textoConfirmacionRetiro = computed(() => props.oferta?.soyLider
  ? "¿Retirar la oferta? Si es un consorcio, se retira para todas las ONGs."
  : situacion.value === "invitada" ? "¿Rechazar la invitación?" : "¿Salir del consorcio?");

function retirarme() {
  if (!props.oferta || enviando.value) return;
  modalRetiro.value?.showModal();
}

async function confirmarRetiro() {
  if (!props.oferta) return;
  const ok = await ejecutar(() => retirarOferta(props.oferta!.id), "");
  if (ok) modalRetiro.value?.close();
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
      <div class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Puedo aportar</span>
          <input v-model.number="cantidad" type="number" min="1" :max="lote.cantidad" :disabled="enviando" />
        </label>
        <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando" @click="ofrecer">
          {{ enviando ? "Enviando..." : invitadas.length ? "Ofrecer en consorcio" : "Ofrecer ayuda" }}
        </button>
      </div>
      <details v-if="ongs.length" class="ong-consorcio">
        <summary>Ofertar en conjunto con otras ONGs (consorcio)</summary>
        <label v-for="o in ongs" :key="o.id" class="ong-consorcio-opcion">
          <input v-model="invitadas" type="checkbox" :value="o.id" :disabled="enviando" /> {{ o.nombre }}
        </label>
      </details>
    </div>

    <!-- 2. La invitaron a un consorcio -->
    <div v-else-if="situacion === 'invitada' && oferta" class="ong-oferta ong-invitacion">
      <p>
        <strong>{{ oferta.liderNombre }}</strong> te invitó a un consorcio para este lote.
        Llevan {{ oferta.total }} de {{ oferta.cantidadRequerida }}.
      </p>
      <div class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Aporto</span>
          <input v-model.number="cantidad" type="number" min="1" :max="maximo" :disabled="enviando" />
        </label>
        <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando" @click="guardarAporte">
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
      <ul class="ong-aportes">
        <li v-for="a in aportesVisibles" :key="a.ongId">
          {{ a.ongNombre }}<template v-if="a.ongId === oferta.liderId"> (líder)</template>:
          {{ a.estado === "PENDIENTE" ? "invitación pendiente" : a.cantidad }}
        </li>
      </ul>
      <div class="ong-oferta-fila">
        <label class="ong-oferta-campo">
          <span>Mi aporte</span>
          <input v-model.number="cantidad" type="number" min="1" :max="maximo" :disabled="enviando || (!oferta.consorcio && !editandoAporte)" />
        </label>
        <template v-if="!oferta.consorcio">
          <template v-if="editandoAporte">
            <button type="button" class="btn-ingresar bandeja-boton ong-guardar-cambios" :disabled="enviando" @click="guardarAporte">Guardar cambios</button>
            <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="cancelarEdicion">Cancelar</button>
          </template>
          <template v-else>
            <button type="button" class="btn-ingresar bandeja-boton" :disabled="enviando" @click="editandoAporte = true">Editar</button>
            <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="retirarme">Retirar oferta</button>
          </template>
        </template>
        <template v-else>
          <button type="button" class="btn-ingresar bandeja-boton ong-guardar-cambios" :disabled="enviando" @click="guardarAporte">Guardar cambios</button>
          <button type="button" class="bandeja-actualizar" :disabled="enviando" @click="retirarme">
            {{ oferta.soyLider ? "Retirar oferta" : "Salir del consorcio" }}
          </button>
        </template>
      </div>
      <button type="button" class="enlace-boton" @click="verHistorial = !verHistorial">
        {{ verHistorial ? "Ocultar historial" : "Ver historial de cambios" }}
      </button>
      <HistorialOferta v-if="verHistorial" :oferta-id="oferta.id" :key="oferta.version" />
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
