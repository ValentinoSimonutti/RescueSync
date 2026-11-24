import { get } from "./api";

// Tipo de recurso del catálogo nacional (catálogo precargado en el backend)
export interface TipoRecurso {
  codigo: string;                         // ej: "PARAMEDICO"
  nombre: string;                         // ej: "Paramédicos"
  categoria: string;                      // PERSONAL, EQUIPO, INSUMO...
  unidad: string;                         // persona, litro, unidad...
  certificacionRequerida: string | null;
}

export function tiposRecurso(): Promise<TipoRecurso[]> {
  return get<TipoRecurso[]>("/catalogo/tipos-recurso");
}