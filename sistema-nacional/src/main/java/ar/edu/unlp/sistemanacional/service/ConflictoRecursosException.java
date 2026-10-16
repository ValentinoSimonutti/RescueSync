package ar.edu.unlp.sistemanacional.service;

import ar.edu.unlp.sistemanacional.dto.ItemVerificacion;

import java.util.List;

// No se pudo comprometer: algún aporte no cumple las restricciones (disponibilidad, habilitación).
// Se responde 409 con el detalle de cada aporte y no se bloquea nada (todo o nada).
public class ConflictoRecursosException extends RuntimeException {

    private final List<ItemVerificacion> items;

    public ConflictoRecursosException(List<ItemVerificacion> items) {
        super("No se pudieron comprometer los recursos: "
                + items.stream().filter(i -> !i.ok()).count() + " aporte(s) no cumplen las restricciones");
        this.items = items;
    }

    public List<ItemVerificacion> getItems() {
        return items;
    }
}
