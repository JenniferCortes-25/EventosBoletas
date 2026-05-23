package co.uniquindio.eventoboletas.infrastructure.adapters;

import co.uniquindio.eventoboletas.domain.entities.Zona;
import co.uniquindio.eventoboletas.domain.repositories.ZonaRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.EventoJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.entities.ZonaJpa;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.EventoJpaRepository;
import co.uniquindio.eventoboletas.infrastructure.persistence.repositories.ZonaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ZonaRepositoryAdapter implements ZonaRepository {

    private final ZonaJpaRepository zonaJpaRepository;
    private final EventoJpaRepository eventoJpaRepository;

    @Override
    public Zona guardar(Zona zona) {
        ZonaJpa jpa = toJpa(zona);
        return toDomain(zonaJpaRepository.save(jpa));
    }

    @Override
    public Optional<Zona> buscarPorId(Long id) {
        return zonaJpaRepository.findById(id).map(this::toDomain);
    }

    private Zona toDomain(ZonaJpa z) {
        return Zona.reconstituir(z.getId(), z.getNombre(), z.getPrecioBase(),
                                 z.getRecargoPorcentaje(), z.getCupoTotal(),
                                 z.getCupoDisponible(),
                                 z.getEvento() != null ? z.getEvento().getId() : null);
    }

    private ZonaJpa toJpa(Zona z) {
        EventoJpa eventoRef = null;
        if (z.getEventoId() != null) {
            eventoRef = eventoJpaRepository.getReferenceById(z.getEventoId());
        }
        return ZonaJpa.builder()
            .id(z.getId())
            .nombre(z.getNombre())
            .precioBase(z.getPrecioBase())
            .recargoPorcentaje(z.getRecargoPorcentaje())
            .cupoTotal(z.getCupoTotal())
            .cupoDisponible(z.getCupoDisponible())
            .evento(eventoRef)
            .build();
    }
}
