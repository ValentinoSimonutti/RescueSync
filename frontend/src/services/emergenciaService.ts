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
  bonitaCaseId: number | null;   // case creado en Bonita para esta emergencia
}

export function crearEmergencia(
  datos: CrearEmergenciaRequest
): Promise<Emergencia> {
  return post<Emergencia>("/emergencias", datos);
}

// Un lote de necesidad: ej { recurso: "Paramédicos", cantidad: 5 }
export interface NuevoLote {
  recurso: string;
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

// El backend filtra por el municipio de la sesión actual.
export function misEmergencias(): Promise<Emergencia[]> {
  return get<Emergencia[]>("/emergencias/mias");
}
