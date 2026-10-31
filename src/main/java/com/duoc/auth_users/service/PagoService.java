package com.duoc.auth_users.service;

import java.util.List;

import com.duoc.auth_users.dto.PagoPremiumResponse;
import com.duoc.auth_users.dto.PagoResponse;

public interface PagoService {

    PagoPremiumResponse comprarPremium(Long usuarioId);

    List<PagoResponse> getPagosByUsuario(Long usuarioId);

}
