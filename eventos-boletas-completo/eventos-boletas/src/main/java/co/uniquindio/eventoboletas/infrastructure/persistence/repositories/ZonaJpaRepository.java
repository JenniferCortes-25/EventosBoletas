package co.uniquindio.eventoboletas.infrastructure.persistence.repositories;

import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaJpaRepository extends JpaRepository<ZonaJpa, Long> {}
