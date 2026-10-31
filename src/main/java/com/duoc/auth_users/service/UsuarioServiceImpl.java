package com.duoc.auth_users.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.auth_users.dto.UsuarioRequest;
import com.duoc.auth_users.dto.UsuarioResponse;
import com.duoc.auth_users.exception.ConflictoException;
import com.duoc.auth_users.exception.RecursoNoEncontradoException;
import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.PagoRepository;
import com.duoc.auth_users.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PagoRepository pagoRepository;

    private Usuario searchUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    }

    private String normalizaEmail(String email) {
        return email.trim().toLowerCase();
    }

    @Override
    public List<UsuarioResponse> getAllUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Override
    public UsuarioResponse getUsuarioById(Long id) {
        Usuario usuarioEncontrado = searchUsuarioById(id);
        return UsuarioResponse.from(usuarioEncontrado);
    }

    @Override
    public UsuarioResponse createUsuario(UsuarioRequest request) {

        String emailNormalizado = normalizaEmail(request.email());

        boolean emailExists = usuarioRepository.existsByEmailIgnoreCase(emailNormalizado);

        if (emailExists) {
            throw new ConflictoException("El email ingresado ya esta registrado");
        }

        Usuario nuevoUsuario = new Usuario();

        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setApellido(request.apellido());
        nuevoUsuario.setEmail(emailNormalizado);
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.password()));
        nuevoUsuario.setRol(request.rol() != null ? request.rol() : Rol.USUARIO);
        nuevoUsuario.setPremium(false);
        nuevoUsuario.setFechaRegistro(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        return UsuarioResponse.from(usuarioGuardado);

    }

    @Override
    public UsuarioResponse updateUsuario(Long id, UsuarioRequest request) {

        Usuario usuarioEncontrado = searchUsuarioById(id);

        String emailNormalizado = normalizaEmail(request.email());

        boolean cambioEmail = !emailNormalizado.equals(usuarioEncontrado.getEmail());

        if (cambioEmail && usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflictoException("El email ingresado ya esta registrado");
        }

        usuarioEncontrado.setNombre(request.nombre());
        usuarioEncontrado.setApellido(request.apellido());
        usuarioEncontrado.setEmail(emailNormalizado);
        usuarioEncontrado.setPasswordHash(passwordEncoder.encode(request.password()));
        usuarioEncontrado.setEmail(emailNormalizado);
        if (request.rol() != null) {
            usuarioEncontrado.setRol(request.rol());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuarioEncontrado);
        return UsuarioResponse.from(usuarioActualizado);
    }

    @Override
    @Transactional
    public void deleteUsuario(Long id) {
        Usuario usuarioEncontrado = searchUsuarioById(id);
        // Eliminar pago
        pagoRepository.deleteByUsuarioId(id);
        // Eliminar usuario
        usuarioRepository.delete(usuarioEncontrado);
    }

}