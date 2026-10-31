package com.duoc.auth_users.service;

import com.duoc.auth_users.dto.LoginRequest;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.UsuarioResponse;

public interface AuthService {

    UsuarioResponse registrar(RegistroRequest request);

    UsuarioResponse login(LoginRequest request);

}
