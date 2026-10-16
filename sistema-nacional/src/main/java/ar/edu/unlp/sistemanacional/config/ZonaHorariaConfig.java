package ar.edu.unlp.sistemanacional.config;

import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

// Todas las fechas se generan con LocalDateTime.now() / LocalDate.now() y se guardan en columnas
// "timestamp" sin zona, así que dependen de la zona por defecto de la JVM. En Docker y en Render
// esa zona es UTC: sin esto, la API grabaría las horas 3 horas adelantadas y una certificación
// podría vencer a las 21 hs. Se fija acá (y no con TZ del entorno) para que valga en cualquier
// lado, incluidos los tests. El bloque static corre al cargar la clase, antes del DataLoader.
@Configuration
public class ZonaHorariaConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA));
    }
}
