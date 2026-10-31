package com.duoc.auth_users.security;

public record UsuarioAutenticado(
                Long id,
                String email,
                String rol,
                boolean premium) {
}
