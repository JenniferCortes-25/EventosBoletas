package co.uniquindio.eventoboletas.infrastructure.persistence.repositories;

import co.uniquindio.eventoboletas.infrastructure.persistence.entities.BoletoJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoletoJpaRepository extends JpaRepository<BoletoJpa, Long> {
    Optional<BoletoJpa> findByCodigoQR(String codigoQR);
    boolean existsByCodigoQR(String codigoQR);
}
