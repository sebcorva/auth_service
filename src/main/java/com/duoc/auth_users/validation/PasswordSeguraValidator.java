package com.duoc.auth_users.validation;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 20;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        List<String> errores = errores(password);
        if (errores.isEmpty()) {
            return true;
        }

        // Un mensaje por regla incumplida, en vez del mensaje generico
        context.disableDefaultConstraintViolation();
        for (String error : errores) {
            context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        }
        return false;
    }

    static List<String> errores(String password) {
        List<String> errores = new ArrayList<>();
        if (password.length() < LONGITUD_MINIMA) {
            errores.add("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (password.length() > LONGITUD_MAXIMA) {
            errores.add("La contraseña no puede superar los " + LONGITUD_MAXIMA + " caracteres");
        }
        if (!password.chars().anyMatch(Character::isUpperCase) || !password.chars().anyMatch(Character::isLowerCase)) {
            errores.add("La contraseña debe combinar mayusculas y minusculas");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            errores.add("La contraseña debe incluir al menos un numero");
        }
        if (!password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            errores.add("La contraseña debe incluir al menos un caracter especial");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            errores.add("La contraseña no puede contener espacios");
        }
        return errores;
    }
}
