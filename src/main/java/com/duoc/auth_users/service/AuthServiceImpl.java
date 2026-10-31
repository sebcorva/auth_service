package com.duoc.auth_users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.duoc.auth_users.dto.LoginRequest;
import com.duoc.auth_users.dto.LoginResponse;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.UsuarioRequest;
import com.duoc.auth_users.dto.UsuarioResponse;
import com.duoc.auth_users.exception.CredencialesInvalidasException;
import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.model.Usuario;
import com.duoc.auth_users.repository.UsuarioRepository;
import com.duoc.auth_users.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UsuarioResponse registrar(RegistroRequest request) {
        // El registro publico siempre crea un USUARIO; la logica de creacion vive en
        // UsuarioService
        UsuarioRequest nuevoUsuario = new UsuarioRequest(
                request.nombre(),
                request.apellido(),
                request.email(),
                request.password(),
                Rol.USUARIO);

        return usuarioService.createUsuario(nuevoUsuario);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new CredencialesInvalidasException());

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.from(usuario));
    }

}
