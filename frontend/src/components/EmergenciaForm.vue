<script setup lang="ts">
import { primeraLetraMayuscula } from "../utils/entradaTexto";
import { computed, nextTick, ref } from "vue";
import {
  crearEmergencia,
  type CrearEmergenciaRequest
} from "../services/emergenciaService";

const emit = defineEmits<{ registrada: [] }>();

const zonaAfectada = ref("");
const descripcion = ref("");
const tipoDesastre = ref("");
const gravedad = ref("");

const error = ref("");
const cargando = ref(false);
const confirmacion = ref<HTMLDialogElement | null>(null);

const intentoRegistro = ref(false);
const formulario = ref<HTMLElement | null>(null);
const erroresCampos = computed(() => ({
  zona: !zonaAfectada.value.trim() ? "Ingresá la zona afectada." : zonaAfectada.value.length > 20 ? "La zona afectada no puede superar los 20 caracteres." : "",
  descripcion: !descripcion.value.trim() ? "Ingresá una descripción." : descripcion.value.length > 50 ? "La descripción no puede superar los 50 caracteres." : "",
  tipo: ["INUNDACION", "INCENDIO", "TERREMOTO"].includes(tipoDesastre.value) ? "" : "Seleccioná un tipo de desastre.",
  gravedad: ["BAJA", "MEDIA", "ALTA"].includes(gravedad.value) ? "" : "Seleccioná una gravedad."
}));

function validarCampos() {
  intentoRegistro.value = true;
  if (!Object.values(erroresCampos.value).some(Boolean)) return true;
  confirmacion.value?.close();
  void nextTick(() => {
    formulario.value?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  });
  return false;
}

function revisarEmergencia() {
  if (cargando.value) return;
  error.value = "";
  if (validarCampos()) confirmacion.value?.showModal();
}

function editarEmergencia() {
  if (!cargando.value) confirmacion.value?.close();
}

function mostrarOpcion(valor: string) {
  const etiquetas: Record<string, string> = {
    INUNDACION: "Inundación", INCENDIO: "Incendio", TERREMOTO: "Terremoto",
    BAJA: "Baja", MEDIA: "Media", ALTA: "Alta"
  };
  return etiquetas[valor] ?? "Sin seleccionar";
}

async function registrarEmergencia() {
  if (cargando.value || !validarCampos()) return;
  error.value = "";
  cargando.value = true;

  const datos: CrearEmergenciaRequest = {
    zonaAfectada: zonaAfectada.value,
    descripcion: descripcion.value,
    tipoDesastre: tipoDesastre.value,
    gravedad: gravedad.value
  };

  try {
    await crearEmergencia(datos);
    confirmacion.value?.close();

    intentoRegistro.value = false;
    zonaAfectada.value = "";
    descripcion.value = "";
    tipoDesastre.value = "";
    gravedad.value = "";
    emit("registrada");
  } catch (e) {
    // Mostramos el mensaje real del backend (ej: Bonita apagado, sin permiso)
    error.value = e instanceof Error ? e.message : "No se pudo registrar la emergencia.";
    console.error(e);
  } finally {
    cargando.value = false;
  }
}
</script>

