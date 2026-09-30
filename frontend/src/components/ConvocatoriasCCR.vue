<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import type { Emergencia } from "../services/emergenciaService";
import { coberturaDe, convocatoriasAbiertas, type Cobertura } from "../services/ofertaService";
import { capitalizar, cierreDe, tiempoRestante } from "../utils/tiempo";
import HistorialOferta from "./HistorialOferta.vue";

// Pantalla del coordinador: cómo van las ofertas de cada convocatoria abierta.
// Solo muestra: la elección de quién entrega se hace cuando vence el timer de Bonita.
type Fila = { emergencia: Emergencia; cobertura: Cobertura | null; error: string };
const filas = ref<Fila[]>([]);
const cargando = ref(true);
const actualizando = ref(false);
const error = ref("");
const ahora = ref(Date.now());
const historialAbierto = ref<number | null>(null);
let activo = true;
let reloj: ReturnType<typeof setInterval>;
let refresco: ReturnType<typeof setInterval>;

function ventanaCerrada(emergencia: Emergencia) {
  const fin = cierreDe(emergencia);
  return Number.isFinite(fin) && fin <= ahora.value;
}

function lotesCubiertos(cobertura: Cobertura) {
  return cobertura.lotes.filter(l => l.cubierto).length;
}

function alternarHistorial(ofertaId: number) {
  historialAbierto.value = historialAbierto.value === ofertaId ? null : ofertaId;
}

async function cargar() {
  if (actualizando.value) return;
  actualizando.value = true;
  try {
    const emergencias = await convocatoriasAbiertas();
    const datos = await Promise.all(emergencias.map(async (emergencia): Promise<Fila> => {
      try {
        return { emergencia, cobertura: await coberturaDe(emergencia.id), error: "" };
      } catch (e) {
        return { emergencia, cobertura: null, error: e instanceof Error ? e.message : "No se pudo cargar la cobertura." };
      }
    }));
    if (!activo) return;
    filas.value = datos;
    error.value = "";
  } catch (e) {
    if (activo) error.value = e instanceof Error ? e.message : "No se pudieron cargar las convocatorias.";
  } finally {
    if (activo) {
      ahora.value = Date.now();
      cargando.value = false;
      actualizando.value = false;
    }
  }
}

function alVolver() {
  if (document.visibilityState === "visible") void cargar();
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
        <span class="inicio-etiqueta">SEGUIMIENTO</span>
        <h1 class="emergencia-titulo">Convocatorias abiertas</h1>
      </div>
      <button type="button" class="bandeja-actualizar" :disabled="actualizando" @click="cargar">Actualizar</button>
    </div>
    <p class="ong-ayuda">
      Cobertura de los lotes según las ofertas de las ONGs. Las ofertas pueden cambiar hasta que cierre la ventana;
      al cierre, Bonita consulta este consolidado y se evalúa quién entrega.
    </p>
    <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
    <p v-if="cargando" role="status">Cargando convocatorias...</p>
    <p v-else-if="!error && filas.length === 0" class="bandeja-vacia">No hay convocatorias abiertas.</p>
    <ul v-else class="bandeja-lista">
      <li v-for="fila in filas" :key="fila.emergencia.id" class="bandeja-item">
        <div class="bandeja-item-superior">
          <span class="bandeja-tipo">{{ capitalizar(fila.emergencia.tipoDesastre) }}</span>
          <span aria-hidden="true">·</span>
          <span class="bandeja-zona">{{ fila.emergencia.zonaAfectada }}</span>
        </div>
        <div class="bandeja-item-inferior">
          <span class="bandeja-detalle">
            <span class="bandeja-gravedad">Gravedad {{ capitalizar(fila.emergencia.gravedad) }}</span>
            <span>Emergencia #{{ fila.emergencia.id }}</span>
          </span>
          <span class="ong-tiempo" :class="{ 'ccr-cerrada': ventanaCerrada(fila.emergencia) }">
            {{ ventanaCerrada(fila.emergencia)
              ? "Ventana cerrada — esperando evaluación"
              : `Cierra en ${tiempoRestante(fila.emergencia, ahora)}` }}
          </span>
        </div>

        <p v-if="fila.error" class="emergencia-error ong-error" role="alert">{{ fila.error }}</p>
        <template v-else-if="fila.cobertura">
          <p class="ccr-resumen" :class="{ 'ccr-resumen-ok': fila.cobertura.lotesCubiertos }">
            {{ lotesCubiertos(fila.cobertura) }} de {{ fila.cobertura.lotes.length }} lotes cubiertos ·
            {{ fila.cobertura.totalOfertas }} {{ fila.cobertura.totalOfertas === 1 ? "oferta" : "ofertas" }}
          </p>
          <ul class="ong-lotes">
            <li v-for="lote in fila.cobertura.lotes" :key="lote.loteId" class="ong-lote ong-lote-oferta">
              <div class="ong-lote-cabecera">
                <div>
                  <strong>{{ lote.recurso }}</strong>
                  <p>
                    Ofrecido {{ lote.ofrecido }} de {{ lote.requerido }}
                    <template v-if="lote.faltante > 0"> · faltan {{ lote.faltante }}</template>
                    <template v-else> · cubierto</template>
                  </p>
                </div>
                <div class="cobertura-mini">
                  <div class="cobertura-barra" role="img" :aria-label="`Cubierto ${lote.porcentaje}%`">
                    <span :style="{ width: lote.porcentaje + '%' }" :class="{ completa: lote.cubierto }"></span>
                  </div>
                  <small>{{ lote.porcentaje }}%</small>
                </div>
              </div>
              <p v-if="!lote.ofertas.length" class="ccr-sin-ofertas">Todavía no hay ofertas para este lote.</p>
              <ul v-else class="ccr-ofertas">
                <li v-for="oferta in lote.ofertas" :key="oferta.id" class="ccr-oferta">
                  <div class="ccr-oferta-cabecera">
                    <span>
                      <strong>{{ oferta.liderNombre }}</strong>
                      <span v-if="oferta.consorcio" class="ccr-etiqueta">Consorcio</span>
                    </span>
                    <span>Total {{ oferta.total }} · v{{ oferta.version }}</span>
                  </div>
                  <ul class="ong-aportes">
                    <li v-for="a in oferta.aportes.filter(x => x.estado === 'CONFIRMADO' || x.estado === 'PENDIENTE')" :key="a.ongId">
                      {{ a.ongNombre }}: {{ a.estado === "PENDIENTE" ? "invitación pendiente" : a.cantidad }}
                    </li>
                  </ul>
                  <button type="button" class="enlace-boton" @click="alternarHistorial(oferta.id)">
                    {{ historialAbierto === oferta.id ? "Ocultar historial" : "Ver historial" }}
                  </button>
                  <HistorialOferta v-if="historialAbierto === oferta.id" :oferta-id="oferta.id" :key="oferta.version" />
                </li>
              </ul>
            </li>
          </ul>
        </template>
      </li>
    </ul>
  </section>
</template>
