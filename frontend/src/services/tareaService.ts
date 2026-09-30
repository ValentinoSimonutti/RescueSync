import { get } from "./api";
import type { Emergencia } from "./emergenciaService";

// Una tarea de la bandeja (TareaDTO en Java): qué tarea (Bonita) + de qué emergencia (Postgres)
export interface Tarea {
  tareaId: number;
  nombreTarea: string;      // ej: "Generar lotes"
  emergencia: Emergencia;
}

// GET /api/tareas -> las tareas pendientes del usuario logueado
export function misTareas(): Promise<Tarea[]> {
  return get<Tarea[]>("/tareas");
}
