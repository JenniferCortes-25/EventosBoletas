package co.uniquindio.eventoboletas.application.dtos.response;

import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para el comprobante de compra de Boleto (resultado de CU-01).
 */
public record BoletoResponse(
    Long id,
    String codigoQR,
    BigDecimal precioFinal,
    LocalDateTime fechaEmision,
    EstadoBoleto estadoBoleto,
    Long clienteId,
    String nombreCliente,
    Long zonaId,
    String nombreZona,
    Long eventoId,
    String nombreEvento,
    String lugarEvento,
    LocalDateTime fechaEvento,
    PagoResponse pago
) {}
