package com.duoc.auth_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.duoc.auth_users.dto.UsuarioRequest;
import com.duoc.auth_users.dto.UsuarioResponse;
import com.duoc.auth_users.dto.UsuarioUpdateRequest;
import com.duoc.auth_users.exception.ConflictoException;
import com.duoc.auth_users.exception.RecursoNoEncontradoException;
import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.PagoRepository;
import com.duoc.auth_users.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario existente() {
        return new Usuario(7L, "Ana", "Perez", "ana@test.cl", "hash", Rol.USUARIO, false, LocalDateTime.now(), null, null);
    }

    @Test
    void crearNormalizaEmailHasheaPasswordYUsaRolUsuarioPorDefecto() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@test.cl")).thenReturn(false);
        when(passwordEncoder.encode("Clave2026!")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse creado = usuarioService.createUsuario(
                new UsuarioRequest("Nuevo", "Usuario", " NUEVO@test.cl ", "Clave2026!", null));

        assertThat(creado.email()).isEqualTo("nuevo@test.cl");
        assertThat(creado.rol()).isEqualTo(Rol.USUARIO);
        assertThat(creado.premium()).isFalse();
        verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(u -> "hash-nuevo".equals(u.getPasswordHash())));
    }

    @Test
    void crearConEmailRepetidoLanzaConflicto() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.createUsuario(
                new UsuarioRequest("Ana", "Perez", "ana@test.cl", "Clave2026!", Rol.USUARIO)))
                .isInstanceOf(ConflictoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.getUsuarioById(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarConEmailDeOtroUsuarioLanzaConflicto() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente()));
        when(usuarioRepository.existsByEmailIgnoreCase("otro@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.updateUsuario(7L,
                new UsuarioUpdateRequest("Ana", "Perez", "otro@test.cl", null, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void actualizarSinPasswordNiRolConservaAmbos() {
        Usuario usuario = existente();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse actualizado = usuarioService.updateUsuario(7L,
                new UsuarioUpdateRequest("Ana Maria", "Perez", "ana@test.cl", null, null));

        assertThat(actualizado.nombre()).isEqualTo("Ana Maria");
        assertThat(actualizado.rol()).isEqualTo(Rol.USUARIO);
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizarConPasswordLaHashea() {
        Usuario usuario = existente();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("Nueva2026!")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.updateUsuario(7L, new UsuarioUpdateRequest("Ana", "Perez", "ana@test.cl", "Nueva2026!", null));

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
    }

    @Test
    void eliminarBorraPrimeroLosPagosYLuegoElUsuario() {
        Usuario usuario = existente();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        usuarioService.deleteUsuario(7L);

        InOrder orden = inOrder(pagoRepository, usuarioRepository);
        orden.verify(pagoRepository).deleteByUsuarioId(7L);
        orden.verify(usuarioRepository).delete(usuario);
    }
}
