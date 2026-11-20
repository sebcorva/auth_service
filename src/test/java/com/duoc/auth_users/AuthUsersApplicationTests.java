package com.duoc.auth_users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.duoc.auth_users.model.Rol;
import com.duoc.auth_users.repository.UsuarioRepository;

// Levanta el contexto completo contra H2 (perfil test): no necesita Oracle
@SpringBootTest
@ActiveProfiles("test")
class AuthUsersApplicationTests {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void contextLoadsYCreaElAdminInicial() {
		assertThat(usuarioRepository.existsByRol(Rol.ADMIN)).isTrue();
		assertThat(usuarioRepository.findByEmailIgnoreCase("admin@test.cl")).isPresent();
	}

}
