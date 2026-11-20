package com.duoc.auth_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.duoc.auth_users.dto.LoginRequest;
import com.duoc.auth_users.dto.LoginResponse;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.UsuarioRequest;
import com.duoc.auth_users.exception.CredencialesInvalidasException;
import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.UsuarioRepository;
import com.duoc.auth_users.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario() {
        return new Usuario(5L, "Ana", "Perez", "ana@test.cl", "hash", Rol.USUARIO, false, LocalDateTime.now());
    }

    @Test
    void registrarSiempreCreaUnUsuarioConRolUsuario() {
        authService.registrar(new RegistroRequest("Ana", "Perez", "ana@test.cl", "Clave2026!"));

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).createUsuario(captor.capture());
        assertThat(captor.getValue().rol()).isEqualTo(Rol.USUARIO);
        assertThat(captor.getValue().email()).isEqualTo("ana@test.cl");
    }

    @Test
    void loginCorrectoDevuelveTokenYUsuario() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("Clave2026!", "hash")).thenReturn(true);
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("token-falso");

        LoginResponse respuesta = authService.login(new LoginRequest("  ANA@test.cl ", "Clave2026!"));

        assertThat(respuesta.token()).isEqualTo("token-falso");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.usuario().email()).isEqualTo("ana@test.cl");
    }

    @Test
    void loginConEmailInexistenteLanzaCredencialesInvalidas() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@test.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@test.cl", "Clave2026!")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void loginConPasswordIncorrectaLanzaCredencialesInvalidas() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@test.cl", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
