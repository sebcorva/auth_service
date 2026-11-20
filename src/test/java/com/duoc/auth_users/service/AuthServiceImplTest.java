package com.duoc.auth_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.duoc.auth_users.dto.RecuperarRequest;
import com.duoc.auth_users.dto.RecuperarResponse;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.RestablecerRequest;
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
        return new Usuario(5L, "Ana", "Perez", "ana@test.cl", "hash", Rol.USUARIO, false, LocalDateTime.now(), null, null);
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

    // ---------- Recuperar contraseña ----------

    @Test
    void recuperarGeneraCodigoDeSeisDigitosHasheadoYConVencimiento() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(any())).thenReturn("hash-codigo");

        RecuperarResponse respuesta = authService.recuperar(new RecuperarRequest("ANA@test.cl"));

        assertThat(respuesta.codigoDemo()).matches("\\d{6}");
        assertThat(respuesta.minutosVigencia()).isEqualTo(15);
        assertThat(usuario.getCodigoRecuperacionHash()).isEqualTo("hash-codigo");
        assertThat(usuario.getCodigoRecuperacionExpira())
                .isBetween(LocalDateTime.now().plusMinutes(14), LocalDateTime.now().plusMinutes(16));
        verify(passwordEncoder).encode(respuesta.codigoDemo());
    }

    @Test
    void recuperarConEmailInexistenteDevuelveElMismoMensajeSinCodigo() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@test.cl")).thenReturn(Optional.empty());

        RecuperarResponse respuesta = authService.recuperar(new RecuperarRequest("nadie@test.cl"));

        assertThat(respuesta.codigoDemo()).isNull();
        assertThat(respuesta.mensaje()).isEqualTo(AuthServiceImpl.MENSAJE_RECUPERACION);
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuarioConCodigo(LocalDateTime expira) {
        Usuario usuario = usuario();
        usuario.setCodigoRecuperacionHash("hash-codigo");
        usuario.setCodigoRecuperacionExpira(expira);
        return usuario;
    }

    @Test
    void restablecerConCodigoValidoCambiaLaPasswordYBorraElCodigo() {
        Usuario usuario = usuarioConCodigo(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hash-codigo")).thenReturn(true);
        when(passwordEncoder.encode("Nueva2026!")).thenReturn("hash-nueva");

        authService.restablecer(new RestablecerRequest("ana@test.cl", "123456", "Nueva2026!"));

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nueva");
        assertThat(usuario.getCodigoRecuperacionHash()).isNull();
        assertThat(usuario.getCodigoRecuperacionExpira()).isNull();
    }

    @Test
    void restablecerConCodigoIncorrectoLanza400() {
        Usuario usuario = usuarioConCodigo(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("000000", "hash-codigo")).thenReturn(false);

        assertThatThrownBy(() -> authService.restablecer(new RestablecerRequest("ana@test.cl", "000000", "Nueva2026!")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void restablecerConCodigoVencidoLanza400() {
        Usuario usuario = usuarioConCodigo(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.restablecer(new RestablecerRequest("ana@test.cl", "123456", "Nueva2026!")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void restablecerSinCodigoSolicitadoLanza400() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@test.cl")).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> authService.restablecer(new RestablecerRequest("ana@test.cl", "123456", "Nueva2026!")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
