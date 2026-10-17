package ar.edu.unlp.rescuesync.config;

import ar.edu.unlp.rescuesync.entity.enums.EstadoAporte;
import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.entity.enums.EstadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.ResultadoOferta;
import ar.edu.unlp.rescuesync.entity.enums.RolValidacion;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

// Hibernate crea cada columna enum con un CHECK que lista los valores que había al crear la tabla,
// y con ddl-auto: update no lo actualiza cuando se agrega un valor (ej. EstadoOferta.DESCARTADA).
// Al arrancar se rehacen esos CHECK con los valores actuales de cada enum.
@Component
@Order(0)
public class RestriccionesEnum implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public RestriccionesEnum(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        actualizar("oferta", "estado", EstadoOferta.class);
        actualizar("oferta", "resultado", ResultadoOferta.class);
        actualizar("oferta", "rol_validacion", RolValidacion.class);
        actualizar("aporte", "estado", EstadoAporte.class);
        actualizar("aporte", "rol_validacion", RolValidacion.class);
        actualizar("emergencia", "estado", EstadoEmergencia.class);
    }

    private void actualizar(String tabla, String columna, Class<? extends Enum<?>> tipo) {
        String valores = Arrays.stream(tipo.getEnumConstants())
                .map(v -> "'" + v.name() + "'")
                .collect(Collectors.joining(", "));
        String restriccion = tabla + "_" + columna + "_check";
        jdbc.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT IF EXISTS " + restriccion);
        jdbc.execute("ALTER TABLE " + tabla + " ADD CONSTRAINT " + restriccion
                + " CHECK (" + columna + " IN (" + valores + "))");
    }
}
