import { get, post } from "./api";
import type { Emergencia } from "./emergenciaService";
import type { RolValidacion } from "./ofertaService";

// Una ONG dentro de la oferta adjudicada de un lote
export interface Participante {
  ongId: number;
  ongNombre: string;
  cantidad: number;
  rol: RolValidacion | null;
  fechaFinalizacion: string | null;   // null = todavía está trabajando
}

// La oferta adjudicada de un lote
export interface Asignacion {
  ofertaId: number;
  loteId: number;
  recurso: string;
  cantidad: number;
  consorcio: boolean;
  ongs: Participante[];
}

// DespliegueDTO en Java
export interface Despliegue {
  emergencia: Emergencia;
  asignaciones: Asignacion[];
  ongsTotal: number;
  ongsFinalizadas: number;
}

// GET /api/emergencias/despliegues -> adjudicadas, en ejecución y cerradas (coordinador)
export function despliegues(): Promise<Despliegue[]> {
  return get<Despliegue[]>("/emergencias/despliegues");
}

// GET /api/emergencias/{id}/despliegue
export function despliegueDe(emergenciaId: number): Promise<Despliegue> {
  return get<Despliegue>(`/emergencias/${emergenciaId}/despliegue`);
}

// POST /api/emergencias/{id}/despliegue/inicio -> completa "Monitorear estado de despliegue"
export function iniciarDespliegue(emergenciaId: number): Promise<Despliegue> {
  return post<Despliegue>(`/emergencias/${emergenciaId}/despliegue/inicio`);
}

// POST /api/emergencias/{id}/finalizacion -> la ONG logueada terminó sus actividades
export function finalizarActividades(emergenciaId: number): Promise<Despliegue> {
  return post<Despliegue>(`/emergencias/${emergenciaId}/finalizacion`);
}
