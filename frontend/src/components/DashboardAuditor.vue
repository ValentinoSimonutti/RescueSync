<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
  indicadoresBonita, indicadoresLocales,
  type DashboardBonita, type DashboardLocal
} from "../services/dashboardService";
import { tiposRecurso, type TipoRecurso } from "../services/catalogoService";
import { descargarJson } from "../utils/exportar";
import PanelProceso from "./dashboard/PanelProceso.vue";
import PanelOperacion from "./dashboard/PanelOperacion.vue";

// Dashboard del Auditor / Directivo. Junta las dos fuentes de indicadores:
//  - base local de la app (ofertas, consorcios, suministros)
//  - API de Bonita (tiempos de cada etapa del proceso)
// Cada una se pide por separado: si una no responde, las otras se muestran igual.

interface Fuente<T> {
  datos: T | null;
  cargando: boolean;
  error: string;
}

function nuevaFuente<T>() {
  return ref<Fuente<T>>({ datos: null, cargando: false, error: "" });
}

const local = nuevaFuente<DashboardLocal>();
const bonita = nuevaFuente<DashboardBonita>();
const catalogo = ref<TipoRecurso[]>([]);   // solo para las unidades (litros, personas...)

const desde = ref("");
const hasta = ref("");

async function cargar<T>(fuente: typeof local | typeof bonita, pedido: () => Promise<T>) {
  const f = fuente.value as Fuente<T>;
  f.cargando = true;
  f.error = "";
  try {
    f.datos = await pedido();
  } catch (e) {
    f.error = e instanceof Error ? e.message : "No se pudieron cargar los datos";
  } finally {
    f.cargando = false;
  }
}

// El filtro de fechas aplica a la base local y a Bonita
function cargarConFiltro() {
  void cargar(local, () => indicadoresLocales(desde.value, hasta.value));
  void cargar(bonita, () => indicadoresBonita(desde.value, hasta.value));
}

function actualizarTodo() {
  cargarConFiltro();
}

// ---- Rangos rápidos ----
const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

function rangoRapido(tipo: "todo" | "mes" | "30") {
  const hoy = new Date();
  if (tipo === "todo") {
    desde.value = "";
    hasta.value = "";
  } else if (tipo === "mes") {
    desde.value = iso(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
    hasta.value = iso(hoy);
  } else {
    desde.value = iso(new Date(hoy.getTime() - 29 * 86400000));
    hasta.value = iso(hoy);
  }
  cargarConFiltro();
}

const rangoActivo = computed(() => {
  if (!desde.value && !hasta.value) return "todo";
  const hoy = new Date();
  if (hasta.value === iso(hoy) && desde.value === iso(new Date(hoy.getFullYear(), hoy.getMonth(), 1))) return "mes";
  if (hasta.value === iso(hoy) && desde.value === iso(new Date(hoy.getTime() - 29 * 86400000))) return "30";
  return "";
});

const rangoInvalido = computed(() => !!desde.value && !!hasta.value && desde.value > hasta.value);

function alCambiarFecha() {
  if (!rangoInvalido.value) cargarConFiltro();
}

// nombre del recurso -> unidad, para mostrar "litros", "personas"... en el panel local
const unidades = computed<Record<string, string>>(() =>
  Object.fromEntries(catalogo.value.map(t => [t.nombre, t.unidad])));

const hayDatos = computed(() => !!(local.value.datos || bonita.value.datos));

function exportarTodo() {
  descargarJson(`rescuesync-indicadores-${iso(new Date())}`, {
    generado: new Date().toISOString(),
    periodo: { desde: desde.value || null, hasta: hasta.value || null },
    baseLocal: local.value.datos,
    bonita: bonita.value.datos
  });
}

onMounted(() => {
  actualizarTodo();
  tiposRecurso().then(t => (catalogo.value = t)).catch(() => {});   // si falla, se muestra "unidad"
});
</script>

<template>
  <section class="dash" aria-labelledby="dash-titulo">
    <div class="bandeja-encabezado">
      <div>
        <span class="inicio-etiqueta">AUDITORÍA Y SEGUIMIENTO</span>
        <h1 id="dash-titulo" class="emergencia-titulo">Indicadores de RescueSync</h1>
      </div>
      <div class="dash-encabezado-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="!hayDatos" @click="exportarTodo">
          Exportar todo (JSON)
        </button>
        <button type="button" class="bandeja-actualizar"
          :disabled="local.cargando || bonita.cargando" @click="actualizarTodo">
          Actualizar
        </button>
      </div>
    </div>

    <!-- Filtros: una sola fila arriba de todo -->
    <div class="bandeja-filtros dash-filtros" role="group" aria-label="Período">
      <label class="bandeja-filtro">
        Desde
        <input v-model="desde" type="date" :max="hasta || undefined" @change="alCambiarFecha" />
      </label>
      <label class="bandeja-filtro">
        Hasta
        <input v-model="hasta" type="date" :min="desde || undefined" @change="alCambiarFecha" />
      </label>
      <div class="dash-rangos" role="group" aria-label="Rangos rápidos">
        <button type="button" class="dash-boton-chico" :aria-pressed="rangoActivo === 'todo'" @click="rangoRapido('todo')">Todo</button>
        <button type="button" class="dash-boton-chico" :aria-pressed="rangoActivo === 'mes'" @click="rangoRapido('mes')">Este mes</button>
        <button type="button" class="dash-boton-chico" :aria-pressed="rangoActivo === '30'" @click="rangoRapido('30')">Últimos 30 días</button>
      </div>
    </div>
    <p v-if="rangoInvalido" class="emergencia-error" role="alert">La fecha "desde" tiene que ser anterior a "hasta".</p>
    <p class="ong-ayuda">
      El período filtra las emergencias por fecha de registro y los casos de Bonita por fecha de inicio.
    </p>

    <section class="dash-seccion" aria-labelledby="dash-proceso">
      <header class="dash-seccion-encabezado">
        <h2 id="dash-proceso">Tiempos del proceso</h2>
        <span class="dash-fuente">Fuente: API REST de Bonita</span>
      </header>
      <p v-if="bonita.error" class="emergencia-error" role="alert">{{ bonita.error }}</p>
      <p v-else-if="bonita.cargando && !bonita.datos" role="status">Consultando a Bonita...</p>
      <PanelProceso v-if="bonita.datos" :datos="bonita.datos" />
    </section>

    <section class="dash-seccion" aria-labelledby="dash-operacion">
      <header class="dash-seccion-encabezado">
        <h2 id="dash-operacion">Operación: emergencias, ofertas y suministros</h2>
        <span class="dash-fuente">Fuente: base de datos de RescueSync</span>
      </header>
      <p v-if="local.error" class="emergencia-error" role="alert">{{ local.error }}</p>
      <p v-else-if="local.cargando && !local.datos" role="status">Cargando indicadores...</p>
      <PanelOperacion v-if="local.datos" :datos="local.datos" :unidades="unidades" />
    </section>
  </section>
</template>