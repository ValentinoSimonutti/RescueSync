import { get, post } from "./api";

// Lo que devuelve Spring sobre el usuario logueado (UsuarioDTO en Java)
export interface Usuario {
  username: string;
  nombre: string;
  rol: "OPERADOR_MUNICIPAL" | "CCR" | "REPRESENTANTE_ONG" | "AUDITOR";
  organizacionId: number | null;
  organizacionNombre: string | null;
}

// POST /api/auth/login -> Spring valida contra Bonita y crea la sesión (cookie)
export function login(username: string, password: string): Promise<Usuario> {
  return post<Usuario>("/auth/login", { username, password });
}

// GET /api/auth/me -> quién está logueado (falla con 401 si nadie)
export function usuarioActual(): Promise<Usuario> {
  return get<Usuario>("/auth/me");
}

// POST /api/auth/logout -> borra la sesión en Spring
export function logout(): Promise<void> {
  return post<void>("/auth/logout");
}
