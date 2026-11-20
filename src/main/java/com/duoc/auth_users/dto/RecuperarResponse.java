package com.duoc.auth_users.dto;

// codigoDemo simula el correo de recuperacion: en un sistema real el codigo se enviaria
// por email y NO vendria en la respuesta. Es null si el email no esta registrado.
public record RecuperarResponse(
        String mensaje,
        String codigoDemo,
        int minutosVigencia) {
}
