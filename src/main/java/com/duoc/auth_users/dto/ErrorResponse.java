package com.duoc.auth_users.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String mensaje,
        List<String> detalles,
        LocalDateTime timestamp) {

    public ErrorResponse(int status, String mensaje) {
        this(status, mensaje, List.of(), LocalDateTime.now());
    }

    public ErrorResponse(int status, String mensaje, List<String> detalles) {
        this(status, mensaje, detalles, LocalDateTime.now());
    }

}
