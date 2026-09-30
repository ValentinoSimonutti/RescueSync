<script setup lang="ts">
import { primeraLetraMayuscula } from "../utils/entradaTexto";
import { computed, nextTick, ref } from "vue";
import type { Tarea } from "../services/tareaService";
import { publicarLotes, type NuevoLote } from "../services/emergenciaService";

// La tarea que se eligió en la bandeja (trae la emergencia adentro)
const props = defineProps<{
  tarea: Tarea;
}>();

const emit = defineEmits<{
  volver: [];       // volver a la bandeja sin hacer nada
  publicado: [];    // se publicó bien -> App.vue vuelve a la bandeja
}>();

// Lista de lotes que el coordinador va armando. Arranca con una fila vacía.
const lotes = ref<NuevoLote[]>([{ recurso: "", cantidad: 1 }]);

// Duración de la ventana de ofertas en minutos (2 para probar rápido el timer)
const duracionMinutos = ref(2);

const error = ref("");
const cargando = ref(false);
const intentoPublicar = ref(false);
const formulario = ref<HTMLElement | null>(null);
const confirmacion = ref<HTMLDialogElement | null>(null);
const cantidadValida = (valor: number) => Number.isSafeInteger(valor) && valor > 0;
const erroresLotes = computed(() => lotes.value.map(lote => ({
  recurso: lote.recurso.trim() ? "" : "Ingresá el recurso necesario.",
  cantidad: cantidadValida(lote.cantidad) ? "" : "Ingresá una cantidad entera mayor a 0."
})));
const errorDuracion = computed(() => cantidadValida(duracionMinutos.value) ? "" : "Ingresá una duración entera de al menos 1 minuto.");

function validar() {
  intentoPublicar.value = true;
  if (lotes.value.length && !errorDuracion.value && !erroresLotes.value.some(l => l.recurso || l.cantidad)) return true;
  confirmacion.value?.close();
  void nextTick(() => formulario.value?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());
  return false;
}

function revisar() {
  if (cargando.value) return;
  error.value = "";
  if (validar()) confirmacion.value?.showModal();
}

function editar() {
  if (!cargando.value) confirmacion.value?.close();
}

function capitalizar(valor: string) {
  return valor
    .toLocaleLowerCase("es")
    .replace(/^./u, (letra) => letra.toLocaleUpperCase("es"));
}

function agregarLote() {
  lotes.value.push({ recurso: "", cantidad: 1 });
}

function quitarLote(indice: number) {
  lotes.value.splice(indice, 1);
}

async function publicar() {
  if (cargando.value || !validar()) return;
  error.value = "";

  cargando.value = true;
  try {
    await publicarLotes(props.tarea.emergencia.id, {
      duracionMinutos: duracionMinutos.value,
      lotes: lotes.value
    });
    confirmacion.value?.close();
    emit("publicado");
  } catch (e) {
    // Mensaje real del backend (ej: sin permiso, Bonita apagado, ya publicada)
    error.value = e instanceof Error ? e.message : "No se pudo publicar la convocatoria";
  } finally {
    cargando.value = false;
  }
}
</script>

