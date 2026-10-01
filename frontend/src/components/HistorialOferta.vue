<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { historialOferta, type VersionOferta } from "../services/ofertaService";
import { fechaCorta } from "../utils/tiempo";

type VersionConOferta = VersionOferta & { ofertaId: number };

// Permite mostrar en una cronología las versiones de una o varias ofertas del mismo lote.
const props = defineProps<{ ofertaId?: number; ofertaIds?: number[] }>();

const ofertaIds = computed(() => [...new Set(props.ofertaIds?.length
  ? props.ofertaIds
  : props.ofertaId !== undefined ? [props.ofertaId] : [])]);
const versiones = ref<VersionConOferta[]>([]);
const cargando = ref(true);
const error = ref("");
const historialCombinado = computed(() => ofertaIds.value.length > 1);
let solicitud = 0;

watch(ofertaIds, async ids => {
  const actual = ++solicitud;
  cargando.value = true;
  error.value = "";
  if (!ids.length) {
    versiones.value = [];
    cargando.value = false;
    return;
  }
  try {
    const historiales = await Promise.all(ids.map(async ofertaId => ({
      ofertaId,
      versiones: await historialOferta(ofertaId)
    })));
    if (actual !== solicitud) return;
    versiones.value = historiales
      .flatMap(({ ofertaId, versiones }) => versiones.map(version => ({ ...version, ofertaId })))
      .sort((a, b) => Date.parse(b.fecha) - Date.parse(a.fecha));
  } catch (e) {
    if (actual !== solicitud) return;
    error.value = e instanceof Error ? e.message : "No se pudo cargar el historial.";
  } finally {
    if (actual === solicitud) cargando.value = false;
  }
}, { immediate: true });
</script>

<template>
  <div class="historial">
    <p v-if="cargando" role="status">Cargando historial...</p>
    <p v-else-if="error" class="ong-oferta-error" role="alert">{{ error }}</p>
    <ol v-else class="historial-lista">
      <li v-for="v in versiones" :key="`${v.ofertaId}-${v.numero}`">
        <span class="historial-version">v{{ v.numero }}</span>
        <span>
          <strong>{{ v.autor }}</strong> · {{ v.accion }}
          <small>
            <template v-if="historialCombinado">Oferta #{{ v.ofertaId }} · </template>
            {{ fechaCorta(v.fecha) }} · total {{ v.total }} · {{ v.detalle }}
          </small>
        </span>
      </li>
    </ol>
  </div>
</template>
