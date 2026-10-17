import { get, post } from "./api";
import type { Emergencia } from "./emergenciaService";
import type { Oferta } from "./ofertaService";

// Un lote con las ofertas que el municipio puede elegir (validadas y que cubren el lote)
export interface LoteAdjudicable {
  loteId: number;
  recurso: string;
  recursoCodigo: string | null;
  requerido: number;
  ofertas: Oferta[];
}

export interface Adjudicacion {
  emergencia: Emergencia;
  lotes: LoteAdjudicable[];
}

export interface Seleccion {
  loteId: number;
  ofertaId: number;
}

// GET /api/emergencias/{id}/adjudicacion
export function opcionesAdjudicacion(emergenciaId: number): Promise<Adjudicacion> {
  return get<Adjudicacion>(`/emergencias/${emergenciaId}/adjudicacion`);
}

// POST /api/emergencias/{id}/adjudicacion -> adjudica una oferta por lote y completa la tarea en Bonita
export function adjudicar(emergenciaId: number, selecciones: Seleccion[]): Promise<Emergencia> {
  return post<Emergencia>(`/emergencias/${emergenciaId}/adjudicacion`, { selecciones });
}
