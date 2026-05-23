package co.uniquindio.eventoboletas.domain.repositories;

import co.uniquindio.eventoboletas.domain.entities.Zona;

import java.util.Optional;

public interface ZonaRepository {

    Zona guardar(Zona zona);

    Optional<Zona> buscarPorId(Long id);
}
