package co.uniquindio.eventoboletas.infrastructure.persistence.entities;

import co.uniquindio.eventoboletas.domain.enums.EstadoBoleto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BOLETOS",
       uniqueConstraints = @UniqueConstraint(name = "UQ_BOLETO_QR", columnNames = "CODIGO_QR"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_boleto")
    @SequenceGenerator(name = "seq_boleto", sequenceName = "SEQ_BOLETOS",
                       allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_QR", nullable = false, length = 50, unique = true)
    private String codigoQR;

    @Column(name = "PRECIO_FINAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "FECHA_EMISION", nullable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoBoleto estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENTE_ID", nullable = false)
    private ClienteJpa cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ZONA_ID", nullable = false)
    private ZonaJpa zona;

    @OneToOne(mappedBy = "boleto", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, optional = false)
    private PagoJpa pago;
}