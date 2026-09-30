<script setup lang="ts">
import { ref } from "vue";
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

function agregarLote() {
  lotes.value.push({ recurso: "", cantidad: 1 });
}

function quitarLote(indice: number) {
  lotes.value.splice(indice, 1);
}

async function publicar() {
  error.value = "";

  // Validación rápida en el front (el backend vuelve a validar igual)
  if (lotes.value.length === 0 || lotes.value.some(l => !l.recurso.trim() || l.cantidad < 1)) {
    error.value = "Cada lote necesita un recurso y una cantidad mayor a 0.";
    return;
  }

  cargando.value = true;
  try {
    await publicarLotes(props.tarea.emergencia.id, {
      duracionMinutos: duracionMinutos.value,
      lotes: lotes.value
    });
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
  <section class="emergencia-card">
    <span class="inicio-etiqueta emergencia-etiqueta">GENERAR LOTES</span>
    <h1 class="emergencia-titulo">
      {{ tarea.emergencia.tipoDesastre }} · {{ tarea.emergencia.zonaAfectada }}
    </h1>
    <p class="bandeja-detalle">
      Emergencia #{{ tarea.emergencia.id }} · Gravedad {{ tarea.emergencia.gravedad }}
    </p>
    <p v-if="tarea.emergencia.descripcion">{{ tarea.emergencia.descripcion }}</p>

    <!-- Una fila por lote -->
    <div v-for="(lote, i) in lotes" :key="i" class="lote-fila">
      <div class="emergencia-campo lote-recurso">
        <label :for="`recurso-${i}`">Recurso</label>
        <input :id="`recurso-${i}`" v-model="lote.recurso" type="text" placeholder="Ej: Paramédicos" />
      </div>
      <div class="emergencia-campo lote-cantidad">
        <label :for="`cantidad-${i}`">Cantidad</label>
        <input :id="`cantidad-${i}`" v-model.number="lote.cantidad" type="number" min="1" />
      </div>
      <button type="button" class="lote-quitar" @click="quitarLote(i)" :disabled="lotes.length === 1"
              aria-label="Quitar lote">✕</button>
    </div>

    <button type="button" class="bandeja-actualizar" @click="agregarLote">+ Agregar lote</button>

    <div class="emergencia-campo">
      <label for="duracion">Duración de la ventana de ofertas (minutos)</label>
      <input id="duracion" v-model.number="duracionMinutos" type="number" min="1" />
    </div>

    <p v-if="error" class="emergencia-error">{{ error }}</p>

    <div class="lote-acciones">
      <button type="button" class="bandeja-actualizar" @click="emit('volver')">Volver</button>
      <button type="button" class="btn-ingresar emergencia-submit" @click="publicar" :disabled="cargando">
        {{ cargando ? "Publicando..." : "Publicar convocatoria" }}
      </button>
    </div>
  </section>
</template>
