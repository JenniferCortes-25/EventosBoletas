package co.uniquindio.eventoboletas.domain.repositories;

import co.uniquindio.eventoboletas.domain.entities.Evento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para Evento.
 */
public interface EventoRepository {

    Evento guardar(Evento evento);

    Optional<Evento> buscarPorId(Long id);

    List<Evento> buscarActivos();
}
