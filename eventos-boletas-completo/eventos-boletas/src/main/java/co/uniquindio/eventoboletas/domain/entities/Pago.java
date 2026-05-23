package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio Pago.
 * Patrón Value Object-like: inmutable una vez creado.
 */
@Getter
@NoArgsConstructor
public class Pago {

    private Long id;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private LocalDateTime fechaPago;
    private EstadoPago estado;

    private Pago(Long id, BigDecimal monto, MetodoPago metodoPago,
                 LocalDateTime fechaPago, EstadoPago estado) {
        this.id = id;
        this.monto = Objects.requireNonNull(monto);
        this.metodoPago = Objects.requireNonNull(metodoPago);
        this.fechaPago = fechaPago;
        this.estado = Objects.requireNonNull(estado);
    }

    public static Pago crear(BigDecimal monto, MetodoPago metodoPago) {
        return new Pago(null, monto, metodoPago, LocalDateTime.now(), EstadoPago.APROBADO);
    }

    public static Pago reconstituir(Long id, BigDecimal monto, MetodoPago metodoPago,
                                    LocalDateTime fechaPago, EstadoPago estado) {
        return new Pago(id, monto, metodoPago, fechaPago, estado);
    }

    public void asignarId(Long id) {
        this.id = id;
    }
}
