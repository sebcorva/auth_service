package com.duoc.auth_users.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.duoc.auth_users.dto.RecuperarRequest;
import com.duoc.auth_users.dto.RecuperarResponse;
import com.duoc.auth_users.dto.RestablecerRequest;

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

    static final int MINUTOS_VIGENCIA_CODIGO = 15;
    static final String MENSAJE_RECUPERACION = "Si el email esta registrado, te enviamos un codigo de recuperacion";
    private static final SecureRandom RANDOM = new SecureRandom();


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


    @Override
    @Transactional
    public RecuperarResponse recuperar(RecuperarRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(emailNormalizado);
        if (encontrado.isEmpty()) {
            // Mismo mensaje que si existiera, para no revelar que emails estan registrados
            return new RecuperarResponse(MENSAJE_RECUPERACION, null, MINUTOS_VIGENCIA_CODIGO);
        }

        Usuario usuario = encontrado.get();
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usuario.setCodigoRecuperacionHash(passwordEncoder.encode(codigo));
        usuario.setCodigoRecuperacionExpira(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA_CODIGO));
        usuarioRepository.save(usuario);

        // Simulacion del envio de correo: el codigo se devuelve en la respuesta
        return new RecuperarResponse(MENSAJE_RECUPERACION, codigo, MINUTOS_VIGENCIA_CODIGO);
    }

    @Override
    @Transactional
    public void restablecer(RestablecerRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .filter(u -> codigoVigente(u, request.codigo()))
                .orElseThrow(() -> new IllegalArgumentException("El codigo es invalido o expiro"));

        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        // Un solo uso: el codigo se elimina al restablecer
        usuario.setCodigoRecuperacionHash(null);
        usuario.setCodigoRecuperacionExpira(null);
        usuarioRepository.save(usuario);
    }

    private boolean codigoVigente(Usuario usuario, String codigo) {
        return usuario.getCodigoRecuperacionHash() != null
                && usuario.getCodigoRecuperacionExpira() != null
                && usuario.getCodigoRecuperacionExpira().isAfter(LocalDateTime.now())
                && passwordEncoder.matches(codigo, usuario.getCodigoRecuperacionHash());
    }
}
