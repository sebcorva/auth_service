package com.duoc.auth_users.dto;

// Comprobante del pago + sesion nueva (el token anterior aun dice premium=false)
public record PagoPremiumResponse(
        PagoResponse pago,
        LoginResponse sesion) {
}
