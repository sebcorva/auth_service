package com.duoc.auth_users.dto;

import com.duoc.auth_users.validation.PasswordSegura;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerRequest(
        @NotBlank(message = "El email no puede estar vacio") @Email(message = "El email no tiene un formato valido") String email,
        @NotBlank(message = "El codigo no puede estar vacio") @Pattern(regexp = "\\d{6}", message = "El codigo debe tener 6 digitos") String codigo,
        @NotBlank(message = "La contraseña no puede estar vacia") @PasswordSegura String nuevaPassword) {
}
