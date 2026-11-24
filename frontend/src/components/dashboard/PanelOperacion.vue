<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { ChartData } from "chart.js";
import type { Conteo, DashboardLocal } from "../../services/dashboardService";
import { duracion, etiqueta, fechaHora, formatoNumero, porcentaje, redondear, type Columna } from "../../utils/exportar";
import { BARRA, ESTADO, SERIE, altoBarras, opcionesBarras } from "../../utils/graficos";
import Grafico from "./Grafico.vue";
import TarjetaDatos from "./TarjetaDatos.vue";

// Panel con los indicadores propios (base de datos local de la app)
const props = defineProps<{
  datos: DashboardLocal;
  unidades: Record<string, string>;   // nombre del recurso -> unidad (sale del catálogo local)
}>();

const unidad = (recurso: string) => props.unidades[recurso] ?? "unidad";

// "litro" -> "litros", "ración" -> "raciones" (el catálogo tiene las unidades en singular)
function enPlural(u: string, cantidad: number) {
  if (cantidad === 1 || u.endsWith("s")) return u;
  if (u.endsWith("ón")) return u.slice(0, -2) + "ones";
  return u + "s";
}
const cantidadCon = (cantidad: number, recurso: string) =>
  `${formatoNumero(cantidad)} ${enPlural(unidad(recurso), cantidad)}`;

const etiquetaMes = (mes: string) => {
  const [anio, m] = mes.split("-").map(Number);
  const texto = new Date(anio!, m! - 1, 1).toLocaleDateString("es-AR", { month: "long", year: "numeric" });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
};
const meses = computed(() => [...new Set(props.datos.movilizadosPorMes.map(m => m.mes))].sort());

// ---- Suministros movilizados en el mes (tabla: cada recurso con su unidad, no se suman litros con personas) ----
const mesElegido = ref("");
watch(meses, lista => {
  if (!lista.includes(mesElegido.value)) mesElegido.value = lista.at(-1) ?? "";
}, { immediate: true });

const suministrosDelMes = computed(() => props.datos.movilizadosPorMes
  .filter(m => m.mes === mesElegido.value)
  .sort((a, b) => a.recurso.localeCompare(b.recurso, "es")));

// ---- Evolución mensual: un recurso a la vez ----
const recursos = computed(() => props.datos.movilizadosPorRecurso.map(r => r.recurso).sort((a, b) => a.localeCompare(b, "es")));
const recursoElegido = ref("");
watch(recursos, lista => {
  if (!lista.includes(recursoElegido.value)) recursoElegido.value = lista[0] ?? "";
}, { immediate: true });

const cantidadEnMes = (mes: string) => props.datos.movilizadosPorMes
  .filter(m => m.mes === mes && m.recurso === recursoElegido.value)
  .reduce((s, m) => s + m.unidades, 0);

const graficoMes = computed<ChartData<"bar">>(() => ({
  labels: meses.value.map(etiquetaMes),
  datasets: [{
    label: recursoElegido.value,
    data: meses.value.map(cantidadEnMes),
    backgroundColor: SERIE[0],
    ...BARRA,
    maxBarThickness: 56
  }]
}));
const opcionesMes = computed(() => {
  // Se lee acá (no solo en el callback) para que el gráfico se rehaga cuando llegan las unidades
  const recurso = recursoElegido.value;
  void unidad(recurso);
  return opcionesBarras({ formatoValor: v => cantidadCon(v, recurso), etiquetas: true });
});
const etiquetaMesValor = (v: number) => cantidadCon(v, recursoElegido.value);

// ---- Participación de las ONGs: lotes individuales + en consorcio (se apilan: suman su participación) ----
const graficoOngs = computed<ChartData<"bar">>(() => ({
  labels: props.datos.participacionOngs.map(o => o.ong),
  datasets: [
    { label: "Lotes individuales", data: props.datos.participacionOngs.map(o => o.lotesIndividuales), backgroundColor: SERIE[0], ...BARRA, borderRadius: 3, borderColor: "#fff", borderWidth: { right: 2 } },
    { label: "Lotes en consorcio", data: props.datos.participacionOngs.map(o => o.lotesEnConsorcio), backgroundColor: SERIE[1], ...BARRA, borderRadius: 3 }
  ]
}));
const opcionesOngs = opcionesBarras({ horizontal: true, apiladas: true, leyenda: true, etiquetas: true });
const etiquetaLotes = (v: number) => `${formatoNumero(v)} ${v === 1 ? "lote" : "lotes"}`;

// ---- Conteos simples ----
function graficoConteo(conteos: Conteo[], titulo: string, colores?: string[]): ChartData<"bar"> {
  return {
    labels: conteos.map(c => etiqueta(c.clave)),
    datasets: [{ label: titulo, data: conteos.map(c => c.cantidad), backgroundColor: colores ?? SERIE[0], ...BARRA }]
  };
}
const graficoEstados = computed(() => graficoConteo(props.datos.porEstado, "Emergencias"));
const graficoTipos = computed(() => graficoConteo(props.datos.porTipo, "Emergencias"));
const graficoGravedad = computed(() => graficoConteo(props.datos.porGravedad, "Emergencias"));

