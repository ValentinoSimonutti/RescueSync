package ar.edu.unlp.sistemanacional;

import ar.edu.unlp.sistemanacional.config.ZonaHorariaConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Las fechas tienen que salir en hora argentina (GMT-3) aunque la JVM arranque en UTC.
// Surefire corre los tests con -Duser.timezone=UTC para simular Docker/Render.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class ZonaHorariaTest {

    @Autowired
    MockMvc mvc;

    @Test
    void laJvmQuedaEnHoraArgentina() {
        assertThat(ZoneId.systemDefault()).isEqualTo(ZonaHorariaConfig.ZONA);
    }

    @Test
    void fechaAltaDeUnaOngSaleEnHoraArgentina() throws Exception {
        String json = mvc.perform(post("/api/ongs/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cuit":"20-12345678-6","nombre":"ONG Horario","email":"a@b.com","provincia":"La Pampa",
                         "username":"horario","password":"secreta"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        LocalDateTime fechaAlta = LocalDateTime.parse(JsonPath.read(json, "$.fechaAlta"));
        LocalDateTime ahoraEnArgentina = LocalDateTime.now(ZonaHorariaConfig.ZONA);
        assertThat(Duration.between(fechaAlta, ahoraEnArgentina).abs()).isLessThan(Duration.ofMinutes(1));
    }
}
