package com.duoc.auth_users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.duoc.auth_users.validation.PasswordSegura;

public record RegistroRequest(

        @NotBlank(message = "El nombre no puede estar vacio") @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,

        @NotBlank(message = "El apellido no puede estar vacio") @Size(max = 100, message = "El apellido no puede superar los 100 caracteres") String apellido,

        @NotBlank(message = "El email no puede estar vacio") @Email(message = "El email no tiene un formato valido") @Size(max = 150, message = "El email no puede superar los 150 caracteres") String email,

        @NotBlank(message = "La contraseña no puede estar vacia") @PasswordSegura String password) {

}
