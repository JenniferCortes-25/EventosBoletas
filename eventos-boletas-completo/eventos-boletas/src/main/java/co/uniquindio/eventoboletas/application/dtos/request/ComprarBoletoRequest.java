package co.uniquindio.eventoboletas.application.dtos.request;

import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para la transacción Comprar Boleto (CU-01).
 *
 * Cambios respecto a la versión anterior:
 *  - Se añade {@code cantidad}: número de boletos a comprar en una sola transacción.
 *    Mínimo 1, limitado superiormente por el cupo disponible de la zona (validado en dominio).
 */
public record ComprarBoletoRequest(

    @NotNull(message = "El ID del cliente es obligatorio")
    Long clienteId,

    @NotNull(message = "El ID del evento es obligatorio")
    Long eventoId,

    @NotNull(message = "El ID de la zona es obligatorio")
    Long zonaId,

    @NotNull(message = "El método de pago es obligatorio")
    MetodoPago metodoPago,

    @NotNull(message = "La cantidad de boletos es obligatoria")
    @Min(value = 1, message = "Debe comprar al menos 1 boleto")
    Integer cantidad

) {}