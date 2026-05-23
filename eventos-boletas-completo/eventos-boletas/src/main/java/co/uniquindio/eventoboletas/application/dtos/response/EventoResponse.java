package co.uniquindio.eventoboletas.application.dtos.response;

import co.uniquindio.eventoboletas.domain.enums.EstadoEvento;

import java.time.LocalDateTime;
import java.util.List;

public record EventoResponse(
    Long id,
    String nombre,
    LocalDateTime fecha,
    String lugar,
    EstadoEvento estado,
    List<ZonaResponse> zonas
) {}
