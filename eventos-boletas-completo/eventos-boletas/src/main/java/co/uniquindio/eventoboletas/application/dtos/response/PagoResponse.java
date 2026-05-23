package co.uniquindio.eventoboletas.application.dtos.response;

import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
    Long id,
    BigDecimal monto,
    MetodoPago metodoPago,
    LocalDateTime fechaPago,
    EstadoPago estado
) {}
