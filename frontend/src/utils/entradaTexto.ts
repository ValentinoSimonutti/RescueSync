// Actualiza el valor real y conserva el cursor al editar dentro del texto.
export function primeraLetraMayuscula(evento: Event, actualizar: (valor: string) => void) {
  const campo = evento.target as HTMLInputElement | HTMLTextAreaElement;
  if ((evento as InputEvent).isComposing) return;
  const original = campo.value;
  let valor = original.replace(/\p{L}/u, letra => letra.toLocaleUpperCase("es"));
  if (campo.maxLength >= 0) valor = valor.slice(0, campo.maxLength);
  if (valor !== original) {
    const inicio = campo.selectionStart;
    const fin = campo.selectionEnd;
    campo.value = valor;
    if (inicio !== null && fin !== null) campo.setSelectionRange(inicio, fin);
  }
  actualizar(valor);
}
