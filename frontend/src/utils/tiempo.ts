import type { Emergencia } from "../services/emergenciaService";

// El backend manda LocalDateTime sin zona (hora de Argentina). Si algún día trae offset, se respeta.
export function cierreDe(emergencia: Emergencia): number {
  const fecha = emergencia.fechaCierreOfertas;
  if (!fecha) return NaN;
  return Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/i.test(fecha) ? fecha : `${fecha}-03:00`);
}

// "1 h 3 min 20 s" hasta el cierre de la ventana de ofertas
export function tiempoRestante(emergencia: Emergencia, ahora: number): string {
  const fin = cierreDe(emergencia);
  if (!Number.isFinite(fin)) return "Cierre no disponible";
  const segundos = Math.max(0, Math.ceil((fin - ahora) / 1000));
  const horas = Math.floor(segundos / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  return `${horas ? `${horas} h ` : ""}${minutos} min ${segundos % 60} s`;
}

// "INUNDACION" -> "Inundacion"
export function capitalizar(valor: string): string {
  return valor.toLocaleLowerCase("es").replace(/^./u, letra => letra.toLocaleUpperCase("es"));
}

// Fecha legible para el historial
export function fechaCorta(fecha: string): string {
  const d = new Date(fecha);
  return Number.isNaN(d.getTime()) ? fecha : d.toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" });
}
