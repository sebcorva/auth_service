package com.duoc.auth_users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.auth_users.dto.PagoPremiumResponse;
import com.duoc.auth_users.dto.PagoResponse;
import com.duoc.auth_users.security.UsuarioAutenticado;
import com.duoc.auth_users.service.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/premium")
    public ResponseEntity<PagoPremiumResponse> comprarPremium(
            @AuthenticationPrincipal UsuarioAutenticado usuarioActual) {
        PagoPremiumResponse respuesta = pagoService.comprarPremium(usuarioActual.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> getMisPagos(
            @AuthenticationPrincipal UsuarioAutenticado usuarioActual) {
        List<PagoResponse> pagos = pagoService.getPagosByUsuario(usuarioActual.id());
        return ResponseEntity.ok(pagos);
    }

}
