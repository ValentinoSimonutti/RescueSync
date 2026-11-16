import {
  BarController, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip,
  type ChartOptions, type Scale
} from "chart.js";

// Solo se registran las piezas que usa el dashboard (barras): el bundle queda más chico
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

Chart.defaults.font.family = "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";
Chart.defaults.font.size = 12;
Chart.defaults.color = "#626b7e";

// Paleta categórica (orden fijo: el color sigue a la serie, nunca a su posición en el ranking).
// Validada para daltonismo con el validador de la guía de visualización.
export const SERIE = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300"] as const;
export const GRILLA = "#eceef3";
const TEXTO_FUERTE = "#10172b";

// Barras finas con el extremo de datos redondeado
export const BARRA = { borderRadius: 4, maxBarThickness: 28, borderSkipped: "start" as const };

// Opciones comunes. horizontal = barras acostadas (categorías largas en el eje Y)
export function opcionesBarras(opts: {
  horizontal?: boolean;
  apiladas?: boolean;
  leyenda?: boolean;
  formatoValor?: (v: number) => string;
  maximo?: number;
}): ChartOptions<"bar"> {
  const ejeValor = {
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
      // En pantallas angostas se acortan las etiquetas largas (el tooltip muestra el nombre completo)
      callback(this: Scale, valor: string | number) {
        const texto = this.getLabelForValue(Number(valor));
        return this.chart.width < 420 && texto.length > 22 ? texto.slice(0, 21) + "…" : texto;
      }
    }
  };
  return {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: opts.horizontal ? "y" : "x",
    animation: { duration: 250 },
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
