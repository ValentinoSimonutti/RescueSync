import { get } from "./api";

// ---- Base local (DashboardLocalDTO en Java) ---------------------------------
export interface Conteo {
  clave: string;
  cantidad: number;
}

export interface DashboardLocal {
  resumen: {
    emergencias: number;
    enCurso: number;
    cerradas: number;
    lotesAdjudicados: number;
    unidadesMovilizadas: number;
    porcentajeConsorcio: number | null;
    porcentajeCobertura: number | null;
    duracionPromedioHoras: number | null;
    ofertas: number;
    promedioVersiones: number | null;
  };
  porEstado: Conteo[];
  porTipo: Conteo[];
  porGravedad: Conteo[];
  movilizadosPorMes: { mes: string; recurso: string; unidades: number }[];
  movilizadosPorRecurso: { recurso: string; unidades: number; lotes: number }[];
  consorcios: { lotesAdjudicados: number; porConsorcio: number; individuales: number; porcentaje: number | null };
  participacionOngs: {
    ong: string;
    unidadesMovilizadas: number;
    lotesIndividuales: number;
    lotesEnConsorcio: number;
    ofertasPresentadas: number;
  }[];
  validaciones: Conteo[];
  proyectosFinalizados: {
    emergenciaId: number;
    zona: string;
    tipo: string | null;
    gravedad: string | null;
    fechaRegistro: string | null;
    fechaCierre: string | null;
    duracionHoras: number | null;
    lotes: number;
    unidades: number;
    ongs: number;
    coberturaParcial: boolean;
  }[];
}

// ---- Bonita (DashboardBonitaDTO) ---------------------------------------------
export interface EtapaBonita {
  orden: number;
  nombre: string;
  casos: number;
  promedioMin: number | null;
  minimoMin: number | null;
  maximoMin: number | null;
}

export interface TareaBonita {
  nombre: string;
  etapa: string;
  responsable: string;
  tipo: string;
  ejecuciones: number;
  promedioMin: number | null;
  maximoMin: number | null;
}

export interface DashboardBonita {
  casosActivos: number;
  casosFinalizados: number;
  duracionPromedioCasoMin: number | null;
  etapas: EtapaBonita[];
  tareas: TareaBonita[];
  pendientes: { tarea: string; responsable: string; cantidad: number; esperaPromedioMin: number | null }[];
  casos: {
    caseId: number;
    emergenciaId: number | null;
    zona: string | null;
    estado: string;
    inicio: string | null;
    fin: string | null;
    duracionMin: number | null;
    etapaActual: string | null;
  }[];
  advertencia: string | null;
}

// ---- Sistema Nacional (DashboardSistemaNacionalDTO) ----------------------------
export interface DashboardNacional {
  recursos: { codigo: string; nombre: string; unidad: string; total: number; comprometido: number; disponible: number }[];
  compromisosActivos: number;
  compromisosLiberados: number;
  unidadesComprometidas: number;
  ongsHabilitadas: number;
  ongsRegistradas: number;
  ongsPorEstado: Conteo[];
  detalleActivos: { emergenciaRef: string; ong: string; recurso: string; cantidad: number; rol: string; fecha: string | null }[];
}

// desde / hasta: "yyyy-MM-dd" o vacío
function rango(desde: string, hasta: string): string {
  const params = new URLSearchParams();
  if (desde) params.set("desde", desde);
  if (hasta) params.set("hasta", hasta);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export function indicadoresLocales(desde: string, hasta: string): Promise<DashboardLocal> {
  return get<DashboardLocal>(`/dashboard/local${rango(desde, hasta)}`);
}

export function indicadoresBonita(desde: string, hasta: string): Promise<DashboardBonita> {
  return get<DashboardBonita>(`/dashboard/bonita${rango(desde, hasta)}`);
}

export function indicadoresNacionales(): Promise<DashboardNacional> {
  return get<DashboardNacional>("/dashboard/sistema-nacional");
}
