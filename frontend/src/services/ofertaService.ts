import { get, post, put } from "./api";
import type { Emergencia } from "./emergenciaService";

// Lo que pone UNA ONG dentro de una oferta (AporteDTO en Java)
export interface Aporte {
  ongId: number;
  ongNombre: string;
  cantidad: number;
  estado: "PENDIENTE" | "CONFIRMADO" | "RECHAZADO" | "RETIRADO";
  rolValidacion: RolValidacion | null;   // perfil del Sistema Nacional para esta ONG
  cantidadValidada: number | null;
}

// Perfil que asigna el Sistema Nacional al validar
export type RolValidacion = "PRINCIPAL" | "APOYO_SECUNDARIO" | "SIN_HABILITACION_VIGENTE";

// Una oferta: individual (1 aporte) o consorcio (varios aportes) — OfertaDTO en Java
export interface Oferta {
  id: number;
  loteId: number;
  emergenciaId: number;
  recurso: string;
  cantidadRequerida: number;   // lo que pide el lote
  total: number;               // suma de aportes confirmados
  estado: "ACTIVA" | "RETIRADA";
  version: number;
  consorcio: boolean;
  liderId: number;
  liderNombre: string;
  fechaActualizacion: string;
  aportes: Aporte[];
  soyLider: boolean | null;    // solo viene cuando consulta una ONG
  miAporte: Aporte | null;     // el aporte de la ONG que consulta (si participa)
  zonaAfectada: string;
  rolValidacion: RolValidacion | null;          // null antes de la validación
  cantidadValidada: number | null;
  observacionesValidacion: string[];
  resultado: "ADJUDICADA" | "NO_SELECCIONADA" | null;   // null antes de adjudicar
}

// Una línea del historial (trazabilidad)
export interface VersionOferta {
  numero: number;
  fecha: string;
  autor: string;
  accion: string;
  total: number;
  detalle: string;
}

export interface Ong {
  id: number;
  nombre: string;
}

// Cobertura: cuánto se ofreció por lote
export interface CoberturaLote {
  loteId: number;
  recurso: string;
  recursoCodigo: string | null;
  requerido: number;
  ofrecido: number;
  faltante: number;
  porcentaje: number;
  cubierto: boolean;          // al menos una oferta cubre el lote sola
  ofertasCompletas: number;   // ofertas que compiten por el lote
  ofertas: Oferta[];
}

export interface Cobertura {
  emergencia: Emergencia;
  lotesCubiertos: boolean;
  totalOfertas: number;
  lotes: CoberturaLote[];
}

// ---- ONG ----

// Crea una oferta. Si invita ONGs, arma un consorcio (las invitadas quedan pendientes)
export function crearOferta(loteId: number, cantidad: number, ongsInvitadas: number[] = []): Promise<Oferta> {
  return post<Oferta>("/ofertas", { loteId, cantidad, ongsInvitadas });
}

// Ofertas donde participa la ONG logueada + invitaciones pendientes
export function misOfertas(): Promise<Oferta[]> {
  return get<Oferta[]>("/ofertas/mias");
}

// Cambia el aporte propio (o acepta una invitación). Genera una versión nueva
export function actualizarAporte(ofertaId: number, cantidad: number): Promise<Oferta> {
  return put<Oferta>(`/ofertas/${ofertaId}/aporte`, { cantidad });
}

// Líder: retira la oferta. Invitada: rechaza. Miembro: sale del consorcio
export function retirarOferta(ofertaId: number): Promise<Oferta> {
  return post<Oferta>(`/ofertas/${ofertaId}/retirar`);
}

// ONGs a las que se puede invitar a un consorcio
export function ongsParaInvitar(): Promise<Ong[]> {
  return get<Ong[]>("/ofertas/ongs");
}

// ---- ONG y coordinador ----

export function historialOferta(ofertaId: number): Promise<VersionOferta[]> {
  return get<VersionOferta[]>(`/ofertas/${ofertaId}/historial`);
}

export function coberturaDe(emergenciaId: number): Promise<Cobertura> {
  return get<Cobertura>(`/emergencias/${emergenciaId}/cobertura`);
}

// ---- Coordinador ----

export function convocatoriasAbiertas(): Promise<Emergencia[]> {
  return get<Emergencia[]>("/emergencias/convocatorias");
}
