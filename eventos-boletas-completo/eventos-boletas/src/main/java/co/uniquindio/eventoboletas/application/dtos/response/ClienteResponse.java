package co.uniquindio.eventoboletas.application.dtos.response;

import co.uniquindio.eventoboletas.domain.enums.EstadoCliente;

/**
 * DTO de salida para Cliente.
 */
public record ClienteResponse(
    Long id,
    String nombre,
    String apellido,
    String email,
    String documento,
    EstadoCliente estado
) {}
