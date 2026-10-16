package ar.edu.unlp.sistemanacional;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Milestone 2: validación por niveles, compromiso todo-o-nada, idempotencia, liberación y cierre.
// Usa los datos del DataLoader (ONG 1 = 30712345671, ONG 2 = 30700000016, etc.).
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class ValidacionYCompromisoTest {

    static final String ONG1 = "30712345671";
    static final String ONG2 = "30700000016";
    static final String ANDINA = "30698765433";      // PRIMEROS_AUXILIOS vencida
    static final String DELTA = "30705556667";       // PENDIENTE
    static final String NO_REGISTRADA = "20123456786";

    @Autowired
    MockMvc mvc;

    private String token(String usuario, String password) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private ResultActions postJson(String url, String token, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String compromiso(String emergencia, String oferta, String cuit, String tipo, int cantidad) {
        return """
                {"emergenciaRef":"%s","gravedad":"ALTA","ofertas":[
                  {"ofertaRef":"%s","tipoRecurso":"%s","aportes":[{"cuit":"%s","cantidad":%d}]}]}
                """.formatted(emergencia, oferta, tipo, cuit, cantidad);
    }

    private int disponible(String token, String cuit, String tipo) throws Exception {
        String json = mvc.perform(get("/api/ongs/" + cuit).header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        List<Integer> valores = JsonPath.read(json, "$.inventario[?(@.tipoRecurso=='" + tipo + "')].disponible");
        return valores.get(0);
    }

    // ---------- Validación ----------

    @Test
    void validacionDevuelvePerfilesSinRechazoBinario() throws Exception {
        String bonita = token("bonita", "bonita123");
        postJson("/api/validaciones/ofertas", bonita, """
                {"emergenciaRef":"E-1","gravedad":"ALTA","ofertas":[
                  {"ofertaRef":"O-1","tipoRecurso":"PARAMEDICO","cantidadRequerida":30,"aportes":[
                    {"cuit":"%s","cantidad":50},
                    {"cuit":"%s","cantidad":5},
                    {"cuit":"%s","cantidad":2},
                    {"cuit":"%s","cantidad":3},
                    {"cuit":"%s","cantidad":1}]}]}
                """.formatted(ONG1, ONG2, ANDINA, DELTA, NO_REGISTRADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertas[0].rolOferta").value("PRINCIPAL"))
                // ONG 1: AVANZADO → PRINCIPAL, pero solo tiene 20 de los 50 que ofrece
                .andExpect(jsonPath("$.ofertas[0].aportes[0].rolAsignado").value("PRINCIPAL"))
                .andExpect(jsonPath("$.ofertas[0].aportes[0].nivelRiesgoMaximo").value("ALTA"))
                .andExpect(jsonPath("$.ofertas[0].aportes[0].cantidadValidable").value(20))
                .andExpect(jsonPath("$.ofertas[0].aportes[0].capacidadesHabilitadas", hasItem("BOTE_RESCATE")))
                // ONG 2: BASICO (riesgo BAJA) en una emergencia ALTA → apoyo
                .andExpect(jsonPath("$.ofertas[0].aportes[1].rolAsignado").value("APOYO_SECUNDARIO"))
                .andExpect(jsonPath("$.ofertas[0].aportes[1].nivelRiesgoMaximo").value("BAJA"))
                // Brigada Andina: certificación vencida → apoyo
                .andExpect(jsonPath("$.ofertas[0].aportes[2].rolAsignado").value("APOYO_SECUNDARIO"))
                .andExpect(jsonPath("$.ofertas[0].aportes[2].observaciones[0]", containsString("venció")))
                // Manos del Delta: pendiente → sin habilitación
                .andExpect(jsonPath("$.ofertas[0].aportes[3].rolAsignado").value("SIN_HABILITACION_VIGENTE"))
                .andExpect(jsonPath("$.ofertas[0].aportes[3].estadoOng").value("PENDIENTE"))
                // CUIT desconocido
                .andExpect(jsonPath("$.ofertas[0].aportes[4].estadoOng").value("NO_REGISTRADA"))
                // 20 + 5 + 2 = 27 validados de 30 requeridos
                .andExpect(jsonPath("$.ofertas[0].cantidadValidada").value(27))
                .andExpect(jsonPath("$.ofertas[0].cubreRequerido").value(false));
    }

    @Test
    void mismaOngEsPrincipalEnGravedadBaja() throws Exception {
        postJson("/api/validaciones/ofertas", token("bonita", "bonita123"), """
                {"emergenciaRef":"E-2","gravedad":"BAJA","ofertas":[
                  {"ofertaRef":"O-1","tipoRecurso":"PARAMEDICO","aportes":[{"cuit":"%s","cantidad":5}]},
                  {"ofertaRef":"O-2","tipoRecurso":"NO_EXISTE","aportes":[{"cuit":"%s","cantidad":5}]}]}
                """.formatted(ONG2, ONG2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertas[0].aportes[0].rolAsignado").value("PRINCIPAL"))
                .andExpect(jsonPath("$.ofertas[1].rolOferta").value("SIN_HABILITACION_VIGENTE"))
                .andExpect(jsonPath("$.ofertas[1].observaciones[0]", containsString("catálogo")));
    }

    @Test
    void rolOngNoPuedeValidarNiComprometer() throws Exception {
        String ong = token("ong1", "ong123");
        postJson("/api/validaciones/ofertas", ong, compromiso("E-3", "O-1", ONG1, "PARAMEDICO", 1))
                .andExpect(status().isForbidden());
        postJson("/api/compromisos", ong, compromiso("E-3", "O-1", ONG1, "PARAMEDICO", 1))
                .andExpect(status().isForbidden());
    }

    // ---------- Compromiso, liberación y cierre ----------

    @Test
    void cicloCompletoComprometerFinalizarCerrar() throws Exception {
        String bonita = token("bonita", "bonita123");
        int inicial = disponible(bonita, ONG1, "PARAMEDICO");   // 20

        // Verificar no bloquea nada
        postJson("/api/compromisos/verificar", bonita, compromiso("E-10", "O-1", ONG1, "PARAMEDICO", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factible").value(true));
        assertEquals(inicial, disponible(bonita, ONG1, "PARAMEDICO"));

        // Comprometer: bloquea 8
        postJson("/api/compromisos", bonita, compromiso("E-10", "O-1", ONG1, "PARAMEDICO", 8))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(1))
                .andExpect(jsonPath("$.compromisos[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$.compromisos[0].rolAsignado").value("PRINCIPAL"));
        assertEquals(inicial - 8, disponible(bonita, ONG1, "PARAMEDICO"));

        // Reintento del mismo request (Bonita): no vuelve a descontar
        postJson("/api/compromisos", bonita, compromiso("E-10", "O-1", ONG1, "PARAMEDICO", 8))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.yaExistian").value(1));
        assertEquals(inicial - 8, disponible(bonita, ONG1, "PARAMEDICO"));

        // Otra emergencia quiere más de lo que queda (12): 409 y no bloquea nada
        postJson("/api/compromisos", bonita, compromiso("E-11", "O-9", ONG1, "PARAMEDICO", 15))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.items[0].ok").value(false))
                .andExpect(jsonPath("$.items[0].disponible").value(inicial - 8));

        // La ONG ve sus compromisos
        mvc.perform(get("/api/compromisos").header("Authorization", token("ong1", "ong123")))
                .andExpect(jsonPath("$[?(@.emergenciaRef=='E-10')].cantidad", contains(8)));

        // La ONG terminó: se libera lo suyo
        mvc.perform(post("/api/proyectos/E-10/ongs/" + ONG1 + "/finalizacion").header("Authorization", bonita))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liberados", hasSize(1)))
                .andExpect(jsonPath("$.liberados[0].estado").value("LIBERADO"));
        assertEquals(inicial, disponible(bonita, ONG1, "PARAMEDICO"));

        // Cierre (idempotente) y después no admite más compromisos
        mvc.perform(post("/api/proyectos/E-10/cierre").header("Authorization", bonita))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoProyecto").value("CERRADO"))
                .andExpect(jsonPath("$.liberados", hasSize(0)));
        postJson("/api/compromisos", bonita, compromiso("E-10", "O-2", ONG1, "PARAMEDICO", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("cerrado")));
    }

    @Test
    void consorcioEsTodoONada() throws Exception {
        String bonita = token("bonita", "bonita123");
        int inicial = disponible(bonita, ONG1, "RACION_ALIMENTO");

        // ONG 1 tiene raciones; Manos del Delta (pendiente) no puede: no se bloquea nada de nadie
        postJson("/api/compromisos", bonita, """
                {"emergenciaRef":"E-20","gravedad":"MEDIA","ofertas":[
                  {"ofertaRef":"O-1","tipoRecurso":"RACION_ALIMENTO","aportes":[
                    {"cuit":"%s","cantidad":1000},{"cuit":"%s","cantidad":10}]}]}
                """.formatted(ONG1, DELTA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.items[0].ok").value(true))
                .andExpect(jsonPath("$.items[1].ok").value(false))
                .andExpect(jsonPath("$.items[1].motivo", containsString("PENDIENTE")));
        assertEquals(inicial, disponible(bonita, ONG1, "RACION_ALIMENTO"));

        // Consorcio válido: ONG 1 (AVANZADO) + Red Solidaria del Litoral (INTERMEDIO → riesgo MEDIA)
        postJson("/api/compromisos", bonita, """
                {"emergenciaRef":"E-20","gravedad":"MEDIA","ofertas":[
                  {"ofertaRef":"O-1","tipoRecurso":"RACION_ALIMENTO","aportes":[
                    {"cuit":"%s","cantidad":1000},{"cuit":"30711223343","cantidad":500}]}]}
                """.formatted(ONG1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(2));

        mvc.perform(get("/api/proyectos/E-20").header("Authorization", bonita))
                .andExpect(jsonPath("$.estado").value("EN_EJECUCION"))
                .andExpect(jsonPath("$.compromisos", hasSize(2)));

        mvc.perform(post("/api/proyectos/E-20/cierre").header("Authorization", bonita))
                .andExpect(jsonPath("$.liberados", hasSize(2)));
        assertEquals(inicial, disponible(bonita, ONG1, "RACION_ALIMENTO"));
    }

    @Test
    void ongNoPuedeBajarSuInventarioPorDebajoDeLoComprometido() throws Exception {
        postJson("/api/compromisos", token("bonita", "bonita123"), compromiso("E-30", "O-1", ONG1, "KIT_MEDICO", 60))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/ongs/me/inventario/KIT_MEDICO").header("Authorization", token("ong1", "ong123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidadTotal\":50}"))
                .andExpect(status().isConflict());
    }
}
