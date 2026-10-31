package com.duoc.auth_users.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.auth_users.dto.LoginResponse;
import com.duoc.auth_users.dto.PagoPremiumResponse;
import com.duoc.auth_users.dto.PagoResponse;
import com.duoc.auth_users.dto.UsuarioResponse;
import com.duoc.auth_users.exception.ConflictoException;
import com.duoc.auth_users.exception.RecursoNoEncontradoException;
import com.duoc.auth_users.model.Pago;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.PagoRepository;
import com.duoc.auth_users.repository.UsuarioRepository;
import com.duoc.auth_users.security.JwtService;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final int precio;

    public PagoServiceImpl(PagoRepository pagoRepository, UsuarioRepository usuarioRepository,
            JwtService jwtService, @Value("${premium.precio}") int precio) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.precio = precio;
    }

    @Override
    @Transactional
    public PagoPremiumResponse comprarPremium(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (usuario.isPremium()) {
            throw new ConflictoException("El usuario ya es premium");
        }

        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(precio);
        pago.setFecha(LocalDateTime.now());
        Pago pagoGuardado = pagoRepository.save(pago);

        usuario.setPremium(true);
        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario);

        return new PagoPremiumResponse(
                PagoResponse.from(pagoGuardado),
                new LoginResponse(token, UsuarioResponse.from(usuario)));
    }

    @Override
    public List<PagoResponse> getPagosByUsuario(Long usuarioId) {
        return pagoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId)
                .stream()
                .map(PagoResponse::from)
                .toList();
    }

}
