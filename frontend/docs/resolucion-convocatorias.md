# Integración pendiente: convocatorias vencidas

El frontend consulta GET /api/emergencias/{id}/cobertura. El detalle desplegable y el diagnóstico funcionan con esa consulta. No se agregaron llamadas a endpoints inexistentes.

## Reapertura

Propuesta de endpoint (todavía no implementado):
POST /api/emergencias/{id}/reabrir

Cuerpo propuesto:
{
  "tareaId": 123,
  "duracionMinutos": 10,
  "lotes": [{"recurso": "Agua", "cantidad": 20}]
}

- Agregar la ruta en controllers/EmergenciaController.java y la operación en service/EmergenciaService.java.
- Verificar rol CCR, emergencia y tarea correspondiente, ventana vencida y estado que permita resolverla.
- Validar recursos no vacíos, cantidades enteras entre 1 y 100 y duración positiva en el servidor.
- Definir rondas: preservar el historial de lotes y ofertas de la ronda anterior. No borrar lotes que tengan ofertas asociadas ni trasladar ofertas automáticamente a necesidades editadas.
- Calcular el nuevo cierre desde el servidor y sincronizarlo con el nuevo timer en Bonita.
- Completar la tarea alternativa, no la tarea inicial «Generar lotes». Revisar su contrato y transición en el proceso desplegado.
- Evitar duplicar rondas si se reintenta una petición y contemplar fallos entre base de datos y Bonita.

No reutilizar POST /emergencias/{id}/lotes: actualmente solo admite emergencias REGISTRADAS.

En el frontend, GenerarLotesForm recibe lotesIniciales y reapertura. Falta conectar esta operación en publicar(), quitar el bloqueo exclusivo de reapertura y propagar publicado desde ResolverConvocatoria a App.

La duración propuesta comienza en 2 minutos como el formulario original: la consulta de cobertura no contiene la duración original, por lo que no se presenta como dato recuperado.

## Aceptación parcial

Propuesta:
POST /api/emergencias/{id}/aceptar-parcial
Cuerpo: {"tareaId": 123}

- Agregar controlador y servicio con validación de rol, tarea y estado.
- Recalcular cobertura en el servidor. Exigir al menos un aporte válido y cobertura incompleta.
- Registrar la decisión sin inventar cobertura completa ni marcar recursos como entregados.
- Si aceptar implica elegir ofertas concretas, el contrato debe incluir sus identificadores y versiones y la pantalla debe permitir seleccionarlas.
- Revisar Bonita: se necesita una decisión y una transición específica para continuar con cobertura parcial. Completar una tarea que siempre vuelve a publicar no alcanza.
- Conservar la adjudicación en la etapa y el actor previstos por el proceso.

En ResolverConvocatoria falta habilitar el botón cuando el servidor permita la operación, agregar confirmación y enviar la decisión, manteniendo la pantalla y los datos ante errores. Al completar, refrescar la bandeja.

## Verificación integrada

1. Vencimiento sin ofertas: informa faltantes y no permite aceptar parcialmente.
2. Cobertura parcial: refleja cantidades del servidor, permite la decisión una sola vez.
3. Reapertura: precarga cantidades originales, permite editar, inicia otro plazo real y conserva historial.
4. Error de Bonita o del servidor: no muestra éxito ni pierde el borrador.
5. Reintento o tarea ya resuelta desde otra sesión: no duplica convocatorias.
