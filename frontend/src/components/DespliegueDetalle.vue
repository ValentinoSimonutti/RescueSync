<script setup lang="ts">
import type { Despliegue } from "../services/ejecucionService";
import type { RolValidacion } from "../services/ofertaService";
import { fechaCorta } from "../utils/tiempo";

// Qué se adjudicó por lote y en qué está cada ONG (lo usan Monitorear y Seguimiento)
defineProps<{ despliegue: Despliegue }>();

const etiquetaRol: Record<RolValidacion, string> = {
  PRINCIPAL: "Principal",
  APOYO_SECUNDARIO: "Apoyo secundario",
  SIN_HABILITACION_VIGENTE: "Sin habilitación vigente"
};
</script>

<template>
  <ul class="despliegue-lotes">
    <li v-for="asignacion in despliegue.asignaciones" :key="asignacion.ofertaId">
      <h3>{{ asignacion.recurso }} · {{ asignacion.cantidad }}{{ asignacion.consorcio ? " · en consorcio" : "" }}</h3>
      <ul class="despliegue-ongs">
        <li v-for="ong in asignacion.ongs" :key="ong.ongId">
          <span>
            <strong>{{ ong.ongNombre }}</strong>: {{ ong.cantidad }}
            <span v-if="ong.rol" class="adjudicar-rol" :class="{ apoyo: ong.rol !== 'PRINCIPAL' }">{{ etiquetaRol[ong.rol] }}</span>
          </span>
          <span :class="ong.fechaFinalizacion ? 'despliegue-fin' : 'despliegue-curso'">
            {{ ong.fechaFinalizacion ? `Finalizó el ${fechaCorta(ong.fechaFinalizacion)}` : despliegue.emergencia.estado === "EN_EJECUCION" ? "En curso" : "Sin iniciar" }}
          </span>
        </li>
      </ul>
    </li>
  </ul>
</template>
