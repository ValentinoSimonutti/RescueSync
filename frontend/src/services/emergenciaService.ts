import { get, post } from "./api";

export interface CrearEmergenciaRequest {
  zonaAfectada: string;
  descripcion: string;
  tipoDesastre: string;
  gravedad: string;
}

export interface Emergencia {
  id: number;
  zonaAfectada: string;
  descripcion: string;
  tipoDesastre: string;
  gravedad: string;
  estado: string;
  fechaCierreOfertas: string | null;
  bonitaCaseId: number | null;   // case creado en Bonita para esta emergencia
  avisoCompromiso: string | null;   // por qué el Sistema Nacional rechazó el último compromiso
  coberturaParcial: boolean;        // el coordinador decidió seguir solo con los lotes cubiertos
}

export function crearEmergencia(
  datos: CrearEmergenciaRequest
): Promise<Emergencia> {
  return post<Emergencia>("/emergencias", datos);
}

// Un lote de necesidad: ej { recursoCodigo: "PARAMEDICO", cantidad: 5 }
// El código sale del catálogo nacional; el nombre lo completa el backend.
export interface NuevoLote {
  recursoCodigo: string;
  cantidad: number;
}

// Lo que manda el coordinador al publicar la convocatoria (PublicarLotesDTO en Java)
export interface PublicarLotesRequest {
  duracionMinutos: number;   // duración de la ventana de ofertas (el timer de Bonita)
  lotes: NuevoLote[];
}

// POST /api/emergencias/{id}/lotes -> guarda los lotes y completa "Generar lotes" en Bonita
export function publicarLotes(emergenciaId: number, datos: PublicarLotesRequest): Promise<Emergencia> {
  return post<Emergencia>(`/emergencias/${emergenciaId}/lotes`, datos);
}

// POST /api/emergencias/{id}/reapertura -> reabre desde cero (mismos recursos o menos) y completa
// "Evaluar curso de acción alternativo" en Bonita
export function reabrirConvocatoria(emergenciaId: number, datos: PublicarLotesRequest): Promise<Emergencia> {
  return post<Emergencia>(`/emergencias/${emergenciaId}/reapertura`, datos);
}

// POST /api/emergencias/{id}/cobertura-parcial -> el municipio adjudica solo los lotes cubiertos
export function continuarConCoberturaParcial(emergenciaId: number): Promise<Emergencia> {
  return post<Emergencia>(`/emergencias/${emergenciaId}/cobertura-parcial`);
}

// El backend filtra por el municipio de la sesión actual.
export function misEmergencias(): Promise<Emergencia[]> {
  return get<Emergencia[]>("/emergencias/mias");
}

export interface Lote {
  id: number;
  recurso: string;
  recursoCodigo: string | null;   // null en lotes creados antes del catálogo
  cantidad: number;
  emergenciaId: number;
}

export function lotesDeEmergencia(emergenciaId: number): Promise<Lote[]> {
  return get<Lote[]>(`/emergencias/${emergenciaId}/lotes`);
}
