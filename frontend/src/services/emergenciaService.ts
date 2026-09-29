import { post } from "./api";

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