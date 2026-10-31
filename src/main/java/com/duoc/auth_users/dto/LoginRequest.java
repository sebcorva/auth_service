package com.duoc.auth_users.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
                @NotBlank(message = "El email no puede estar vacio") String email,
                @NotBlank(message = "La contraseña no puede estar vacia") String password) {

}
