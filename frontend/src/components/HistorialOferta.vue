<script setup lang="ts">
import { onMounted, ref } from "vue";
import { historialOferta, type VersionOferta } from "../services/ofertaService";
import { fechaCorta } from "../utils/tiempo";

// Trazabilidad: todas las versiones de una oferta (la más nueva primero)
const props = defineProps<{ ofertaId: number }>();

const versiones = ref<VersionOferta[]>([]);
const cargando = ref(true);
const error = ref("");

onMounted(async () => {
  try {
    versiones.value = await historialOferta(props.ofertaId);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "No se pudo cargar el historial.";
  } finally {
    cargando.value = false;
  }
});
</script>

<template>
  <div class="historial">
    <p v-if="cargando" role="status">Cargando historial...</p>
    <p v-else-if="error" class="ong-oferta-error" role="alert">{{ error }}</p>
    <ol v-else class="historial-lista">
      <li v-for="v in versiones" :key="v.numero">
        <span class="historial-version">v{{ v.numero }}</span>
        <span>
          <strong>{{ v.autor }}</strong> · {{ v.accion }}
          <small>{{ fechaCorta(v.fecha) }} · total {{ v.total }} · {{ v.detalle }}</small>
        </span>
      </li>
    </ol>
  </div>
</template>
