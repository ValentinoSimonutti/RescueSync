// Exportación de datos estructurados del dashboard (CSV y JSON), sin librerías.

export interface Columna<T> {
  titulo: string;
  valor: (fila: T) => string | number | boolean | null | undefined;
  // Lo que va al CSV si difiere de lo que se muestra (ej: minutos como número en vez de "3 h 20 min")
  crudo?: (fila: T) => string | number | boolean | null | undefined;
  tituloCsv?: string;   // encabezado del CSV si cambia la unidad (ej: "Promedio (min)")
}

// CSV con separador ";" y BOM: Excel en español lo abre con columnas y acentos bien
export function aCsv<T>(filas: T[], columnas: Columna<T>[]): string {
  const celda = (v: unknown) => {
    if (v === null || v === undefined) return "";
    const texto = typeof v === "number" ? String(v).replace(".", ",") : String(v);
    return /[";\n\r]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto;
  };
  const lineas = [
    columnas.map(c => celda(c.tituloCsv ?? c.titulo)).join(";"),
    ...filas.map(f => columnas.map(c => celda((c.crudo ?? c.valor)(f))).join(";"))
  ];
  return "﻿" + lineas.join("\r\n");
}

// Redondeo para el CSV (los minutos vienen con muchos decimales)
export function redondear(n: number | null | undefined, decimales = 2): number | null {
  if (n === null || n === undefined) return null;
  const f = 10 ** decimales;
  return Math.round(n * f) / f;
}

export function descargarCsv<T>(nombre: string, filas: T[], columnas: Columna<T>[]) {
  descargar(`${nombre}.csv`, aCsv(filas, columnas), "text/csv;charset=utf-8");
}

export function descargarJson(nombre: string, datos: unknown) {
  descargar(`${nombre}.json`, JSON.stringify(datos, null, 2), "application/json");
}

function descargar(archivo: string, contenido: string, tipo: string) {
  const url = URL.createObjectURL(new Blob([contenido], { type: tipo }));
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = archivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}

// ---- Formatos ------------------------------------------------------------------

// Minutos -> "45 s", "12 min", "3 h 20 min", "2,5 días"
export function duracion(min: number | null | undefined): string {
  if (min === null || min === undefined) return "—";
  if (min < 1) return `${Math.round(min * 60)} s`;
  if (min < 60) return `${formatoNumero(min, min < 10 ? 1 : 0)} min`;
  const horas = min / 60;
  if (horas < 24) {
    const h = Math.floor(horas);
    const m = Math.round(min - h * 60);
    return m ? `${h} h ${m} min` : `${h} h`;
  }
  return `${formatoNumero(horas / 24, 1)} días`;
}

export function formatoNumero(n: number | null | undefined, decimales = 0): string {
  if (n === null || n === undefined) return "—";
  return n.toLocaleString("es-AR", { maximumFractionDigits: decimales });
}

export function porcentaje(n: number | null | undefined): string {
  return n === null || n === undefined ? "—" : `${formatoNumero(n, 1)} %`;
}

export function fechaHora(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" });
}

// "EN_EJECUCION" -> "En ejecución"
const ETIQUETAS: Record<string, string> = {
  REGISTRADA: "Registrada",
  EN_CONVOCATORIA: "En convocatoria",
  EN_EVALUACION: "En evaluación",
  ADJUDICADA: "Adjudicada",
  EN_EJECUCION: "En ejecución",
  CERRADA: "Cerrada",
  INUNDACION: "Inundación",
  INCENDIO: "Incendio",
  TERREMOTO: "Terremoto",
  ALTA: "Alta",
  MEDIA: "Media",
  BAJA: "Baja",
  PRINCIPAL: "Principal",
  APOYO_SECUNDARIO: "Apoyo secundario",
  SIN_HABILITACION_VIGENTE: "Sin habilitación vigente",
  SIN_VALIDAR: "Sin validar (no llegó a evaluarse)",
  HABILITADA: "Habilitada",
  PENDIENTE: "Pendiente",
  SUSPENDIDA: "Suspendida",
  SIN_DATO: "Sin dato"
};

export function etiqueta(clave: string | null | undefined): string {
  if (!clave) return "—";
  return ETIQUETAS[clave] ?? clave.charAt(0) + clave.slice(1).toLowerCase().replace(/_/g, " ");
}
