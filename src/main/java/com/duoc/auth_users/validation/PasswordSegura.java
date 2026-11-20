package com.duoc.auth_users.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Contraseña segura: 8 a 20 caracteres, mayusculas y minusculas, al menos un numero,
 * al menos un caracter especial y sin espacios. Cada regla que no se cumple genera
 * su propio mensaje. Un valor null o vacio se considera valido: la obligatoriedad
 * se controla con @NotBlank.
 */
@Documented
@Constraint(validatedBy = PasswordSeguraValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSegura {

    String message() default "La contraseña no es segura";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
