package com.duoc.auth_users.dto;

import java.time.LocalDateTime;

import com.duoc.auth_users.model.Pago;

public record PagoResponse(
        Long id,
        int monto,
        LocalDateTime fecha) {

    public static PagoResponse from(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMonto(),
                pago.getFecha());
    }

}
