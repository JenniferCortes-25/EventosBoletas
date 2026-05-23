package co.uniquindio.eventoboletas.infrastructure.persistence.repositories;

import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.EventoJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventoJpaRepository extends JpaRepository<EventoJpa, Long> {

    @Query("SELECT e FROM EventoJpa e LEFT JOIN FETCH e.zonas WHERE e.estado = :estado")
    List<EventoJpa> findByEstadoWithZonas(@Param("estado") EstadoEvento estado);

    @Query("SELECT e FROM EventoJpa e LEFT JOIN FETCH e.zonas WHERE e.id = :id")
    Optional<EventoJpa> findByIdWithZonas(@Param("id") Long id);

    // ── NUEVO ─────────────────────────────────────────────────────────────────
    /**
     * Trae TODOS los eventos (ACTIVO, CANCELADO, AGOTADO) con sus zonas,
     * ordenados por fecha ascendente para la pantalla de compra.
     */
    @Query("SELECT e FROM EventoJpa e LEFT JOIN FETCH e.zonas ORDER BY e.fecha ASC")
    List<EventoJpa> findAllWithZonas();
}