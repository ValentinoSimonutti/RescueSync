package ar.edu.unlp.sistemanacional.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// El CUIT se acepta con o sin guiones ("30-71234567-1" o "30712345671") y se guarda solo con dígitos.
public final class Cuit {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private Cuit() {
    }

    // Devuelve los 11 dígitos o tira 400 si el CUIT no es válido (formato o dígito verificador)
    public static String normalizar(String cuit) {
        String digitos = cuit == null ? "" : cuit.replaceAll("[\\s-]", "");
        if (!digitos.matches("\\d{11}") || !digitoVerificadorOk(digitos)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CUIT inválido: " + cuit);
        }
        return digitos;
    }

    private static boolean digitoVerificadorOk(String digitos) {
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (digitos.charAt(i) - '0') * PESOS[i];
        }
        int resto = 11 - (suma % 11);
        int esperado = resto == 11 ? 0 : (resto == 10 ? 9 : resto);
        return esperado == digitos.charAt(10) - '0';
    }
}
