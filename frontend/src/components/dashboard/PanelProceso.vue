<script setup lang="ts">
import { computed, ref } from "vue";
import type { ChartData, ChartOptions } from "chart.js";
import type { DashboardBonita, EtapaBonita, TareaBonita } from "../../services/dashboardService";
import { duracion, fechaHora, formatoNumero, redondear, type Columna } from "../../utils/exportar";
import { BARRA, SERIE, opcionesBarras } from "../../utils/graficos";
import Grafico from "./Grafico.vue";
import TarjetaDatos from "./TarjetaDatos.vue";

// Panel con los tiempos del proceso, calculados a partir de la API de Bonita
const props = defineProps<{ datos: DashboardBonita }>();

// Clic en una barra de etapa -> la tabla de tareas muestra solo esa etapa
const etapaElegida = ref<string | null>(null);

const etapasConDatos = computed(() => props.datos.etapas.filter(e => e.casos > 0));

const graficoEtapas = computed<ChartData<"bar">>(() => ({
  labels: props.datos.etapas.map(e => `${e.orden}. ${e.nombre}`),
  datasets: [{
    label: "Tiempo promedio",
    data: props.datos.etapas.map(e => e.promedioMin),
    backgroundColor: props.datos.etapas.map(e =>
      !etapaElegida.value || etapaElegida.value === e.nombre ? SERIE[0] : "#c9d8ee"),
    ...BARRA
  }]
}));

const opcionesEtapas = computed<ChartOptions<"bar">>(() => {
  const base = opcionesBarras({ horizontal: true, formatoValor: duracion });
  return {
    ...base,
    onClick: (_evento, elementos) => {
      const etapa = elementos.length ? props.datos.etapas[elementos[0]!.index] : null;
      etapaElegida.value = !etapa || etapaElegida.value === etapa.nombre ? null : etapa.nombre;
    },
    onHover: (evento, elementos) => {
      const lienzo = evento.native?.target as HTMLElement | undefined;
      if (lienzo) lienzo.style.cursor = elementos.length ? "pointer" : "default";
    },
    plugins: {
      ...base.plugins,
      tooltip: {
        ...base.plugins!.tooltip,
        callbacks: {
          label: ctx => {
            const e = props.datos.etapas[ctx.dataIndex]!;
            return e.casos === 0 ? " Ningún caso pasó todavía por esta etapa" : ` Promedio: ${duracion(e.promedioMin)}`;
          },
          afterLabel: ctx => {
            const e = props.datos.etapas[ctx.dataIndex]!;
            if (e.casos === 0) return "";
            return [`Mínimo: ${duracion(e.minimoMin)} · Máximo: ${duracion(e.maximoMin)}`,
              `${e.casos} ${e.casos === 1 ? "caso" : "casos"} · clic para ver sus tareas`];
          }
        }
      }
    }
  };
});

const tareasFiltradas = computed(() =>
  etapaElegida.value ? props.datos.tareas.filter(t => t.etapa === etapaElegida.value) : props.datos.tareas);

const columnasEtapas: Columna<EtapaBonita>[] = [
  { titulo: "Etapa", valor: e => `${e.orden}. ${e.nombre}` },
  { titulo: "Casos", valor: e => e.casos },
  { titulo: "Promedio", tituloCsv: "Promedio (min)", valor: e => duracion(e.promedioMin), crudo: e => redondear(e.promedioMin) },
  { titulo: "Mínimo", tituloCsv: "Mínimo (min)", valor: e => duracion(e.minimoMin), crudo: e => redondear(e.minimoMin) },
  { titulo: "Máximo", tituloCsv: "Máximo (min)", valor: e => duracion(e.maximoMin), crudo: e => redondear(e.maximoMin) }
];

