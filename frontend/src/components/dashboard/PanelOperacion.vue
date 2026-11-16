<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { ChartData } from "chart.js";
import type { Conteo, DashboardLocal } from "../../services/dashboardService";
import { duracion, etiqueta, fechaHora, formatoNumero, porcentaje, redondear, type Columna } from "../../utils/exportar";
import { BARRA, SERIE, opcionesBarras } from "../../utils/graficos";
import Grafico from "./Grafico.vue";
import TarjetaDatos from "./TarjetaDatos.vue";

// Panel con los indicadores propios (base de datos local de la app)
const props = defineProps<{
  datos: DashboardLocal;
  unidades: Record<string, string>;   // nombre del recurso -> unidad (sale del Sistema Nacional)
}>();

const unidad = (recurso: string) => props.unidades[recurso] ?? "unidad";

// "litro" -> "litros", "ración" -> "raciones" (el catálogo nacional tiene las unidades en singular)
function enPlural(u: string, cantidad: number) {
  if (cantidad === 1 || u.endsWith("s")) return u;
  if (u.endsWith("ón")) return u.slice(0, -2) + "ones";
  return u + "s";
}
const cantidadCon = (cantidad: number, recurso: string) =>
  `${formatoNumero(cantidad)} ${enPlural(unidad(recurso), cantidad)}`;

// ---- Suministros movilizados por mes: un recurso a la vez (no se suman litros con personas) ----
const recursos = computed(() => props.datos.movilizadosPorRecurso.map(r => r.recurso));
const recursoElegido = ref("");
watch(recursos, lista => {
  if (!lista.includes(recursoElegido.value)) recursoElegido.value = lista[0] ?? "";
}, { immediate: true });

const meses = computed(() => [...new Set(props.datos.movilizadosPorMes.map(m => m.mes))].sort());
const etiquetaMes = (mes: string) => {
  const [anio, m] = mes.split("-").map(Number);
  const texto = new Date(anio!, m! - 1, 1).toLocaleDateString("es-AR", { month: "short", year: "numeric" });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
};

const graficoMes = computed<ChartData<"bar">>(() => ({
  labels: meses.value.map(etiquetaMes),
  datasets: [{
    label: recursoElegido.value,
    data: meses.value.map(mes => props.datos.movilizadosPorMes
      .filter(m => m.mes === mes && m.recurso === recursoElegido.value)
      .reduce((s, m) => s + m.unidades, 0)),
    backgroundColor: SERIE[0],
    ...BARRA
  }]
}));
const opcionesMes = computed(() => {
  // Se lee acá (no solo en el callback) para que el gráfico se rehaga cuando llegan las unidades del SN
  const recurso = recursoElegido.value;
  void unidad(recurso);
  return opcionesBarras({ formatoValor: v => cantidadCon(v, recurso) });
});

// ---- Lotes adjudicados por recurso (conteo comparable entre recursos) ----
const graficoRecursos = computed<ChartData<"bar">>(() => ({
  labels: props.datos.movilizadosPorRecurso.map(r => r.recurso),
  datasets: [{
    label: "Lotes adjudicados",
    data: props.datos.movilizadosPorRecurso.map(r => r.lotes),
    backgroundColor: SERIE[0],
    ...BARRA
  }]
}));
const opcionesRecursos = computed(() => {
  const base = opcionesBarras({ horizontal: true });
  base.plugins!.tooltip!.callbacks = {
    afterLabel: ctx => {
      const r = props.datos.movilizadosPorRecurso[ctx.dataIndex]!;
      return `${cantidadCon(r.unidades, r.recurso)} movilizados`;
    }
  };
  return base;
});

// ---- Participación de las ONGs: individual vs. consorcio (series con color fijo) ----
const graficoOngs = computed<ChartData<"bar">>(() => ({
  labels: props.datos.participacionOngs.map(o => o.ong),
  datasets: [
    { label: "Lotes individuales", data: props.datos.participacionOngs.map(o => o.lotesIndividuales), backgroundColor: SERIE[0], ...BARRA },
    { label: "Lotes en consorcio", data: props.datos.participacionOngs.map(o => o.lotesEnConsorcio), backgroundColor: SERIE[1], ...BARRA }
  ]
}));
const opcionesOngs = opcionesBarras({ horizontal: true, leyenda: true });

