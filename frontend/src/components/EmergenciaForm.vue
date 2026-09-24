<script setup lang="ts">
import { ref } from "vue";
import {
  crearEmergencia,
  type CrearEmergenciaRequest
} from "../services/emergenciaService";

const zonaAfectada = ref("");
const descripcion = ref("");
const tipoDesastre = ref("");
const gravedad = ref("");

const mensaje = ref("");
const error = ref("");
const cargando = ref(false);

async function registrarEmergencia() {
  mensaje.value = "";
  error.value = "";
  cargando.value = true;

  const datos: CrearEmergenciaRequest = {
    zonaAfectada: zonaAfectada.value,
    descripcion: descripcion.value,
    tipoDesastre: tipoDesastre.value,
    gravedad: gravedad.value
  };

  try {
    const emergencia = await crearEmergencia(datos);

    mensaje.value = `Emergencia registrada correctamente. ID: ${emergencia.id}`;

    zonaAfectada.value = "";
    descripcion.value = "";
    tipoDesastre.value = "";
    gravedad.value = "";
  } catch (e) {
    error.value = "No se pudo registrar la emergencia.";
    console.error(e);
  } finally {
    cargando.value = false;
  }
}
</script>

<template>
  <section>
    <h1>Registrar emergencia</h1>

    <div>
      <label for="zona">Zona afectada</label>
      <input
        id="zona"
        v-model="zonaAfectada"
        type="text"
      />
    </div>

    <div>
      <label for="descripcion">Descripción</label>
      <textarea
        id="descripcion"
        v-model="descripcion"
      ></textarea>
    </div>

    <div>
      <label for="tipo">Tipo de desastre</label>

      <select id="tipo" v-model="tipoDesastre">
        <option value="">Seleccionar</option>
        <option value="INUNDACION">Inundación</option>
        <option value="INCENDIO">Incendio</option>
        <option value="TERREMOTO">Terremoto</option>
      </select>
    </div>

    <div>
      <label for="gravedad">Gravedad</label>

      <select id="gravedad" v-model="gravedad">
        <option value="">Seleccionar</option>
        <option value="BAJA">Baja</option>
        <option value="MEDIA">Media</option>
        <option value="ALTA">Alta</option>
      </select>
    </div>

    <button
      @click="registrarEmergencia"
      :disabled="cargando"
    >
      {{ cargando ? "Registrando..." : "Registrar" }}
    </button>

    <p v-if="mensaje">
      {{ mensaje }}
    </p>

    <p v-if="error">
      {{ error }}
    </p>
  </section>
</template>