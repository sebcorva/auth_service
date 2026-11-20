package com.duoc.auth_users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarRequest(
        @NotBlank(message = "El email no puede estar vacio") @Email(message = "El email no tiene un formato valido") String email) {
}
