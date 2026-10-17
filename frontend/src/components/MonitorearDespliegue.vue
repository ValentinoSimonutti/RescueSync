<script setup lang="ts">
import { onMounted, ref } from "vue";
import type { Tarea } from "../services/tareaService";
import { despliegueDe, iniciarDespliegue, type Despliegue } from "../services/ejecucionService";
import { capitalizar } from "../utils/tiempo";
import DespliegueDetalle from "./DespliegueDetalle.vue";

// "Monitorear estado de despliegue": el Sistema Nacional ya comprometió los recursos.
// El coordinador revisa qué ONG pone qué y da inicio a la ejecución.
const props = defineProps<{ tarea: Tarea }>();
const emit = defineEmits<{ volver: []; iniciado: [] }>();

const despliegue = ref<Despliegue | null>(null);
const cargando = ref(true);
const enviando = ref(false);
const error = ref("");
const confirmacion = ref<HTMLDialogElement | null>(null);

async function cargar() {
  cargando.value = true;
  error.value = "";
  try {
    despliegue.value = await despliegueDe(props.tarea.emergencia.id);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudo cargar el despliegue";
  } finally {
    cargando.value = false;
  }
}
onMounted(cargar);

async function iniciar() {
  if (enviando.value) return;
  enviando.value = true;
  error.value = "";
  try {
    await iniciarDespliegue(props.tarea.emergencia.id);
    confirmacion.value?.close();
    emit("iniciado");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudo iniciar el despliegue";
  } finally {
    enviando.value = false;
  }
}
</script>

<template>
  <section class="emergencia-card" :aria-busy="cargando">
    <span class="inicio-etiqueta emergencia-etiqueta">Monitorear estado de despliegue</span>
    <h1 class="emergencia-titulo">{{ capitalizar(tarea.emergencia.tipoDesastre) }} · {{ tarea.emergencia.zonaAfectada }}</h1>
    <p class="lote-resumen">Emergencia #{{ tarea.emergencia.id }} · Gravedad {{ capitalizar(tarea.emergencia.gravedad) }}</p>
    <p class="ong-ayuda">
      El Sistema Nacional ya comprometió estos recursos: las ONGs no pueden ofrecerlos en otra emergencia.
      Al iniciar el despliegue, cada ONG puede marcar sus actividades como finalizadas. El avance se sigue en "Despliegues".
    </p>

    <p v-if="cargando" role="status">Cargando despliegue...</p>
    <DespliegueDetalle v-else-if="despliegue" :despliegue="despliegue" />
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>

    <div class="lote-acciones">
      <button type="button" class="bandeja-actualizar" @click="emit('volver')">Volver</button>
      <button type="button" class="btn-ingresar emergencia-submit" :disabled="!despliegue || enviando"
        @click="confirmacion?.showModal()">Iniciar despliegue</button>
    </div>

    <dialog ref="confirmacion" class="modal-salir modal-emergencia" aria-labelledby="iniciar-despliegue-titulo"
      :aria-busy="enviando" @cancel.prevent="!enviando && confirmacion?.close()">
      <h2 id="iniciar-despliegue-titulo">Iniciar despliegue</h2>
      <p>Las ONGs adjudicadas van a poder marcar sus actividades como finalizadas. ¿Confirmás el inicio?</p>
      <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="enviando" autofocus @click="confirmacion?.close()">Cancelar</button>
        <button type="button" class="btn-ingresar" :disabled="enviando" @click="iniciar">
          {{ enviando ? "Iniciando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
