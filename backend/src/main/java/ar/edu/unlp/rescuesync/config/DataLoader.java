package ar.edu.unlp.rescuesync.config;

import ar.edu.unlp.rescuesync.repository.CentroCoordinadorRepository;
import ar.edu.unlp.rescuesync.repository.MunicipioRepository;
import ar.edu.unlp.rescuesync.repository.ONGRepository;
import ar.edu.unlp.rescuesync.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ar.edu.unlp.rescuesync.entity.Municipio;
import ar.edu.unlp.rescuesync.entity.CentroCoordinador;
import ar.edu.unlp.rescuesync.entity.ONG;
import ar.edu.unlp.rescuesync.auth.Rol;
import ar.edu.unlp.rescuesync.auth.Usuario;

@Component
public class DataLoader implements CommandLineRunner {

    private final MunicipioRepository municipioRepository;
    private final CentroCoordinadorRepository centroCoordinadorRepository;
    private final ONGRepository ongRepository;
    private final UsuarioRepository usuarioRepository;

    public DataLoader(
            MunicipioRepository municipioRepository,
            CentroCoordinadorRepository centroCoordinadorRepository,
            ONGRepository ongRepository,
            UsuarioRepository usuarioRepository) {

        this.municipioRepository = municipioRepository;
        this.centroCoordinadorRepository = centroCoordinadorRepository;
        this.ongRepository = ongRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {

        if (usuarioRepository.count() == 0) {

            Municipio municipio = new Municipio("Municipio 1");

            CentroCoordinador centroCoordinador =
                    new CentroCoordinador("Centro Coordinador 1");

            ONG ong1 = new ONG("ONG 1");
            ONG ong2 = new ONG("ONG 2");

            municipioRepository.save(municipio);
            centroCoordinadorRepository.save(centroCoordinador);
            ongRepository.save(ong1);
            ongRepository.save(ong2);

            Usuario municipio1 = new Usuario(
                    "Municipio",
                    "Uno",
                    "municipio1@rescuesync.local",
                    "municipio1",
                    Rol.OPERADOR_MUNICIPAL,
                    municipio
            );

            Usuario coord1 = new Usuario(
                    "Coordinador",
                    "Uno",
                    "coord1@rescuesync.local",
                    "coord1",
                    Rol.CCR,
                    centroCoordinador
            );

            Usuario usuarioOng1 = new Usuario(
                    "ONG",
                    "Uno",
                    "ong1@rescuesync.local",
                    "ong1",
                    Rol.REPRESENTANTE_ONG,
                    ong1
            );

            Usuario usuarioOng2 = new Usuario(
                    "ONG",
                    "Dos",
                    "ong2@rescuesync.local",
                    "ong2",
                    Rol.REPRESENTANTE_ONG,
                    ong2
            );

            Usuario auditor1 = new Usuario(
                    "Auditor",
                    "Uno",
                    "auditor1@rescuesync.local",
                    "auditor1",
                    Rol.AUDITOR,
                    null
            );

            usuarioRepository.save(municipio1);
            usuarioRepository.save(coord1);
            usuarioRepository.save(usuarioOng1);
            usuarioRepository.save(usuarioOng2);
            usuarioRepository.save(auditor1);
        }
    }
}