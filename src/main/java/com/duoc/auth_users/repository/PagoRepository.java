package com.duoc.auth_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duoc.auth_users.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