// ---- Conteos simples ----
function graficoConteo(conteos: Conteo[], titulo: string): ChartData<"bar"> {
  return {
    labels: conteos.map(c => etiqueta(c.clave)),
    datasets: [{ label: titulo, data: conteos.map(c => c.cantidad), backgroundColor: SERIE[0], ...BARRA }]
  };
}
const graficoEstados = computed(() => graficoConteo(props.datos.porEstado, "Emergencias"));
const graficoValidaciones = computed(() => graficoConteo(props.datos.validaciones, "Ofertas"));
const opcionesConteo = opcionesBarras({ horizontal: true });

const tipoYGravedad = computed(() => [
  ...props.datos.porTipo.map(c => ({ dimension: "Tipo de desastre", ...c })),
  ...props.datos.porGravedad.map(c => ({ dimension: "Gravedad", ...c }))
]);

// ---- Columnas de tablas y CSV ----
type MovMes = DashboardLocal["movilizadosPorMes"][number];
const columnasMes: Columna<MovMes>[] = [
  { titulo: "Mes", valor: m => m.mes },
  { titulo: "Recurso", valor: m => m.recurso },
  { titulo: "Cantidad", valor: m => m.unidades },
  { titulo: "Unidad", valor: m => unidad(m.recurso) }
];
type MovRecurso = DashboardLocal["movilizadosPorRecurso"][number];
const columnasRecursos: Columna<MovRecurso>[] = [
  { titulo: "Recurso", valor: r => r.recurso },
  { titulo: "Lotes adjudicados", valor: r => r.lotes },
  { titulo: "Cantidad movilizada", valor: r => r.unidades },
  { titulo: "Unidad", valor: r => unidad(r.recurso) }
];
type Ong = DashboardLocal["participacionOngs"][number];
const columnasOngs: Columna<Ong>[] = [
  { titulo: "ONG", valor: o => o.ong },
  { titulo: "Lotes individuales", valor: o => o.lotesIndividuales },
  { titulo: "Lotes en consorcio", valor: o => o.lotesEnConsorcio },
  { titulo: "Unidades aportadas", valor: o => o.unidadesMovilizadas },
  { titulo: "Ofertas presentadas", valor: o => o.ofertasPresentadas }
];
const columnasConteo = (titulo: string): Columna<Conteo>[] => [
  { titulo, valor: c => etiqueta(c.clave), crudo: c => c.clave },
  { titulo: "Cantidad", valor: c => c.cantidad }
];
const columnasTipo: Columna<{ dimension: string } & Conteo>[] = [
  { titulo: "Dimensión", valor: c => c.dimension },
  { titulo: "Valor", valor: c => etiqueta(c.clave), crudo: c => c.clave },
  { titulo: "Emergencias", valor: c => c.cantidad }
];
type Proyecto = DashboardLocal["proyectosFinalizados"][number];
const columnasProyectos: Columna<Proyecto>[] = [
  { titulo: "Emergencia", valor: p => `#${p.emergenciaId} · ${p.zona}` },
  { titulo: "Tipo", valor: p => etiqueta(p.tipo), crudo: p => p.tipo },
  { titulo: "Gravedad", valor: p => etiqueta(p.gravedad), crudo: p => p.gravedad },
  { titulo: "Registro", valor: p => fechaHora(p.fechaRegistro), crudo: p => p.fechaRegistro },
  { titulo: "Cierre", valor: p => fechaHora(p.fechaCierre), crudo: p => p.fechaCierre },
  { titulo: "Duración", tituloCsv: "Duración (h)", valor: p => duracion(p.duracionHoras === null ? null : p.duracionHoras * 60), crudo: p => redondear(p.duracionHoras) },
  { titulo: "Lotes", valor: p => p.lotes },
  { titulo: "ONGs", valor: p => p.ongs },
  { titulo: "Cobertura", valor: p => p.coberturaParcial ? "Parcial" : "Total" }
];
</script>

