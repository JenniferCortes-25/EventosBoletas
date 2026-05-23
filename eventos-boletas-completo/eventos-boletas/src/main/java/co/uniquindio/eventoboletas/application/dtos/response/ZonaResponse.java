package co.uniquindio.eventoboletas.application.dtos.response;

import java.math.BigDecimal;

public record ZonaResponse(
    Long id,
    String nombre,
    BigDecimal precioBase,
    BigDecimal recargoPorcentaje,
    BigDecimal precioFinal,
    int cupoTotal,
    int cupoDisponible
) {}
