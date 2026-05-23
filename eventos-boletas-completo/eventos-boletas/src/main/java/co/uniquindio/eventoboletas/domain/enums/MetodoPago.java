package co.uniquindio.eventoboletas.domain.enums;

import java.math.BigDecimal;

/**
 * Método de pago aceptado por el sistema.
 *
 * Cada método lleva su propio recargoPorcentaje, cumpliendo P6 (RN-04):
 *   precioFinal = precioBase × (1 + recargo_zona + recargo_metodoPago)
 *
 * Recargos definidos:
 *   EFECTIVO        → 0 %    (sin recargo)
 *   PSE             → 1 %
 *   TARJETA_DEBITO  → 2 %
 *   TRANSFERENCIA   → 1,5 %
 *   TARJETA_CREDITO → 5 %
 */
public enum MetodoPago {

    EFECTIVO        (BigDecimal.ZERO),
    TARJETA_DEBITO  (new BigDecimal("0.02")),
    TARJETA_CREDITO (new BigDecimal("0.05")),
    PSE             (new BigDecimal("0.01")),
    TRANSFERENCIA   (new BigDecimal("0.015"));

    private final BigDecimal recargoPorcentaje;

    MetodoPago(BigDecimal recargoPorcentaje) {
        this.recargoPorcentaje = recargoPorcentaje;
    }

    public BigDecimal getRecargoPorcentaje() {
        return recargoPorcentaje;
    }
}