import {
  BarController, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip,
  type ChartOptions, type Plugin, type Scale
} from "chart.js";

// Solo se registran las piezas que usa el dashboard (barras): el bundle queda más chico
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

Chart.defaults.font.family = "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";
Chart.defaults.font.size = 12;
Chart.defaults.color = "#626b7e";

// Paleta categórica (orden fijo: el color sigue a la serie, nunca a su posición en el ranking).
// Validada para daltonismo con el validador de la guía de visualización.
export const SERIE = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300"] as const;
// Colores de estado (reservados: nunca se usan para una serie). Siempre van con su etiqueta.
export const ESTADO = { bien: "#0ca30c", alerta: "#fab219", critico: "#d03b3b", neutro: "#b7bcc8" } as const;
export const SERIE_APAGADA = "#c9d8ee";   // barra no elegida cuando se filtra por una
export const GRILLA = "#eceef3";
const TEXTO_FUERTE = "#10172b";

// Barras finas con el extremo de datos redondeado
export const BARRA = { borderRadius: 4, maxBarThickness: 28, borderSkipped: "start" as const };

// Alto de un gráfico de barras horizontales según cuántas categorías tiene
export const altoBarras = (categorias: number, extra = 0) => Math.max(140, 24 + categorias * 38 + extra);

// Escribe el valor al final de cada barra (en las apiladas, el total al final de la pila).
// Con pocas barras, leer el número directo es más rápido que ir y volver del eje.
export function pluginEtiquetas(obtener: () => ((valor: number, indice: number) => string) | undefined): Plugin<"bar"> {
  return {
    id: "etiquetasValor",
    afterDatasetsDraw(chart) {
      const formato = obtener();
      if (!formato) return;
      const visibles = chart.data.datasets.map((_, i) => i).filter(i => chart.isDatasetVisible(i));
      if (!visibles.length) return;
      const horizontal = chart.options.indexAxis === "y";
      const meta = chart.getDatasetMeta(visibles[visibles.length - 1]!);
      const { ctx } = chart;
      ctx.save();
      ctx.font = `600 12px ${Chart.defaults.font.family}`;
      ctx.fillStyle = TEXTO_FUERTE;
      meta.data.forEach((barra, i) => {
        const total = visibles.reduce((s, d) => s + (Number(chart.data.datasets[d]!.data[i]) || 0), 0);
        const texto = formato(total, i);
        if (!texto) return;
        const { x, y } = barra.getProps(["x", "y"], true) as { x: number; y: number };
        if (horizontal) {
          ctx.textAlign = "left";
          ctx.textBaseline = "middle";
          ctx.fillText(texto, x + 8, y);
        } else {
          ctx.textAlign = "center";
          ctx.textBaseline = "bottom";
          ctx.fillText(texto, x, y - 6);
        }
      });
      ctx.restore();
    }
  };
}

// Opciones comunes. horizontal = barras acostadas (categorías largas en el eje Y)
// etiquetas = el valor va escrito en cada barra: se oculta el eje de valores (sería redundante)
export function opcionesBarras(opts: {
  horizontal?: boolean;
  apiladas?: boolean;
  leyenda?: boolean;
  formatoValor?: (v: number) => string;
  maximo?: number;
  etiquetas?: boolean;
}): ChartOptions<"bar"> {
  const ejeValor = {
    display: !opts.etiquetas,
    beginAtZero: true,
    max: opts.maximo,
    stacked: opts.apiladas,
    grid: { color: GRILLA, drawTicks: false },
    border: { display: false },
    ticks: {
      padding: 8,
      maxRotation: 0,
      autoSkipPadding: 14,
      precision: opts.formatoValor ? undefined : 0,
      callback: (v: string | number) => opts.formatoValor ? opts.formatoValor(Number(v)) : v
    }
  };
  const ejeCategoria = {
    stacked: opts.apiladas,
    grid: { display: false },
    border: { color: GRILLA },
    ticks: {
      color: TEXTO_FUERTE,
      padding: 6,
      // Las etiquetas largas se acortan con "…" (el tooltip muestra el nombre completo)
      callback(this: Scale, valor: string | number) {
        const texto = this.getLabelForValue(Number(valor));
        // Chart.js le da al eje de categorías cerca de un tercio del ancho: se corta para que entre entero
        const maximo = Math.max(10, Math.floor((this.chart.width * 0.34) / 6.6));
        return texto.length > maximo ? texto.slice(0, maximo - 1) + "…" : texto;
      }
    }
  };
  return {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: opts.horizontal ? "y" : "x",
    animation: { duration: 250 },
    // Lugar para el valor escrito al final de la barra
    layout: { padding: opts.etiquetas ? (opts.horizontal ? { right: 76 } : { top: 24 }) : 0 },
    interaction: { mode: "nearest", axis: opts.horizontal ? "y" : "x", intersect: false },
    scales: opts.horizontal ? { x: ejeValor, y: ejeCategoria } : { x: ejeCategoria, y: ejeValor },
    plugins: {
      legend: {
        display: opts.leyenda ?? false,
        position: "top",
        align: "start",
        labels: { usePointStyle: true, pointStyle: "rectRounded", boxWidth: 10, boxHeight: 10, color: TEXTO_FUERTE }
      },
      tooltip: {
        backgroundColor: "#10162a",
        padding: 10,
        cornerRadius: 8,
        boxPadding: 4,
        callbacks: opts.formatoValor
          ? { label: ctx => ` ${ctx.dataset.label ? ctx.dataset.label + ": " : ""}${opts.formatoValor!(Number(ctx.raw))}` }
          : undefined
      }
    }
  };
}