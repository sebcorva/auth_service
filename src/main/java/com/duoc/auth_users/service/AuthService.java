package com.duoc.auth_users.service;

import com.duoc.auth_users.dto.RecuperarRequest;
import com.duoc.auth_users.dto.RecuperarResponse;
import com.duoc.auth_users.dto.RestablecerRequest;

import com.duoc.auth_users.dto.LoginRequest;
import com.duoc.auth_users.dto.LoginResponse;
import com.duoc.auth_users.dto.RegistroRequest;
import com.duoc.auth_users.dto.UsuarioResponse;

public interface AuthService {

    UsuarioResponse registrar(RegistroRequest request);

    LoginResponse login(LoginRequest request);

    RecuperarResponse recuperar(RecuperarRequest request);

    void restablecer(RestablecerRequest request);

}
