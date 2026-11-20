package com.duoc.auth_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.duoc.auth_users.dto.PagoPremiumResponse;
import com.duoc.auth_users.exception.ConflictoException;
import com.duoc.auth_users.model.Pago;
import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.PagoRepository;
import com.duoc.auth_users.repository.UsuarioRepository;
import com.duoc.auth_users.security.JwtService;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;

    private PagoServiceImpl pagoService;

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(pagoRepository, usuarioRepository, jwtService, 2000);
    }

    @Test
    void comprarPremiumMarcaAlUsuarioYDevuelveTokenNuevo() {
        Usuario usuario = new Usuario(5L, "Ana", "Perez", "ana@test.cl", "hash", Rol.USUARIO, false, LocalDateTime.now(), null, null);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generarToken(usuario)).thenReturn("token-premium");

        PagoPremiumResponse respuesta = pagoService.comprarPremium(5L);

        assertThat(usuario.isPremium()).isTrue();
        assertThat(respuesta.pago().monto()).isEqualTo(2000);
        assertThat(respuesta.sesion().token()).isEqualTo("token-premium");
        assertThat(respuesta.sesion().usuario().premium()).isTrue();
    }

    @Test
    void comprarPremiumSiendoPremiumLanzaConflicto() {
        Usuario premium = new Usuario(5L, "Ana", "Perez", "ana@test.cl", "hash", Rol.USUARIO, true, LocalDateTime.now(), null, null);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(premium));

        assertThatThrownBy(() -> pagoService.comprarPremium(5L)).isInstanceOf(ConflictoException.class);
        verify(pagoRepository, never()).save(any());
    }
}
