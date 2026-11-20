package com.duoc.auth_users.controller;

import com.duoc.auth_users.dto.RecuperarRequest;
import com.duoc.auth_users.dto.RecuperarResponse;
import com.duoc.auth_users.dto.RestablecerRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.auth_users.dto.LoginRequest;
import com.duoc.auth_users.dto.LoginResponse;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.UsuarioResponse;
import com.duoc.auth_users.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse usuarioRegistrado = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioRegistrado);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(respuesta);
    }


    @PostMapping("/recuperar")
    public ResponseEntity<RecuperarResponse> recuperar(@Valid @RequestBody RecuperarRequest request) {
        return ResponseEntity.ok(authService.recuperar(request));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody RestablecerRequest request) {
        authService.restablecer(request);
        return ResponseEntity.noContent().build();
    }
}
