<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { Chart, type ChartData, type ChartOptions } from "chart.js";
import { pluginEtiquetas } from "../../utils/graficos";

// Envoltorio mínimo de Chart.js: crea el gráfico al montar y lo actualiza cuando cambian los datos
const props = defineProps<{
  datos: ChartData<"bar">;
  opciones: ChartOptions<"bar">;
  descripcion: string;   // texto para lectores de pantalla
  alto?: number;
  // Si se pasa, escribe el valor al final de cada barra (ej: v => `${v} lotes`)
  etiqueta?: (valor: number, indice: number) => string;
}>();

const lienzo = ref<HTMLCanvasElement | null>(null);
let grafico: Chart<"bar"> | null = null;

onMounted(() => {
  if (!lienzo.value) return;
  grafico = new Chart(lienzo.value, {
    type: "bar",
    data: props.datos,
    options: props.opciones,
    plugins: [pluginEtiquetas(() => props.etiqueta)]
  });
});

watch(() => [props.datos, props.opciones] as const, ([datos, opciones]) => {
  if (!grafico) return;
  grafico.data = datos;
  grafico.options = opciones;
  grafico.update();
});

onBeforeUnmount(() => grafico?.destroy());
</script>

<template>
  <div class="grafico" :style="{ height: `${alto ?? 260}px` }">
    <canvas ref="lienzo" role="img" :aria-label="descripcion"></canvas>
  </div>
</template>