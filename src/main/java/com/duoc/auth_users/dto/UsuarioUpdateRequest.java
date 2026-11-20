package com.duoc.auth_users.dto;

import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.validation.PasswordSegura;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body del PUT /usuarios/{id}: igual que UsuarioRequest, pero la contraseña es opcional.
// Si viene vacia o null se conserva la actual; si viene, debe cumplir @PasswordSegura.
public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre no puede estar vacio") @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @NotBlank(message = "El apellido no puede estar vacio") @Size(max = 100, message = "El apellido no puede superar los 100 caracteres") String apellido,
        @NotBlank(message = "El email no puede estar vacio") @Email(message = "El email no tiene un formato valido") @Size(max = 150, message = "El email no puede superar los 150 caracteres") String email,
        @PasswordSegura String password,
        Rol rol) {

    public boolean cambiaPassword() {
        return password != null && !password.isEmpty();
    }
}
