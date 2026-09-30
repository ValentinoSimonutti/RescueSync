import { post } from "./api";

// Lo que devuelve Spring (OfertaDTO en Java)
export interface Oferta {
  id: number;
  cantidad: number;
  recurso: string;
  ongId: number;
  loteId: number;
}

// POST /api/ofertas -> la ONG sale de la sesión en el backend (no se manda desde acá)
export function ofertar(loteId: number, cantidad: number): Promise<Oferta> {
  return post<Oferta>("/ofertas", { loteId, cantidad });
}
