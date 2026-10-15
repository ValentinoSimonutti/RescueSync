package ar.edu.unlp.sistemanacional;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Milestone 1: login JWT, permisos por rol, onboarding y consultas del catálogo.
// Usa los datos del DataLoader. @Transactional: cada test deshace sus cambios.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class AuthYOngsTest {

    @Autowired
    MockMvc mvc;

    private String login(String usuario, String password) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    // ---------- Autenticación ----------

    @Test
    void loginDevuelveTokenConRolYCuit() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ong1\",\"password\":\"ong123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ONG"))
                .andExpect(jsonPath("$.ongCuit").value("30712345671"));
    }

    @Test
    void loginConContraseniaIncorrectaDa401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ong1\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/ongs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/ongs").header("Authorization", "Bearer esto.no.es-un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Token inválido"));
    }

    @Test
    void rolOngNoPuedeHabilitarOngs() throws Exception {
        mvc.perform(patch("/api/ongs/30705556667/estado").header("Authorization", login("ong1", "ong123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"HABILITADA\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- Onboarding y gestión ----------

    @Test
    void onboardingCreaOngPendienteYSuUsuarioPuedeEntrar() throws Exception {
        mvc.perform(post("/api/ongs/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cuit":"30-71234567-1","nombre":"x","email":"a@b.com","provincia":"BA",
                         "username":"nueva","password":"secreta"}"""))
                .andExpect(status().isConflict());   // ese CUIT ya es de ONG 1

        mvc.perform(post("/api/ongs/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cuit":"20-12345678-6","nombre":"ONG Nueva","email":"a@b.com","provincia":"La Pampa",
                         "username":"nueva","password":"secreta"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuit").value("20123456786"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        mvc.perform(get("/api/ongs/me").header("Authorization", login("nueva", "secreta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ONG Nueva"));
    }

    @Test
    void onboardingValidaCuitYCampos() throws Exception {
        mvc.perform(post("/api/ongs/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cuit":"30-71234567-9","nombre":"x","email":"a@b.com","provincia":"BA",
                         "username":"otra","password":"secreta"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("CUIT inválido")));

        mvc.perform(post("/api/ongs/registro").contentType(MediaType.APPLICATION_JSON).content("{\"cuit\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void adminHabilitaYCertifica() throws Exception {
        String admin = login("admin", "admin123");

        mvc.perform(patch("/api/ongs/30705556667/estado").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"HABILITADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("HABILITADA"));

        mvc.perform(post("/api/ongs/30705556667/certificaciones").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificacion\":\"RESCATE_ACUATICO\",\"nivel\":\"BASICO\",\"vigenteHasta\":\"2030-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificaciones[0].codigo").value("RESCATE_ACUATICO"))
                .andExpect(jsonPath("$.certificaciones[0].vigente").value(true));
    }

    @Test
    void ongActualizaSuInventario() throws Exception {
        mvc.perform(put("/api/ongs/me/inventario/CARPA").header("Authorization", login("ong1", "ong123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidadTotal\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoRecurso").value("CARPA"))
                .andExpect(jsonPath("$.disponible").value(12));
    }

    // ---------- Catálogo y consulta ----------

    @Test
    void filtroPorCertificacionIgnoraLasVencidas() throws Exception {
        // Brigada Andina tiene PRIMEROS_AUXILIOS vencida: no aparece
        mvc.perform(get("/api/ongs").param("certificacion", "PRIMEROS_AUXILIOS")
                        .header("Authorization", login("bonita", "bonita123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("ONG 1", "ONG 2")));
    }

    @Test
    void disponibilidadGlobalSoloCuentaOngsHabilitadas() throws Exception {
        // PARAMEDICO: ONG 1 (20) + ONG 2 (6) + Brigada Andina (4)
        mvc.perform(get("/api/recursos/disponibilidad").param("tipoRecurso", "PARAMEDICO")
                        .header("Authorization", login("bonita", "bonita123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(30))
                .andExpect(jsonPath("$[0].porOng", hasSize(3)));

        // CARPA: Fundación Refugio está suspendida, solo cuenta ONG 2 (50)
        mvc.perform(get("/api/recursos/disponibilidad").param("tipoRecurso", "CARPA")
                        .header("Authorization", login("bonita", "bonita123")))
                .andExpect(jsonPath("$[0].disponible").value(50));
    }

    @Test
    void catalogoDeTiposYCertificaciones() throws Exception {
        String token = login("ong2", "ong123");
        mvc.perform(get("/api/catalogo/tipos-recurso").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));
        mvc.perform(get("/api/catalogo/certificaciones").header("Authorization", token))
                .andExpect(jsonPath("$[?(@.codigo=='RESCATE_ACUATICO')].habilitaRecursos[*]",
                        containsInAnyOrder("RESCATISTA_ACUATICO", "BOTE_RESCATE")));
    }
}
