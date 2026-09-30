// Rutas relativas: el proxy de Vite (vite.config.ts) las manda a Spring (8081).
// Así el navegador cree que todo es del mismo sitio y guarda/manda la cookie de sesión.
const API_URL = "/api";

// Error con el código HTTP y el mensaje que mandó el backend
export class ApiError extends Error {
    status: number;

    constructor(status: number, mensaje: string) {
        super(mensaje);
        this.status = status;
    }
}

// Función base: hace la request y, si falla, arma un ApiError con el mensaje del backend
async function request<T>(metodo: string, endpoint: string, datos?: unknown): Promise<T> {

    const response = await fetch(`${API_URL}${endpoint}`, {
        method: metodo,
        headers: datos !== undefined ? { "Content-Type": "application/json" } : {},
        body: datos !== undefined ? JSON.stringify(datos) : undefined
        // No hace falta mandar la cookie a mano: el navegador la manda sola (mismo sitio)
    });

    if (!response.ok) {
        // Spring manda el mensaje en "mensaje" (errores de Bonita) o en "message" (errores de Spring)
        let mensaje = `Error HTTP: ${response.status}`;
        try {
            const cuerpo = await response.json();
            mensaje = cuerpo.mensaje ?? cuerpo.message ?? mensaje;
        } catch {
            // la respuesta no era JSON: dejamos el mensaje genérico
        }
        throw new ApiError(response.status, mensaje);
    }

    // Algunas respuestas no tienen cuerpo (ej: logout)
    const texto = await response.text();
    return (texto ? JSON.parse(texto) : undefined) as T;
}

export function get<T>(endpoint: string): Promise<T> {
    return request<T>("GET", endpoint);
}

export function post<T>(endpoint: string, datos?: unknown): Promise<T> {
    return request<T>("POST", endpoint, datos);
}

export function put<T>(endpoint: string, datos?: unknown): Promise<T> {
    return request<T>("PUT", endpoint, datos);
}
