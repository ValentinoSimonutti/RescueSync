package ar.edu.unlp.sistemanacional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Sistema Nacional de Gestión de Recursos y Riesgos: la API externa (cloud) que
// valida ONGs, bloquea los recursos adjudicados y los libera al cerrar el proyecto.
@SpringBootApplication
public class SistemaNacionalApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaNacionalApplication.class, args);
	}

}
