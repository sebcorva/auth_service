package com.duoc.auth_users.service;

import java.util.List;

import com.duoc.auth_users.dto.UsuarioRequest;
import com.duoc.auth_users.dto.UsuarioResponse;

public interface UsuarioService {

    List<UsuarioResponse> getAllUsuarios();

    UsuarioResponse getUsuarioById(Long id);

    UsuarioResponse createUsuario(UsuarioRequest request);

    UsuarioResponse updateUsuario(Long id, UsuarioRequest request);

    void deleteUsuario(Long id);

}
