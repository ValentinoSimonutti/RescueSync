<script setup lang="ts">
import { computed } from "vue";
import type { ChartData } from "chart.js";
import type { Conteo, DashboardNacional } from "../../services/dashboardService";
import { etiqueta, fechaHora, formatoNumero, porcentaje, type Columna } from "../../utils/exportar";
import { BARRA, SERIE, opcionesBarras } from "../../utils/graficos";
import Grafico from "./Grafico.vue";
import TarjetaDatos from "./TarjetaDatos.vue";

// Panel con el estado de los recursos a nivel país (API del Sistema Nacional)
const props = defineProps<{ datos: DashboardNacional }>();

type Recurso = DashboardNacional["recursos"][number];
const ocupacion = (r: Recurso) => r.total ? (r.comprometido * 100) / r.total : 0;

// Cada recurso tiene su unidad (litros, personas...): se compara el % comprometido, no las cantidades
const graficoOcupacion = computed<ChartData<"bar">>(() => ({
  labels: props.datos.recursos.map(r => r.nombre),
  datasets: [{
    label: "Comprometido",
    data: props.datos.recursos.map(ocupacion),
    backgroundColor: SERIE[0],
    ...BARRA
  }]
}));
const opcionesOcupacion = computed(() => {
  const base = opcionesBarras({ horizontal: true, maximo: 100, formatoValor: v => `${formatoNumero(v)} %` });
  base.plugins!.tooltip!.callbacks = {
    label: ctx => ` Comprometido: ${porcentaje(Number(ctx.raw))}`,
    afterLabel: ctx => {
      const r = props.datos.recursos[ctx.dataIndex]!;
      return `${formatoNumero(r.comprometido)} de ${formatoNumero(r.total)} ${r.unidad} · ${formatoNumero(r.disponible)} disponibles`;
    }
  };
  return base;
});

const columnasRecursos: Columna<Recurso>[] = [
  { titulo: "Recurso", valor: r => r.nombre },
  { titulo: "Código", valor: r => r.codigo },
  { titulo: "Unidad", valor: r => r.unidad },
  { titulo: "Inventario", valor: r => formatoNumero(r.total), crudo: r => r.total },
  { titulo: "Comprometido", valor: r => formatoNumero(r.comprometido), crudo: r => r.comprometido },
  { titulo: "Disponible", valor: r => formatoNumero(r.disponible), crudo: r => r.disponible },
  { titulo: "% comprometido", valor: r => porcentaje(ocupacion(r)), crudo: r => Math.round(ocupacion(r) * 10) / 10 }
];

const columnasOngs: Columna<Conteo>[] = [
  { titulo: "Estado", valor: c => etiqueta(c.clave), crudo: c => c.clave },
  { titulo: "ONGs", valor: c => c.cantidad }
];

type Activo = DashboardNacional["detalleActivos"][number];
const columnasActivos: Columna<Activo>[] = [
  { titulo: "Emergencia", valor: c => c.emergenciaRef },
  { titulo: "ONG", valor: c => c.ong },
  { titulo: "Recurso", valor: c => c.recurso },
  { titulo: "Cantidad", valor: c => c.cantidad },
  { titulo: "Rol", valor: c => etiqueta(c.rol), crudo: c => c.rol },
  { titulo: "Comprometido el", valor: c => fechaHora(c.fecha), crudo: c => c.fecha }
];
</script>

<template>
  <div class="dash-kpis">
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">ONGs habilitadas</span>
      <strong>{{ formatoNumero(datos.ongsHabilitadas) }}</strong>
      <span class="dash-kpi-detalle">de {{ datos.ongsRegistradas }} registradas en el padrón</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Compromisos activos</span>
      <strong>{{ formatoNumero(datos.compromisosActivos) }}</strong>
      <span class="dash-kpi-detalle">{{ formatoNumero(datos.unidadesComprometidas) }} unidades bloqueadas</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Compromisos liberados</span>
      <strong>{{ formatoNumero(datos.compromisosLiberados) }}</strong>
      <span class="dash-kpi-detalle">recursos devueltos al cerrar</span>
    </div>
  </div>

  <div class="dash-grilla">
    <TarjetaDatos titulo="Ocupación de recursos a nivel nacional" archivo="sn-disponibilidad"
      descripcion="Porcentaje del inventario de las ONGs habilitadas que está comprometido en emergencias."
      :filas="datos.recursos" :columnas="columnasRecursos">
      <Grafico :datos="graficoOcupacion" :opciones="opcionesOcupacion" :alto="320"
        descripcion="Barras horizontales con el porcentaje comprometido de cada tipo de recurso" />
    </TarjetaDatos>

    <div class="dash-columna">
      <TarjetaDatos titulo="ONGs por estado" archivo="sn-ongs-por-estado" solo-tabla
        descripcion="Padrón nacional: el administrador habilita o suspende a cada ONG."
        :filas="datos.ongsPorEstado" :columnas="columnasOngs" />
      <TarjetaDatos titulo="Compromisos activos" archivo="sn-compromisos-activos" solo-tabla
        :filas="datos.detalleActivos" :columnas="columnasActivos"
        vacio="No hay recursos comprometidos en este momento." />
    </div>
  </div>
</template>