const columnasTareas: Columna<TareaBonita>[] = [
  { titulo: "Tarea", valor: t => t.nombre },
  { titulo: "Etapa", valor: t => t.etapa },
  { titulo: "Responsable", valor: t => t.responsable },
  { titulo: "Tipo", valor: t => t.tipo },
  { titulo: "Ejecuciones", valor: t => t.ejecuciones },
  { titulo: "Promedio", tituloCsv: "Promedio (min)", valor: t => duracion(t.promedioMin), crudo: t => redondear(t.promedioMin) },
  { titulo: "Máximo", tituloCsv: "Máximo (min)", valor: t => duracion(t.maximoMin), crudo: t => redondear(t.maximoMin) }
];

type Pendiente = DashboardBonita["pendientes"][number];
const columnasPendientes: Columna<Pendiente>[] = [
  { titulo: "Tarea", valor: p => p.tarea },
  { titulo: "Responsable", valor: p => p.responsable },
  { titulo: "Casos esperando", valor: p => p.cantidad },
  { titulo: "Espera promedio", tituloCsv: "Espera promedio (min)", valor: p => duracion(p.esperaPromedioMin), crudo: p => redondear(p.esperaPromedioMin) }
];

type Caso = DashboardBonita["casos"][number];
const columnasCasos: Columna<Caso>[] = [
  { titulo: "Caso", valor: c => c.caseId },
  { titulo: "Emergencia", valor: c => c.emergenciaId ? `#${c.emergenciaId} · ${c.zona}` : "—" },
  { titulo: "Estado", valor: c => c.estado },
  { titulo: "Inicio", valor: c => fechaHora(c.inicio), crudo: c => c.inicio },
  { titulo: "Fin", valor: c => fechaHora(c.fin), crudo: c => c.fin },
  { titulo: "Duración", tituloCsv: "Duración (min)", valor: c => duracion(c.duracionMin), crudo: c => redondear(c.duracionMin) },
  { titulo: "Esperando en", valor: c => c.etapaActual ?? "—" }
];
</script>

<template>
  <div class="dash-kpis">
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Casos en curso</span>
      <strong>{{ formatoNumero(datos.casosActivos) }}</strong>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Casos finalizados</span>
      <strong>{{ formatoNumero(datos.casosFinalizados) }}</strong>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Duración promedio de un caso</span>
      <strong>{{ duracion(datos.duracionPromedioCasoMin) }}</strong>
      <span class="dash-kpi-detalle">de registro a cierre, solo finalizados</span>
    </div>
  </div>

  <p v-if="datos.advertencia" class="dash-aviso" role="status">{{ datos.advertencia }}</p>

  <div class="dash-grilla">
    <TarjetaDatos titulo="Tiempo promedio por etapa" archivo="bonita-tiempo-por-etapa"
      descripcion="Las 7 etapas del proceso. Hacé clic en una barra para ver sus tareas."
      :filas="etapasConDatos.length ? datos.etapas : []" :columnas="columnasEtapas"
      vacio="Todavía no hay tareas terminadas en Bonita para este período.">
      <Grafico :datos="graficoEtapas" :opciones="opcionesEtapas" :alto="300"
        descripcion="Gráfico de barras horizontales con el tiempo promedio de cada etapa del proceso" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Tareas del proceso" archivo="bonita-tareas" solo-tabla
      :descripcion="etapaElegida ? `Etapa: ${etapaElegida}` : 'Cada tarea humana o automática del diagrama.'"
      :filas="tareasFiltradas" :columnas="columnasTareas">
      <template #acciones>
        <button v-if="etapaElegida" type="button" class="dash-boton-chico" @click="etapaElegida = null">
          Ver todas
        </button>
      </template>
    </TarjetaDatos>

    <TarjetaDatos titulo="Tareas esperando ahora" archivo="bonita-pendientes" solo-tabla
      descripcion="Dónde se acumula el trabajo en este momento."
      :filas="datos.pendientes" :columnas="columnasPendientes" vacio="No hay tareas humanas esperando." />

    <TarjetaDatos titulo="Casos de Bonita" archivo="bonita-casos" solo-tabla ancha
      descripcion="Cada emergencia es un caso del proceso RescueSync."
      :filas="datos.casos" :columnas="columnasCasos" />
  </div>
</template>
