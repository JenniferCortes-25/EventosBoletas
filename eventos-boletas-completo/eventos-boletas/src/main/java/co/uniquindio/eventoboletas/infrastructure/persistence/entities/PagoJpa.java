package co.uniquindio.eventoboletas.infrastructure.persistence.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoPago;
import co.uniquindio.eventoboletas.domain.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAGOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pago")
    @SequenceGenerator(name = "seq_pago", sequenceName = "SEQ_PAGOS",
                       allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "METODO_PAGO", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(name = "FECHA_PAGO", nullable = false)
    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoPago estado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOLETO_ID", nullable = false, unique = true)
    private BoletoJpa boleto;
}