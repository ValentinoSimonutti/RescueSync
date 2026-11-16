<script setup lang="ts" generic="T">
import { ref } from "vue";
import { descargarCsv, type Columna } from "../../utils/exportar";

// Tarjeta del dashboard: título, gráfico (slot) y la tabla con los mismos datos.
// Si no hay gráfico (soloTabla), la tabla se ve siempre; si no, se despliega con "Ver datos".
// Cada tarjeta exporta sus filas a CSV.
const props = defineProps<{
  titulo: string;
  descripcion?: string;
  filas: T[];
  columnas: Columna<T>[];
  archivo: string;
  soloTabla?: boolean;
  vacio?: string;
  ancha?: boolean;
}>();

const verTabla = ref(false);

function exportar() {
  descargarCsv(props.archivo, props.filas, props.columnas);
}
</script>

<template>
  <article class="dash-tarjeta" :class="{ 'dash-tarjeta-ancha': ancha }">
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

    <p v-if="!filas.length" class="dash-vacio">{{ vacio ?? "Sin datos para el período elegido." }}</p>
    <div v-else-if="!soloTabla && !verTabla">
      <slot />
    </div>
    <div v-else class="dash-tabla-contenedor">
      <table class="dash-tabla">
        <thead>
          <tr><th v-for="c in columnas" :key="c.titulo" scope="col">{{ c.titulo }}</th></tr>
        </thead>
        <tbody>
          <tr v-for="(fila, i) in filas" :key="i">
            <td v-for="c in columnas" :key="c.titulo">{{ c.valor(fila) ?? "—" }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </article>
</template>
