package ar.edu.unlp.rescuesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // para mantener despierto al Sistema Nacional en Render (ver MantenerDespierto)
public class RescuesyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(RescuesyncApplication.class, args);
	}

}