// El resultado de la validación es un estado: verde, amarillo, rojo y gris (siempre con su nombre al lado)
const COLOR_VALIDACION: Record<string, string> = {
  PRINCIPAL: ESTADO.bien,
  APOYO_SECUNDARIO: ESTADO.alerta,
  SIN_HABILITACION_VIGENTE: ESTADO.critico,
  SIN_VALIDAR: ESTADO.neutro
};
const graficoValidaciones = computed(() => graficoConteo(props.datos.validaciones, "Ofertas",
  props.datos.validaciones.map(c => COLOR_VALIDACION[c.clave] ?? SERIE[0])));

const opcionesConteo = opcionesBarras({ horizontal: true, etiquetas: true });
const etiquetaCantidad = (v: number) => formatoNumero(v);

const tipoYGravedad = computed(() => [
  ...props.datos.porTipo.map(c => ({ dimension: "Tipo de desastre", ...c })),
  ...props.datos.porGravedad.map(c => ({ dimension: "Gravedad", ...c }))
]);

// ---- Columnas de tablas y CSV ----
type MovMes = DashboardLocal["movilizadosPorMes"][number];
const columnasSuministros: Columna<MovMes>[] = [
  { titulo: "Recurso", valor: m => m.recurso },
  { titulo: "Cantidad movilizada", tituloCsv: "Cantidad", valor: m => formatoNumero(m.unidades), crudo: m => m.unidades, alinear: "derecha" },
  { titulo: "Unidad", valor: m => enPlural(unidad(m.recurso), m.unidades), crudo: m => unidad(m.recurso) }
];
const columnasMes: Columna<MovMes>[] = [
  { titulo: "Mes", valor: m => etiquetaMes(m.mes), crudo: m => m.mes },
  { titulo: "Recurso", valor: m => m.recurso },
  { titulo: "Cantidad", valor: m => formatoNumero(m.unidades), crudo: m => m.unidades, alinear: "derecha" },
  { titulo: "Unidad", valor: m => unidad(m.recurso) }
];
type Ong = DashboardLocal["participacionOngs"][number];
const columnasOngs: Columna<Ong>[] = [
  { titulo: "ONG", valor: o => o.ong },
  { titulo: "Lotes individuales", valor: o => o.lotesIndividuales, alinear: "derecha" },
  { titulo: "Lotes en consorcio", valor: o => o.lotesEnConsorcio, alinear: "derecha" },
  { titulo: "Ofertas presentadas", valor: o => o.ofertasPresentadas, alinear: "derecha" }
];
const columnasConteo = (titulo: string): Columna<Conteo>[] => [
  { titulo, valor: c => etiqueta(c.clave), crudo: c => c.clave },
  { titulo: "Cantidad", valor: c => c.cantidad, alinear: "derecha" }
];
const columnasTipo: Columna<{ dimension: string } & Conteo>[] = [
  { titulo: "Dimensión", valor: c => c.dimension },
  { titulo: "Valor", valor: c => etiqueta(c.clave), crudo: c => c.clave },
  { titulo: "Emergencias", valor: c => c.cantidad, alinear: "derecha" }
];
type Proyecto = DashboardLocal["proyectosFinalizados"][number];
const columnasProyectos: Columna<Proyecto>[] = [
  { titulo: "Emergencia", valor: p => `#${p.emergenciaId} · ${p.zona}` },
  { titulo: "Tipo", valor: p => etiqueta(p.tipo), crudo: p => p.tipo },
  { titulo: "Gravedad", valor: p => etiqueta(p.gravedad), crudo: p => p.gravedad },
  { titulo: "Registro", valor: p => fechaHora(p.fechaRegistro), crudo: p => p.fechaRegistro },
  { titulo: "Cierre", valor: p => fechaHora(p.fechaCierre), crudo: p => p.fechaCierre },
  { titulo: "Duración", tituloCsv: "Duración (h)", valor: p => duracion(p.duracionHoras === null ? null : p.duracionHoras * 60), crudo: p => redondear(p.duracionHoras), alinear: "derecha" },
  { titulo: "Lotes", valor: p => p.lotes, alinear: "derecha" },
  { titulo: "ONGs", valor: p => p.ongs, alinear: "derecha" },
  { titulo: "Cobertura", valor: p => p.coberturaParcial ? "Parcial" : "Total", tono: p => p.coberturaParcial ? "alerta" : "bien" }
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
      <span class="dash-kpi-detalle">{{ datos.movilizadosPorRecurso.length }} tipos de recurso</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Por consorcio</span>
      <strong>{{ porcentaje(datos.resumen.porcentajeConsorcio) }}</strong>
      <span class="dash-kpi-detalle">{{ datos.consorcios.porConsorcio }} de {{ datos.consorcios.lotesAdjudicados }} lotes resueltos así</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Cobertura</span>
      <strong>{{ porcentaje(datos.resumen.porcentajeCobertura) }}</strong>
      <span class="dash-kpi-detalle">de los lotes en emergencias adjudicadas</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Duración promedio</span>
      <strong>{{ duracion(datos.resumen.duracionPromedioHoras === null ? null : datos.resumen.duracionPromedioHoras * 60) }}</strong>
      <span class="dash-kpi-detalle">de un proyecto, de registro a cierre</span>
    </div>
    <div class="dash-kpi">
      <span class="dash-kpi-titulo">Ofertas</span>
      <strong>{{ formatoNumero(datos.resumen.ofertas) }}</strong>
      <span class="dash-kpi-detalle">registradas · {{ formatoNumero(datos.resumen.promedioVersiones, 1) }} versiones en promedio</span>
    </div>
  </div>

  <div class="dash-grilla">
    <TarjetaDatos titulo="Suministros movilizados en el mes" archivo="suministros-del-mes" solo-tabla
      descripcion="Lo adjudicado en el mes, por recurso y con su unidad."
      :filas="suministrosDelMes" :columnas="columnasSuministros"
      vacio="Todavía no hay recursos adjudicados en este período.">
      <template #filtros>
        <label class="dash-selector">
          <span>Mes</span>
          <select v-model="mesElegido">
            <option v-for="m in meses" :key="m" :value="m">{{ etiquetaMes(m) }}</option>
          </select>
        </label>
      </template>
    </TarjetaDatos>

    <TarjetaDatos titulo="Evolución mensual" archivo="suministros-por-mes"
      descripcion="Cantidad adjudicada de un recurso en cada mes."
      :filas="datos.movilizadosPorMes" :columnas="columnasMes"
      vacio="Todavía no hay recursos adjudicados en este período.">
      <template #filtros>
        <label class="dash-selector">
          <span>Recurso</span>
          <select v-model="recursoElegido">
            <option v-for="r in recursos" :key="r" :value="r">{{ r }}</option>
          </select>
        </label>
      </template>
      <Grafico :datos="graficoMes" :opciones="opcionesMes" :alto="240" :etiqueta="etiquetaMesValor"
        :descripcion="`Barras con la cantidad de ${recursoElegido} movilizada en cada mes`" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Participación de las ONGs" archivo="participacion-ongs"
      descripcion="Lotes adjudicados en los que participa cada ONG, sola o en consorcio."
      :filas="datos.participacionOngs" :columnas="columnasOngs">
      <Grafico :datos="graficoOngs" :opciones="opcionesOngs" :etiqueta="etiquetaLotes"
        :alto="altoBarras(datos.participacionOngs.length, 36)"
        descripcion="Barras apiladas por ONG: lotes individuales y lotes en consorcio" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Resultado de la validación externa" archivo="validaciones"
      descripcion="Perfil que asignó el Sistema Nacional a cada oferta."
      :filas="datos.resumen.ofertas ? datos.validaciones : []" :columnas="columnasConteo('Resultado')">
      <Grafico :datos="graficoValidaciones" :opciones="opcionesConteo" :etiqueta="etiquetaCantidad"
        :alto="altoBarras(datos.validaciones.length)"
        descripcion="Barras horizontales con la cantidad de ofertas por resultado de validación" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Emergencias por estado" archivo="emergencias-por-estado"
      descripcion="En qué parte del proceso está cada emergencia."
      :filas="datos.resumen.emergencias ? datos.porEstado : []" :columnas="columnasConteo('Estado')">
      <Grafico :datos="graficoEstados" :opciones="opcionesConteo" :etiqueta="etiquetaCantidad"
        :alto="altoBarras(datos.porEstado.length)"
        descripcion="Barras horizontales con la cantidad de emergencias en cada estado" />
    </TarjetaDatos>

    <TarjetaDatos titulo="Emergencias por tipo y gravedad" archivo="emergencias-por-tipo"
      descripcion="Qué desastres se registraron y con qué gravedad."
      :filas="datos.resumen.emergencias ? tipoYGravedad : []" :columnas="columnasTipo">
      <div class="dash-mini-graficos">
        <section>
          <h4>Tipo de desastre</h4>
          <Grafico :datos="graficoTipos" :opciones="opcionesConteo" :etiqueta="etiquetaCantidad"
            :alto="altoBarras(datos.porTipo.length)"
            descripcion="Barras horizontales con la cantidad de emergencias por tipo de desastre" />
        </section>
        <section>
          <h4>Gravedad</h4>
          <Grafico :datos="graficoGravedad" :opciones="opcionesConteo" :etiqueta="etiquetaCantidad"
            :alto="altoBarras(datos.porGravedad.length)"
            descripcion="Barras horizontales con la cantidad de emergencias por gravedad" />
        </section>
      </div>
    </TarjetaDatos>

    <TarjetaDatos titulo="Proyectos finalizados" archivo="proyectos-finalizados" solo-tabla :ocupa="12"
      descripcion="Emergencias cerradas: el Sistema Nacional liberó todos sus recursos."
      :filas="datos.proyectosFinalizados" :columnas="columnasProyectos"
      vacio="Todavía no hay proyectos finalizados en este período." />
  </div>
</template>