package ar.edu.unlp.rescuesync.sistemanacional;

import ar.edu.unlp.rescuesync.entity.enums.EstadoEmergencia;
import ar.edu.unlp.rescuesync.repository.EmergenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// El plan gratuito de Render duerme la API del Sistema Nacional a los 15 minutos sin pedidos,
// y despertarla tarda alrededor de un minuto. Si eso pasa justo cuando vence la ventana de ofertas,
// el conector de Bonita puede cortar por timeout y el caso queda trabado.
//
// Por eso, mientras haya emergencias con el proceso en marcha, el backend le hace un ping cada
// 10 minutos. Al arrancar también le hace uno, para que ya esté despierta cuando empiece la prueba.
@Component
public class MantenerDespierto {

    private static final Logger log = LoggerFactory.getLogger(MantenerDespierto.class);

    // Estados en los que todavía falta alguna llamada de Bonita al Sistema Nacional
    private static final List<EstadoEmergencia> EN_MARCHA = List.of(
            EstadoEmergencia.REGISTRADA, EstadoEmergencia.EN_CONVOCATORIA, EstadoEmergencia.EN_EVALUACION,
            EstadoEmergencia.ADJUDICADA, EstadoEmergencia.EN_EJECUCION);

    private final SistemaNacionalClient sistemaNacional;
    private final EmergenciaRepository emergenciaRepository;
    private boolean primeraVez = true;

    public MantenerDespierto(SistemaNacionalClient sistemaNacional, EmergenciaRepository emergenciaRepository) {
        this.sistemaNacional = sistemaNacional;
        this.emergenciaRepository = emergenciaRepository;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 10 * 60_000)
    public void ping() {
        if (!primeraVez && !emergenciaRepository.existsByEstadoIn(EN_MARCHA)) {
            return;
        }
        primeraVez = false;
        boolean ok = sistemaNacional.despertar();
        log.info("Ping al Sistema Nacional ({}): {}", sistemaNacional.getBaseUrl(), ok ? "responde" : "no respondió");
    }
}
