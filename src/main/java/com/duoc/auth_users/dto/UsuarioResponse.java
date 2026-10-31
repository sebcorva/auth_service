package com.duoc.auth_users.dto;

import java.time.LocalDateTime;

import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol,
        boolean premium,
        LocalDateTime fechaRegistro) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isPremium(),
                usuario.getFechaRegistro());
    }

}
