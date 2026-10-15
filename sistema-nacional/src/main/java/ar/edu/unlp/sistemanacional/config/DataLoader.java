package ar.edu.unlp.sistemanacional.config;

import ar.edu.unlp.sistemanacional.entity.*;
import ar.edu.unlp.sistemanacional.entity.enums.CategoriaRecurso;
import ar.edu.unlp.sistemanacional.entity.enums.EstadoOng;
import ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia;
import ar.edu.unlp.sistemanacional.entity.enums.Rol;
import ar.edu.unlp.sistemanacional.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static ar.edu.unlp.sistemanacional.entity.enums.NivelCompetencia.*;

// Carga el catálogo y datos de prueba la primera vez (base vacía).
// "ONG 1" y "ONG 2" tienen los mismos CUIT que se cargan en la app local de RescueSync.
// Las demás ONGs están para que la validación devuelva perfiles distintos
// (certificación vencida, ONG pendiente, ONG suspendida, etc.).
@Component
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final OngRepository ongRepository;
    private final CertificacionRepository certificacionRepository;
    private final TipoRecursoRepository tipoRecursoRepository;
    private final OngCertificacionRepository ongCertificacionRepository;
    private final RecursoInventarioRepository inventarioRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminPassword;
    private final String sistemaPassword;
    private final String ongPassword;

    private final Map<String, Certificacion> certificaciones = new HashMap<>();
    private final Map<String, TipoRecurso> tipos = new HashMap<>();

    public DataLoader(UsuarioRepository usuarioRepository, OngRepository ongRepository,
                      CertificacionRepository certificacionRepository, TipoRecursoRepository tipoRecursoRepository,
                      OngCertificacionRepository ongCertificacionRepository,
                      RecursoInventarioRepository inventarioRepository, PasswordEncoder passwordEncoder,
                      @Value("${seed.admin-password}") String adminPassword,
                      @Value("${seed.sistema-password}") String sistemaPassword,
                      @Value("${seed.ong-password}") String ongPassword) {
        this.usuarioRepository = usuarioRepository;
        this.ongRepository = ongRepository;
        this.certificacionRepository = certificacionRepository;
        this.tipoRecursoRepository = tipoRecursoRepository;
        this.ongCertificacionRepository = ongCertificacionRepository;
        this.inventarioRepository = inventarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.sistemaPassword = sistemaPassword;
        this.ongPassword = ongPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        cargarCatalogo();

        usuario("admin", adminPassword, Rol.ADMIN, null);
        usuario("bonita", sistemaPassword, Rol.SISTEMA, null);   // cuenta de servicio de RescueSync/Bonita

        LocalDate enUnAnio = LocalDate.now().plusYears(1);
        LocalDate vencida = LocalDate.now().minusMonths(2);

        // ONG 1: grande, bien certificada (en la app local es "ONG 1")
        Ong ong1 = ong("30712345671", "ONG 1", "contacto@ong1.org.ar", "Buenos Aires", EstadoOng.HABILITADA);
        certificar(ong1, "PRIMEROS_AUXILIOS", AVANZADO, enUnAnio);
        certificar(ong1, "RESCATE_ACUATICO", INTERMEDIO, enUnAnio);
        certificar(ong1, "LOGISTICA_HUMANITARIA", AVANZADO, enUnAnio);
        stock(ong1, "PARAMEDICO", 20);
        stock(ong1, "KIT_MEDICO", 100);
        stock(ong1, "RESCATISTA_ACUATICO", 8);
        stock(ong1, "BOTE_RESCATE", 3);
        stock(ong1, "RACION_ALIMENTO", 5000);
        stock(ong1, "AGUA_POTABLE", 3000);
        usuario("ong1", ongPassword, Rol.ONG, ong1);

        // ONG 2: fuerte en incendios, básica en primeros auxilios (en la app local es "ONG 2")
        Ong ong2 = ong("30700000016", "ONG 2", "contacto@ong2.org.ar", "Buenos Aires", EstadoOng.HABILITADA);
        certificar(ong2, "PRIMEROS_AUXILIOS", BASICO, enUnAnio);
        certificar(ong2, "COMBATE_INCENDIOS", AVANZADO, enUnAnio);
        stock(ong2, "PARAMEDICO", 6);
        stock(ong2, "BOMBERO_FORESTAL", 25);
        stock(ong2, "VOLUNTARIO_LOGISTICA", 40);
        stock(ong2, "CARPA", 50);
        stock(ong2, "RACION_ALIMENTO", 1500);
        usuario("ong2", ongPassword, Rol.ONG, ong2);

        // Brigada Andina: rescate en estructuras; su certificación de primeros auxilios está VENCIDA
        Ong andina = ong("30698765433", "Brigada Andina", "info@brigadaandina.org.ar", "Mendoza", EstadoOng.HABILITADA);
        certificar(andina, "BUSQUEDA_RESCATE_ESTRUCTURAS", AVANZADO, enUnAnio);
        certificar(andina, "PRIMEROS_AUXILIOS", INTERMEDIO, vencida);
        stock(andina, "RESCATISTA_USAR", 15);
        stock(andina, "PARAMEDICO", 4);
        usuario("brigada-andina", ongPassword, Rol.ONG, andina);

        // Red Solidaria del Litoral: inundaciones
        Ong litoral = ong("30711223343", "Red Solidaria del Litoral", "red@litoral.org.ar", "Santa Fe", EstadoOng.HABILITADA);
        certificar(litoral, "RESCATE_ACUATICO", AVANZADO, enUnAnio);
        certificar(litoral, "LOGISTICA_HUMANITARIA", INTERMEDIO, enUnAnio);
        stock(litoral, "RESCATISTA_ACUATICO", 12);
        stock(litoral, "BOTE_RESCATE", 6);
        stock(litoral, "RACION_ALIMENTO", 2000);
        stock(litoral, "AGUA_POTABLE", 5000);
        usuario("red-litoral", ongPassword, Rol.ONG, litoral);

        // Manos del Delta: recién registrada, todavía PENDIENTE
        Ong delta = ong("30705556667", "Manos del Delta", "hola@manosdeldelta.org.ar", "Entre Ríos", EstadoOng.PENDIENTE);
        stock(delta, "VOLUNTARIO_LOGISTICA", 10);
        usuario("manos-delta", ongPassword, Rol.ONG, delta);

        // Fundación Refugio: SUSPENDIDA
        Ong refugio = ong("30709876542", "Fundación Refugio", "contacto@refugio.org.ar", "Córdoba", EstadoOng.SUSPENDIDA);
        refugio.cambiarEstado(EstadoOng.SUSPENDIDA, "Documentación vencida");   // con motivo
        certificar(refugio, "LOGISTICA_HUMANITARIA", BASICO, enUnAnio);
        stock(refugio, "CARPA", 30);
        usuario("refugio", ongPassword, Rol.ONG, refugio);
    }

    private void cargarCatalogo() {
        cert("PRIMEROS_AUXILIOS", "Primeros auxilios y atención prehospitalaria",
                "Atención médica de urgencia y traslado de heridos en zona de desastre.");
        cert("RESCATE_ACUATICO", "Rescate acuático",
                "Rescate y evacuación de personas en inundaciones y cursos de agua.");
        cert("COMBATE_INCENDIOS", "Combate de incendios forestales",
                "Control y extinción de incendios forestales y de interfase.");
        cert("BUSQUEDA_RESCATE_ESTRUCTURAS", "Búsqueda y rescate en estructuras colapsadas",
                "Búsqueda, localización y extracción de víctimas en derrumbes (USAR).");
        cert("LOGISTICA_HUMANITARIA", "Logística y distribución humanitaria",
                "Almacenamiento, cadena de frío y distribución de alimentos e insumos.");

        tipo("PARAMEDICO", "Paramédicos", CategoriaRecurso.PERSONAL, "persona", "PRIMEROS_AUXILIOS");
        tipo("RESCATISTA_ACUATICO", "Rescatistas acuáticos", CategoriaRecurso.PERSONAL, "persona", "RESCATE_ACUATICO");
        tipo("BOMBERO_FORESTAL", "Bomberos forestales", CategoriaRecurso.PERSONAL, "persona", "COMBATE_INCENDIOS");
        tipo("RESCATISTA_USAR", "Rescatistas en estructuras", CategoriaRecurso.PERSONAL, "persona", "BUSQUEDA_RESCATE_ESTRUCTURAS");
        tipo("VOLUNTARIO_LOGISTICA", "Voluntarios de logística", CategoriaRecurso.PERSONAL, "persona", null);
        tipo("RACION_ALIMENTO", "Raciones de alimento", CategoriaRecurso.INSUMO, "ración", "LOGISTICA_HUMANITARIA");
        tipo("AGUA_POTABLE", "Agua potable", CategoriaRecurso.INSUMO, "litro", null);
        tipo("KIT_MEDICO", "Kits médicos", CategoriaRecurso.INSUMO, "kit", "PRIMEROS_AUXILIOS");
        tipo("BOTE_RESCATE", "Botes de rescate", CategoriaRecurso.EQUIPO, "unidad", "RESCATE_ACUATICO");
        tipo("CARPA", "Carpas", CategoriaRecurso.EQUIPO, "unidad", null);
    }

    private void cert(String codigo, String nombre, String descripcion) {
        certificaciones.put(codigo, certificacionRepository.save(new Certificacion(codigo, nombre, descripcion)));
    }

    private void tipo(String codigo, String nombre, CategoriaRecurso categoria, String unidad, String certificacion) {
        Certificacion requerida = certificacion == null ? null : certificaciones.get(certificacion);
        tipos.put(codigo, tipoRecursoRepository.save(new TipoRecurso(codigo, nombre, categoria, unidad, requerida)));
    }

    private Ong ong(String cuit, String nombre, String email, String provincia, EstadoOng estado) {
        Ong ong = new Ong(cuit, nombre, email, provincia);
        ong.cambiarEstado(estado, null);
        return ongRepository.save(ong);
    }

    private void certificar(Ong ong, String certificacion, NivelCompetencia nivel, LocalDate vigenteHasta) {
        ongCertificacionRepository.save(new OngCertificacion(ong, certificaciones.get(certificacion), nivel, vigenteHasta));
    }

    private void stock(Ong ong, String tipoRecurso, int cantidad) {
        inventarioRepository.save(new RecursoInventario(ong, tipos.get(tipoRecurso), cantidad));
    }

    private void usuario(String username, String password, Rol rol, Ong ong) {
        usuarioRepository.save(new Usuario(username, passwordEncoder.encode(password), rol, ong));
    }
}
