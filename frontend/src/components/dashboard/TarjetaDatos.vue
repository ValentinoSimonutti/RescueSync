<script setup lang="ts" generic="T">
import { ref } from "vue";
import { descargarCsv, type Columna } from "../../utils/exportar";

// Tarjeta del dashboard: título, gráfico (slot) y la tabla con los mismos datos.
// Si no hay gráfico (soloTabla), la tabla se ve siempre; si no, se despliega con "Ver datos".
// Cada tarjeta exporta sus filas a CSV.
// ocupa = cuántas de las 12 columnas de la grilla usa (en pantallas angostas, todas).
const props = defineProps<{
  titulo: string;
  descripcion?: string;
  filas: T[];
  columnas: Columna<T>[];
  archivo: string;
  soloTabla?: boolean;
  vacio?: string;
  ocupa?: 4 | 5 | 6 | 7 | 8 | 12;
}>();

const verTabla = ref(false);

function exportar() {
  descargarCsv(props.archivo, props.filas, props.columnas);
}
</script>

<template>
  <article class="dash-tarjeta" :class="`dash-ocupa-${ocupa ?? 6}`">
    <header class="dash-tarjeta-encabezado">
      <div>
        <h3>{{ titulo }}</h3>
        <p v-if="descripcion">{{ descripcion }}</p>
      </div>
      <div class="dash-tarjeta-acciones">
        <slot name="acciones" />
        <button v-if="!soloTabla && filas.length" type="button" class="dash-boton-chico"
          :aria-pressed="verTabla" @click="verTabla = !verTabla">
          {{ verTabla ? "Ver gráfico" : "Ver datos" }}
        </button>
        <button type="button" class="dash-boton-chico" :disabled="!filas.length" @click="exportar">CSV</button>
      </div>
    </header>

    <!-- Selectores propios de la tarjeta (ej: mes o recurso), en su propia fila -->
    <div v-if="$slots.filtros && filas.length" class="dash-tarjeta-filtros">
      <slot name="filtros" />
    </div>

    <p v-if="!filas.length" class="dash-vacio">{{ vacio ?? "Sin datos para el período elegido." }}</p>
    <div v-else-if="!soloTabla && !verTabla" class="dash-tarjeta-cuerpo">
      <slot />
    </div>
    <div v-else class="dash-tabla-contenedor">
      <table class="dash-tabla">
        <thead>
          <tr>
            <th v-for="c in columnas" :key="c.titulo" scope="col" :class="{ 'dash-num': c.alinear === 'derecha' }">
              {{ c.titulo }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(fila, i) in filas" :key="i">
            <td v-for="c in columnas" :key="c.titulo" :class="{ 'dash-num': c.alinear === 'derecha' }">
              <span v-if="c.tono?.(fila)" class="dash-insignia" :class="`dash-insignia-${c.tono(fila)}`">
                {{ c.valor(fila) ?? "—" }}
              </span>
              <template v-else>{{ c.valor(fila) ?? "—" }}</template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </article>
</template>