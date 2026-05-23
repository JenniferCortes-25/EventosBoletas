package co.uniquindio.eventoboletas.application.dtos.request;

import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para la transacción Comprar Boleto (CU-01).
 */
public record ComprarBoletoRequest(
    @NotNull(message = "El ID del cliente es obligatorio")
    Long clienteId,

    @NotNull(message = "El ID del evento es obligatorio")
    Long eventoId,

    @NotNull(message = "El ID de la zona es obligatorio")
    Long zonaId,

    @NotNull(message = "El método de pago es obligatorio")
    MetodoPago metodoPago
) {}