<template>
  <div class="dash-kpis">
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Emergencias</span>
      <strong>{{ formatoNumero(datos.resumen.emergencias) }}</strong>
      <span class="dash-kpi-detalle">{{ datos.resumen.enCurso }} en curso · {{ datos.resumen.cerradas }} cerradas</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Lotes adjudicados</span>
      <strong>{{ formatoNumero(datos.resumen.lotesAdjudicados) }}</strong>
      <span class="dash-kpi-detalle">{{ datos.movilizadosPorRecurso.length }} tipos de recurso movilizados</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Resueltos por consorcio</span>
      <strong>{{ porcentaje(datos.resumen.porcentajeConsorcio) }}</strong>
      <span class="dash-kpi-detalle">{{ datos.consorcios.porConsorcio }} de {{ datos.consorcios.lotesAdjudicados }} lotes adjudicados</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Cobertura de lotes</span>
      <strong>{{ porcentaje(datos.resumen.porcentajeCobertura) }}</strong>
      <span class="dash-kpi-detalle">en emergencias ya adjudicadas</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Duración de un proyecto</span>
      <strong>{{ duracion(datos.resumen.duracionPromedioHoras === null ? null : datos.resumen.duracionPromedioHoras * 60) }}</strong>
      <span class="dash-kpi-detalle">promedio de registro a cierre</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Ofertas registradas</span>
      <strong>{{ formatoNumero(datos.resumen.ofertas) }}</strong>
      <span class="dash-kpi-detalle">{{ formatoNumero(datos.resumen.promedioVersiones, 1) }} versiones en promedio</span>
    </div>
  </div>

  <div class="dash-grilla">
    <TarjetaDatos titulo="Suministros movilizados por mes" archivo="suministros-por-mes"
      descripcion="Cantidades adjudicadas, por mes de adjudicación. Elegí el recurso."
      :filas="datos.movilizadosPorMes" :columnas="columnasMes">
      <template #acciones>
        <label class="dash-selector">
          <span class="visualmente-oculto">Recurso</span>
          <select v-model="recursoElegido">
            <option v-for="r in recursos" :key="r" :value="r">{{ r }}</option>
          </select>
        </label>
      </template>
      <Grafico :datos="graficoMes" :opciones="opcionesMes"
        :descripcion="`Barras con la cantidad de ${recursoElegido} movilizada en cada mes`" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Lotes adjudicados por recurso" archivo="lotes-por-recurso"
      descripcion="Pasá el mouse por una barra para ver la cantidad movilizada."
      :filas="datos.movilizadosPorRecurso" :columnas="columnasRecursos">
      <Grafico :datos="graficoRecursos" :opciones="opcionesRecursos"
        descripcion="Barras horizontales con la cantidad de lotes adjudicados de cada recurso" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Participación de las ONGs" archivo="participacion-ongs"
      descripcion="Lotes adjudicados en los que participa cada ONG, sola o en consorcio."
      :filas="datos.participacionOngs" :columnas="columnasOngs">
      <Grafico :datos="graficoOngs" :opciones="opcionesOngs"
        descripcion="Barras agrupadas por ONG: lotes individuales y lotes en consorcio" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Emergencias por estado" archivo="emergencias-por-estado"
      :filas="datos.resumen.emergencias ? datos.porEstado : []" :columnas="columnasConteo('Estado')">
      <Grafico :datos="graficoEstados" :opciones="opcionesConteo"
        descripcion="Barras horizontales con la cantidad de emergencias en cada estado" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Resultado de la validación externa" archivo="validaciones"
      descripcion="Perfil que asignó el Sistema Nacional a cada oferta."
      :filas="datos.resumen.ofertas ? datos.validaciones : []" :columnas="columnasConteo('Resultado')">
      <Grafico :datos="graficoValidaciones" :opciones="opcionesConteo"
        descripcion="Barras horizontales con la cantidad de ofertas por resultado de validación" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Emergencias por tipo y gravedad" archivo="emergencias-por-tipo" solo-tabla
      :filas="datos.resumen.emergencias ? tipoYGravedad : []" :columnas="columnasTipo" />

    <TarjetaDatos titulo="Proyectos finalizados" archivo="proyectos-finalizados" solo-tabla ancha
      descripcion="Emergencias cerradas: el Sistema Nacional liberó todos sus recursos."
      :filas="datos.proyectosFinalizados" :columnas="columnasProyectos"
      vacio="Todavía no hay proyectos finalizados en este período." />
  </div>
</template>
