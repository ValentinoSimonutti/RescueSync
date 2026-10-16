package ar.edu.unlp.sistemanacional;

import ar.edu.unlp.sistemanacional.dto.AporteRequest;
import ar.edu.unlp.sistemanacional.dto.CompromisoRequest;
import ar.edu.unlp.sistemanacional.dto.CompromisoResponse;
import ar.edu.unlp.sistemanacional.dto.OfertaRequest;
import ar.edu.unlp.sistemanacional.entity.RecursoInventario;
import ar.edu.unlp.sistemanacional.entity.enums.NivelRiesgo;
import ar.edu.unlp.sistemanacional.repository.RecursoInventarioRepository;
import ar.edu.unlp.sistemanacional.service.CompromisoService;
import ar.edu.unlp.sistemanacional.service.ConflictoRecursosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Control de concurrencia con Postgres real: varias emergencias compiten en paralelo por el mismo recurso.
// SIN @Transactional: cada hilo hace su propia transacción, como dos requests reales de Bonita.
@SpringBootTest
@Import(TestcontainersConfig.class)
class ConcurrenciaCompromisoTest {

    static final String ONG1 = "30712345671";   // tiene 3 BOTE_RESCATE

    @Autowired
    CompromisoService compromisoService;

    @Autowired
    RecursoInventarioRepository inventarioRepository;

    private final List<String> emergenciasUsadas = new CopyOnWriteArrayList<>();

    private static CompromisoRequest pedido(String emergencia, String oferta, int cantidad) {
        return new CompromisoRequest(emergencia, NivelRiesgo.BAJA, "test concurrencia",
                List.of(new OfertaRequest(oferta, null, "BOTE_RESCATE", null,
                        List.of(new AporteRequest(ONG1, cantidad)))));
    }

    private RecursoInventario botes() {
        Long id = inventarioRepository.findIdByCuitAndTipo(ONG1, "BOTE_RESCATE").orElseThrow();
        return inventarioRepository.findById(id).orElseThrow();
    }

    // Larga todas las tareas a la vez (con una "largada" común) y espera que terminen
    private void enParalelo(List<Callable<Void>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>();
        for (Callable<Void> tarea : tareas) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return tarea.call();
            }));
        }
        largada.countDown();
        for (Future<Void> f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
    }

    @AfterEach
    void liberarTodo() {
        emergenciasUsadas.forEach(e -> {
            try {
                compromisoService.cerrarProyecto(e);
            } catch (Exception ignorada) {
                // la emergencia no llegó a crear proyecto
            }
        });
        assertEquals(0, botes().getCantidadComprometida());
    }

    @Test
    void diezEmergenciasPeleanTresBotesYSoloTresGanan() throws Exception {
        int total = botes().getCantidadTotal();   // 3
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();

        List<Callable<Void>> tareas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String emergencia = "CONC-" + i;
            emergenciasUsadas.add(emergencia);
            tareas.add(() -> {
                try {
                    compromisoService.comprometer(pedido(emergencia, "O-" + emergencia, 1));
                    exitos.incrementAndGet();
                } catch (ConflictoRecursosException e) {
                    rechazos.incrementAndGet();
                }
                return null;
            });
        }

        enParalelo(tareas);

        assertEquals(total, exitos.get(), "ganan tantas emergencias como botes hay");
        assertEquals(10 - total, rechazos.get());
        RecursoInventario botes = botes();
        assertEquals(total, botes.getCantidadComprometida(), "nunca se compromete más que el total");
        assertEquals(0, botes.getDisponible());
    }

    @Test
    void reintentosSimultaneosDeLaMismaOfertaSeCompromentenUnaVez() throws Exception {
        // Bonita reintenta el mismo request 5 veces a la vez (timeout de red, por ejemplo)
        emergenciasUsadas.add("CONC-REINTENTO");
        AtomicInteger creados = new AtomicInteger();

        List<Callable<Void>> tareas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tareas.add(() -> {
                CompromisoResponse r = compromisoService.comprometer(pedido("CONC-REINTENTO", "O-1", 2));
                creados.addAndGet(r.creados());
                return null;
            });
        }

        enParalelo(tareas);

        assertEquals(1, creados.get(), "solo el primero crea el compromiso, el resto lo encuentra");
        assertEquals(2, botes().getCantidadComprometida());
    }
}
