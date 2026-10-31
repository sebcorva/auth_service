package com.duoc.auth_users.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

// Crea el primer ADMIN al arrancar;
@Slf4j
@Component
public class AdminInicial implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            @Value("${admin.email}") String email,
            @Value("${admin.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByRol(Rol.ADMIN)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("Sistema");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Rol.ADMIN);
        admin.setPremium(true);
        admin.setFechaRegistro(LocalDateTime.now());

        usuarioRepository.save(admin);
        log.info("Admin inicial creado: {}", email);
    }

}
