<script setup lang="ts">
const zona = defineModel<string>("zona", { required: true });
const gravedad = defineModel<string>("gravedad", { required: true });
const tipo = defineModel<string>("tipo", { required: true });
const orden = defineModel<string>("orden", { required: true });
defineEmits<{ limpiar: [] }>();
</script>

<template>
  <div class="bandeja-filtros" role="group" aria-label="Filtros y orden de emergencias">
    <label class="bandeja-filtro">
      <span>Buscar por zona</span>
      <input v-model="zona" type="search" placeholder="Escribí una zona" />
    </label>
    <label class="bandeja-filtro">
      <span>Gravedad</span>
      <select v-model="gravedad">
        <option value="">Todas</option>
        <option value="ALTA">Alta</option>
        <option value="MEDIA">Media</option>
        <option value="BAJA">Baja</option>
      </select>
    </label>
    <label class="bandeja-filtro">
      <span>Tipo de desastre</span>
      <select v-model="tipo">
        <option value="">Todos</option>
        <option value="INUNDACION">Inundación</option>
        <option value="INCENDIO">Incendio</option>
        <option value="TERREMOTO">Terremoto</option>
      </select>
    </label>
    <label class="bandeja-filtro">
      <span>Ordenar por</span>
      <select v-model="orden">
        <option value="gravedad-desc">Mayor gravedad</option>
        <option value="gravedad-asc">Menor gravedad</option>
        <option value="id-desc">Más recientes</option>
        <option value="id-asc">Menos recientes</option>
      </select>
    </label>
    <button type="button" class="bandeja-actualizar" @click="$emit('limpiar')">Restablecer</button>
  </div>
</template>
