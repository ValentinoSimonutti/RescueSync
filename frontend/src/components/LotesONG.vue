<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { misTareas } from "../services/tareaService";
import { lotesDeEmergencia, type Emergencia, type Lote } from "../services/emergenciaService";

type Convocatoria = { emergencia: Emergencia; lotes: Lote[]; error: string };
const convocatorias = ref<Convocatoria[]>([]);
const cargando = ref(true);
const actualizando = ref(false);
const error = ref("");
const ahora = ref(Date.now());
let activo = true;
let reloj: ReturnType<typeof setInterval>;
let refresco: ReturnType<typeof setInterval>;

// El contrato actual usa LocalDateTime de Argentina. Respetar el offset si la API lo incorpora.
function cierre(emergencia: Emergencia) {
  const fecha = emergencia.fechaCierreOfertas;
  if (!fecha) return NaN;
  return Date.parse(/(?:Z|[+-]\d{2}:\d{2})$/i.test(fecha) ? fecha : `${fecha}-03:00`);
}
const vigentes = computed(() => convocatorias.value.filter(c => {
  const fin = cierre(c.emergencia);
  return !Number.isFinite(fin) || fin > ahora.value;
}));
function tiempoRestante(emergencia: Emergencia) {
  const fin = cierre(emergencia);
  if (!Number.isFinite(fin)) return "Cierre no disponible";
  const segundos = Math.max(0, Math.ceil((fin - ahora.value) / 1000));
  const horas = Math.floor(segundos / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  return `${horas ? `${horas} h ` : ""}${minutos} min ${segundos % 60} s`;
}
function mostrar(valor: string) {
  return valor.toLocaleLowerCase("es").replace(/^./u, letra => letra.toLocaleUpperCase("es"));
}
async function cargar() {
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    const tareas = await misTareas();
    const emergencias = [...new Map(tareas
      .filter(t => t.nombreTarea === "Cargar o editar ofertas")
      .map(t => [t.emergencia.id, t.emergencia])).values()];
    const datos = await Promise.all(emergencias.map(async emergencia => {
      if (cierre(emergencia) <= Date.now()) return { emergencia, lotes: [], error: "" };
      try {
        return { emergencia, lotes: await lotesDeEmergencia(emergencia.id), error: "" };
      } catch (e) {
        return { emergencia, lotes: [], error: e instanceof Error ? e.message : "No se pudieron cargar los lotes." };
      }
    }));
    if (!activo) return;
    convocatorias.value = datos;
    error.value = "";
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron actualizar las convocatorias.";
  } finally {
    if (activo) {
      ahora.value = Date.now();
      cargando.value = false;
      actualizando.value = false;
    }
  }
}
function alVolver() {
  if (document.visibilityState === "visible") {
    ahora.value = Date.now();
    void cargar();
  }
}
onMounted(() => {
  void cargar();
  reloj = setInterval(() => { ahora.value = Date.now(); }, 1000);
  refresco = setInterval(() => { if (!document.hidden) void cargar(); }, 15000);
  document.addEventListener("visibilitychange", alVolver);
});
onUnmounted(() => {
  activo = false;
  clearInterval(reloj);
  clearInterval(refresco);
  document.removeEventListener("visibilitychange", alVolver);
});
</script>

<template>
  <section class="bandeja">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">CONVOCATORIAS</span>
        <h1 class="emergencia-titulo">Lotes disponibles</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="actualizando" @click="cargar">Actualizar</button>
    </div>
    <p class="ong-aviso">La opción de ofrecer ayuda estará disponible próximamente.</p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-if="cargando" role="status">Cargando lotes...</p>
    <p v-else-if="!error && vigentes.length === 0" class="bandeja-vacia">No hay convocatorias vigentes disponibles.</p>
    <ul v-else class="bandeja-lista">
      <li v-for="convocatoria in vigentes" :key="convocatoria.emergencia.id" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ mostrar(convocatoria.emergencia.tipoDesastre) }}</span>
          <span aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ convocatoria.emergencia.zonaAfectada }}</span>
        </div>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ mostrar(convocatoria.emergencia.gravedad) }}</span>
            <span>Emergencia #{{ convocatoria.emergencia.id }}</span>
          </span>
          <span class="ong-tiempo">Tiempo restante: {{ tiempoRestante(convocatoria.emergencia) }}</span>
        </div>
        <p v-if="convocatoria.error" class="emergencia-error ong-error" role="alert">{{ convocatoria.error }}</p>
        <p v-else-if="!convocatoria.lotes.length" class="ong-lote">No hay lotes publicados para esta convocatoria.</p>
        <ul v-else class="ong-lotes">
          <li v-for="lote in convocatoria.lotes" :key="lote.id" class="ong-lote">
            <div><strong>{{ lote.recurso }}</strong><p>Cantidad requerida: {{ lote.cantidad }}</p></div>
            <button type="button" class="btn-ingresar bandeja-boton" disabled title="Próximamente">Ofrecer ayuda</button>
          </li>
        </ul>
      </li>
    </ul>
  </section>
</template>
