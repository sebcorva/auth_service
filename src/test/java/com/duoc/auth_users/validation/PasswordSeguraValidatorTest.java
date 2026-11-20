package com.duoc.auth_users.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.duoc.auth_users.dto.RegistroRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PasswordSeguraValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> erroresDe(String password) {
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(
                new RegistroRequest("Ana", "Perez", "ana@test.cl", password));
        return violaciones.stream()
                .filter(v -> v.getPropertyPath().toString().equals("password"))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Admin2026!", "Premium2026!", "Usuario2026!", "aB3$efgh", "Xy9#Xy9#Xy9#Xy9#Xy9#" })
    void aceptaContraseñasQueCumplenLasSeisReglas(String password) {
        assertThat(erroresDe(password)).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Ab1!xyz             | La contraseña debe tener al menos 8 caracteres",
            "Ab1!xyzAb1!xyzAb1!xy1 | La contraseña no puede superar los 20 caracteres",
            "admin2026!          | La contraseña debe combinar mayusculas y minusculas",
            "ADMIN2026!          | La contraseña debe combinar mayusculas y minusculas",
            "AdminAdmin!         | La contraseña debe incluir al menos un numero",
            "Admin20266          | La contraseña debe incluir al menos un caracter especial",
            "'Admin 2026!'       | La contraseña no puede contener espacios"
    })
    void cadaReglaIncumplidaTieneSuMensaje(String password, String mensajeEsperado) {
        assertThat(erroresDe(password)).contains(mensajeEsperado);
    }

    @Test
    void reportaTodasLasReglasIncumplidasALaVez() {
        assertThat(erroresDe("abc")).containsExactlyInAnyOrder(
                "La contraseña debe tener al menos 8 caracteres",
                "La contraseña debe combinar mayusculas y minusculas",
                "La contraseña debe incluir al menos un numero",
                "La contraseña debe incluir al menos un caracter especial");
    }

    @Test
    void vaciaSoloReportaQueEsObligatoria() {
        assertThat(erroresDe("")).containsExactly("La contraseña no puede estar vacia");
    }
}