<template>
  <section ref="formulario" class="emergencia-card generar-lotes">
    <span class="inicio-etiqueta emergencia-etiqueta lote-etiqueta">Generar lotes</span>
    <h1 class="emergencia-titulo">
      {{ capitalizar(tarea.emergencia.tipoDesastre) }} · {{ tarea.emergencia.zonaAfectada }}
    </h1>
    <p class="lote-resumen">
      Emergencia #{{ tarea.emergencia.id }} · Gravedad {{ capitalizar(tarea.emergencia.gravedad) }}
    </p>
    <p v-if="tarea.emergencia.descripcion" class="lote-descripcion">
      <strong>Descripción:</strong> {{ tarea.emergencia.descripcion }}
    </p>

    <p class="lotes-requeridos">Todos los campos son obligatorios.</p>

    <!-- Una fila por lote -->
    <div v-for="(lote, i) in lotes" :key="i" class="lote-fila">
      <div class="emergencia-campo lote-recurso">
        <label :for="`recurso-${i}`">Recurso</label>
        <input :id="`recurso-${i}`" v-model="lote.recurso" @input="primeraLetraMayuscula($event, valor => lote.recurso = valor)" autocapitalize="sentences" type="text" placeholder="Ej: Paramédicos" required
          :aria-invalid="intentoPublicar && !!erroresLotes[i]?.recurso"
          :aria-describedby="intentoPublicar && erroresLotes[i]?.recurso ? `error-recurso-${i}` : undefined" />
        <p v-if="intentoPublicar && erroresLotes[i]?.recurso" :id="`error-recurso-${i}`" class="campo-error">{{ erroresLotes[i]?.recurso }}</p>
      </div>
      <div class="emergencia-campo lote-cantidad">
        <label :for="`cantidad-${i}`">Cantidad</label>
        <input :id="`cantidad-${i}`" v-model.number="lote.cantidad" type="number" min="1" step="1" required
          :aria-invalid="intentoPublicar && !!erroresLotes[i]?.cantidad"
          :aria-describedby="intentoPublicar && erroresLotes[i]?.cantidad ? `error-cantidad-${i}` : undefined" />
        <p v-if="intentoPublicar && erroresLotes[i]?.cantidad" :id="`error-cantidad-${i}`" class="campo-error">{{ erroresLotes[i]?.cantidad }}</p>
      </div>
      <button type="button" class="lote-quitar" @click="quitarLote(i)" :disabled="lotes.length === 1"
              aria-label="Quitar lote">✕</button>
    </div>

    <button type="button" class="bandeja-actualizar lote-agregar" @click="agregarLote">+ Agregar lote</button>

    <div class="emergencia-campo lote-duracion">
      <label for="duracion">Duración de la ventana de ofertas (minutos)</label>
      <input id="duracion" v-model.number="duracionMinutos" type="number" min="1" step="1" required
        :aria-invalid="intentoPublicar && !!errorDuracion"
        :aria-describedby="intentoPublicar && errorDuracion ? 'error-duracion' : undefined" />
      <p v-if="intentoPublicar && errorDuracion" id="error-duracion" class="campo-error">{{ errorDuracion }}</p>
    </div>

    <p v-if="error" class="emergencia-error">{{ error }}</p>

    <div class="lote-acciones">
      <button type="button" class="bandeja-actualizar" @click="emit('volver')">Volver</button>
      <button type="button" class="btn-ingresar emergencia-submit" @click="revisar" :disabled="cargando">
        {{ cargando ? "Publicando..." : "Publicar convocatoria" }}
      </button>
    </div>
    <dialog ref="confirmacion" class="modal-salir modal-emergencia"
      aria-labelledby="confirmar-lotes-titulo" aria-describedby="confirmar-lotes-pregunta"
      :aria-busy="cargando" @cancel.prevent="editar">
      <h2 id="confirmar-lotes-titulo">Revisar convocatoria</h2>
      <p id="confirmar-lotes-pregunta">¿Confirmás la publicación de esta convocatoria?</p>
      <dl class="confirmacion-resumen">
        <div>
          <dt>Emergencia #{{ tarea.emergencia.id }}</dt>
          <dd>{{ capitalizar(tarea.emergencia.tipoDesastre) }} · {{ tarea.emergencia.zonaAfectada }}</dd>
        </div>
        <div>
          <dt>Gravedad</dt>
          <dd>{{ capitalizar(tarea.emergencia.gravedad) }}</dd>
        </div>
        <div v-for="(lote, i) in lotes" :key="i">
          <dt>Lote {{ i + 1 }}</dt>
          <dd>{{ lote.recurso }} · Cantidad: {{ lote.cantidad }}</dd>
        </div>
        <div>
          <dt>Duración de la ventana de ofertas</dt>
          <dd>{{ duracionMinutos }} {{ duracionMinutos === 1 ? 'minuto' : 'minutos' }}</dd>
        </div>
      </dl>
      <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="cargando" autofocus @click="editar">Editar</button>
        <button type="button" class="btn-ingresar" :disabled="cargando" @click="publicar">
          {{ cargando ? "Publicando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
