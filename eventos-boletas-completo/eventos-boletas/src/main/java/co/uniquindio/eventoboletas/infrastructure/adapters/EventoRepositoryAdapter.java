package co.uniquindio.eventoboletas.infrastructure.adapters;

import co.uniquindio.eventoboletas.domain.entities.Evento;
import co.uniquindio.eventoboletas.domain.entities.Zona;
import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;
import co.uniquindio.eventoboletas.domain.repositories.EventoRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.EventoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.EventoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador para EventoRepository.
 * Patrón Adapter: convierte entre entidades JPA y entidades de dominio.
 */
@Component
@RequiredArgsConstructor
public class EventoRepositoryAdapter implements EventoRepository {

    private final EventoJpaRepository jpaRepository;

    @Override
    public Evento guardar(Evento evento) {
        EventoJpa jpa = toJpa(evento);
        return toDomain(jpaRepository.save(jpa));
    }

    @Override
    public Optional<Evento> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Evento> buscarActivos() {
        return jpaRepository.findByEstadoWithZonas(EstadoEvento.ACTIVO)
            .stream().map(this::toDomain).toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private Evento toDomain(EventoJpa jpa) {
        Evento evento = Evento.reconstituir(
            jpa.getId(), jpa.getNombre(), jpa.getFecha(),
            jpa.getLugar(), jpa.getEstado()
        );
        if (jpa.getZonas() != null) {
            jpa.getZonas().forEach(z -> evento.agregarZona(zonaToDomain(z)));
        }
        return evento;
    }

    private Zona zonaToDomain(ZonaJpa z) {
        return Zona.reconstituir(z.getId(), z.getNombre(), z.getPrecioBase(),
                                 z.getRecargoPorcentaje(), z.getCupoTotal(),
                                 z.getCupoDisponible(),
                                 z.getEvento() != null ? z.getEvento().getId() : null);
    }

    private EventoJpa toJpa(Evento e) {
        return EventoJpa.builder()
            .id(e.getId())
            .nombre(e.getNombre())
            .fecha(e.getFecha())
            .lugar(e.getLugar())
            .estado(e.getEstado())
            .build();
    }
}
