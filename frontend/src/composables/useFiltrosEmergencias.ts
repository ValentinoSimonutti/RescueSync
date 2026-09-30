import { computed, ref, type Ref } from "vue";
import type { Emergencia } from "../services/emergenciaService";

export function useFiltrosEmergencias<T>(datos: Ref<T[]>, obtenerEmergencia: (item: T) => Emergencia) {
  const zona = ref("");
  const gravedad = ref("");
  const tipo = ref("");
  const orden = ref("gravedad-desc");
  const prioridades: Record<string, number> = { ALTA: 3, MEDIA: 2, BAJA: 1 };
  const normalizar = (texto: string) => texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es");

  const resultados = computed(() => {
    const busqueda = normalizar(zona.value.trim());
    return datos.value.filter((item) => {
      const e = obtenerEmergencia(item);
      return (!gravedad.value || e.gravedad === gravedad.value)
        && (!tipo.value || e.tipoDesastre === tipo.value)
        && normalizar(e.zonaAfectada).includes(busqueda);
    }).sort((a, b) => {
      const ea = obtenerEmergencia(a);
      const eb = obtenerEmergencia(b);
      if (orden.value === "id-desc") return eb.id - ea.id;
      if (orden.value === "id-asc") return ea.id - eb.id;
      const pa = prioridades[ea.gravedad];
      const pb = prioridades[eb.gravedad];
      // Los valores desconocidos quedan al final en ambos sentidos.
      if (pa === undefined || pb === undefined) {
        return pa === pb ? eb.id - ea.id : pa === undefined ? 1 : -1;
      }
      return (orden.value === "gravedad-asc" ? pa - pb : pb - pa) || eb.id - ea.id;
    });
  });

  function limpiar() {
    zona.value = "";
    gravedad.value = "";
    tipo.value = "";
    orden.value = "gravedad-desc";
  }

  return { zona, gravedad, tipo, orden, resultados, limpiar };
}