<template>
  <section ref="formulario" class="emergencia-card">
    <span class="inicio-etiqueta emergencia-etiqueta">NUEVO REPORTE</span>
    <h1 class="emergencia-titulo">Registrar emergencia</h1>

    <p class="formulario-requeridos">Todos los campos son obligatorios.</p>

    <div class="emergencia-campo">
      <label for="zona">Zona afectada</label>
      <div class="zona-control">
        <input
          id="zona"
          v-model="zonaAfectada" @input="primeraLetraMayuscula($event, valor => zonaAfectada = valor)" autocapitalize="sentences"
          maxlength="20"
          required
          :aria-invalid="intentoRegistro && !!erroresCampos.zona"
          :aria-describedby="intentoRegistro && erroresCampos.zona ? 'contador-zona error-zona' : 'contador-zona'"
          type="text"
        />
        <span id="contador-zona" class="campo-contador">{{ zonaAfectada.length }}/20</span>
      </div>
      <p v-if="intentoRegistro && erroresCampos.zona" id="error-zona" class="campo-error">{{ erroresCampos.zona }}</p>
    </div>

    <div class="emergencia-campo">
      <label for="descripcion">Descripción</label>
      <div class="descripcion-control">
        <textarea
          id="descripcion"
          v-model="descripcion" @input="primeraLetraMayuscula($event, valor => descripcion = valor)" autocapitalize="sentences"
          maxlength="50"
          required
          :aria-invalid="intentoRegistro && !!erroresCampos.descripcion"
          :aria-describedby="intentoRegistro && erroresCampos.descripcion ? 'contador-descripcion error-descripcion' : 'contador-descripcion'"
        ></textarea>
        <span id="contador-descripcion" class="campo-contador">{{ descripcion.length }}/50</span>
      </div>
      <p v-if="intentoRegistro && erroresCampos.descripcion" id="error-descripcion" class="campo-error">{{ erroresCampos.descripcion }}</p>
    </div>

    <fieldset class="emergencia-seleccion">
      <legend>Tipo de desastre</legend>
      <div class="emergencia-opciones">
        <label class="emergencia-opcion">
          <input v-model="tipoDesastre" type="radio" name="tipo" required :aria-invalid="intentoRegistro && !!erroresCampos.tipo" :aria-describedby="intentoRegistro && erroresCampos.tipo ? 'error-tipo' : undefined" value="INUNDACION" @click="tipoDesastre === 'INUNDACION' && (tipoDesastre = '')" />
          <span>Inundación</span>
        </label>
        <label class="emergencia-opcion">
          <input v-model="tipoDesastre" type="radio" name="tipo" required :aria-invalid="intentoRegistro && !!erroresCampos.tipo" :aria-describedby="intentoRegistro && erroresCampos.tipo ? 'error-tipo' : undefined" value="INCENDIO" @click="tipoDesastre === 'INCENDIO' && (tipoDesastre = '')" />
          <span>Incendio</span>
        </label>
        <label class="emergencia-opcion">
          <input v-model="tipoDesastre" type="radio" name="tipo" required :aria-invalid="intentoRegistro && !!erroresCampos.tipo" :aria-describedby="intentoRegistro && erroresCampos.tipo ? 'error-tipo' : undefined" value="TERREMOTO" @click="tipoDesastre === 'TERREMOTO' && (tipoDesastre = '')" />
          <span>Terremoto</span>
        </label>
      </div>
      <p v-if="intentoRegistro && erroresCampos.tipo" id="error-tipo" class="campo-error">{{ erroresCampos.tipo }}</p>
    </fieldset>

    <fieldset class="emergencia-seleccion">
      <legend>Gravedad</legend>
      <div class="emergencia-opciones">
        <label class="emergencia-opcion">
          <input v-model="gravedad" type="radio" name="gravedad" required :aria-invalid="intentoRegistro && !!erroresCampos.gravedad" :aria-describedby="intentoRegistro && erroresCampos.gravedad ? 'error-gravedad' : undefined" value="BAJA" @click="gravedad === 'BAJA' && (gravedad = '')" />
          <span>Baja</span>
        </label>
        <label class="emergencia-opcion">
          <input v-model="gravedad" type="radio" name="gravedad" required :aria-invalid="intentoRegistro && !!erroresCampos.gravedad" :aria-describedby="intentoRegistro && erroresCampos.gravedad ? 'error-gravedad' : undefined" value="MEDIA" @click="gravedad === 'MEDIA' && (gravedad = '')" />
          <span>Media</span>
        </label>
        <label class="emergencia-opcion">
          <input v-model="gravedad" type="radio" name="gravedad" required :aria-invalid="intentoRegistro && !!erroresCampos.gravedad" :aria-describedby="intentoRegistro && erroresCampos.gravedad ? 'error-gravedad' : undefined" value="ALTA" @click="gravedad === 'ALTA' && (gravedad = '')" />
          <span>Alta</span>
        </label>
      </div>
      <p v-if="intentoRegistro && erroresCampos.gravedad" id="error-gravedad" class="campo-error">{{ erroresCampos.gravedad }}</p>
    </fieldset>

    <button
      class="btn-ingresar emergencia-submit"
      type="button"
      @click="revisarEmergencia"
      :disabled="cargando"
    >
      {{ cargando ? "Registrando..." : "Registrar" }}
    </button>

    <p v-if="error" class="emergencia-error">
      {{ error }}
    </p>
    <dialog
      ref="confirmacion"
      class="modal-salir modal-emergencia"
      aria-labelledby="confirmacion-emergencia-titulo"
      aria-describedby="confirmacion-emergencia-pregunta"
      :aria-busy="cargando"
      @cancel.prevent="editarEmergencia"
    >
      <h2 id="confirmacion-emergencia-titulo">Revisar emergencia</h2>
      <p id="confirmacion-emergencia-pregunta">¿Confirmás el registro de esta emergencia?</p>
      <dl class="confirmacion-resumen">
        <div>
          <dt>Zona afectada</dt>
          <dd>{{ zonaAfectada || "Sin completar" }}</dd>
        </div>
        <div>
          <dt>Descripción</dt>
          <dd>{{ descripcion || "Sin completar" }}</dd>
        </div>
        <div>
          <dt>Tipo de desastre</dt>
          <dd>{{ mostrarOpcion(tipoDesastre) }}</dd>
        </div>
        <div>
          <dt>Gravedad</dt>
          <dd>{{ mostrarOpcion(gravedad) }}</dd>
        </div>
      </dl>
      <p v-if="error" class="emergencia-error" role="alert">{{ error }}</p>
      <div class="modal-salir-acciones">
        <button type="button" class="bandeja-actualizar" :disabled="cargando" autofocus @click="editarEmergencia">
          Editar
        </button>
        <button type="button" class="btn-ingresar" :disabled="cargando" @click="registrarEmergencia">
          {{ cargando ? "Registrando..." : "Confirmar" }}
        </button>
      </div>
    </dialog>
  </section>
</template>
