package co.uniquindio.eventoboletas.domain.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;
import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio Boleto.
 *
 * Patrón Factory Method: crear() garantiza:
 * - RN-05: el boleto sólo se emite cuando el pago está APROBADO.
 * - Código QR único generado en dominio (UUID).
 * - Invariante: el codigoQR es único en el sistema.
 */
@Getter
@NoArgsConstructor
public class Boleto {

    private Long id;
    private String codigoQR;
    private BigDecimal precioFinal;
    private LocalDateTime fechaEmision;
    private EstadoBoleto estado;
    private Long clienteId;
    private Long zonaId;
    private Pago pago;

    private Boleto(Long id, String codigoQR, BigDecimal precioFinal,
                   LocalDateTime fechaEmision, EstadoBoleto estado,
                   Long clienteId, Long zonaId) {
        this.id = id;
        this.codigoQR = Objects.requireNonNull(codigoQR);
        this.precioFinal = Objects.requireNonNull(precioFinal);
        this.fechaEmision = fechaEmision;
        this.estado = Objects.requireNonNull(estado);
        this.clienteId = clienteId;
        this.zonaId = zonaId;
    }

    /**
     * Factory Method: crea un boleto únicamente cuando el pago está aprobado.
     * RN-05: Un pago fallido no produce boleto.
     */
    public static Boleto emitir(BigDecimal precioFinal, Long clienteId, Long zonaId, Pago pago) {
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new ReglaDeNegocioException(
                "El boleto sólo puede emitirse con un pago aprobado."
            );
        }
        String qr = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        Boleto boleto = new Boleto(null, qr, precioFinal,
                                   LocalDateTime.now(), EstadoBoleto.PAGADO, clienteId, zonaId);
        boleto.pago = pago;
        return boleto;
    }

    public static Boleto reconstituir(Long id, String codigoQR, BigDecimal precioFinal,
                                      LocalDateTime fechaEmision, EstadoBoleto estado,
                                      Long clienteId, Long zonaId) {
        return new Boleto(id, codigoQR, precioFinal, fechaEmision, estado, clienteId, zonaId);
    }

    public void asignarId(Long id) {
        this.id = id;
    }

    public void asignarPago(Pago pago) {
        this.pago = pago;
    }
}
