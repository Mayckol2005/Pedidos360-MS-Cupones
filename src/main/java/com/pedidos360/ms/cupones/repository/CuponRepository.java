
package com.pedidos360.ms.cupones.repository;

import com.pedidos360.ms.cupones.entity.Cupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {

    Optional<Cupon> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM Cupon c
            WHERE UPPER(c.codigo) = UPPER(:codigo)
            """)
    Optional<Cupon> buscarPorCodigoParaActualizar(
            @Param("codigo") String codigo
    );
}
