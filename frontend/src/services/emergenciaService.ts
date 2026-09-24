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
}

export function crearEmergencia(
  datos: CrearEmergenciaRequest
): Promise<Emergencia> {
  return post<Emergencia>("/emergencias", datos);
}